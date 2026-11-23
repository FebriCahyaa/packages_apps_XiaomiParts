/*
 * Copyright (C) 2026 FEBRIAN RAHMAD CAHYA
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.charging

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.BatteryManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.lineageos.settings.utils.FileUtils
import org.lineageos.settings.utils.KernelUtils

/** Snapshot of the charging properties exported by the 6.6 Xiaomi charger driver. */
data class ChargingStatus(
    val supported: Boolean = false,
    val kernel66: Boolean = true,
    val bypass: Boolean = false,
    val boostActive: Boolean = false,
    val boostRequested: Boolean = false,
    val fastChargeMode: Int? = null,
    val quickChargeType: Int? = null,
    val adapterId: String? = null,
    val pdVerified: Boolean? = null,
    val plugged: Boolean = false,
    val batteryTemperature: Int? = null,
    val hot: Boolean = false,
    val restoreOnBoot: Boolean = true,
    val tempGuard: Boolean = true,
    val forceAuth: Boolean = false,
)

/**
 * Single source of truth for bypass charging and wired fast charging.
 *
 * Bypass and boost are mutually exclusive: bypass suspends the charger input, so asking the
 * charger for a higher current at the same time only generates heat. The effective boost state
 * is therefore `boost preference && !bypass && !hot`.
 */
object ChargingRepository {
    private const val TAG = "ChargingRepository"

    private const val PREFS_NAME = "charging_prefs"
    private const val PREF_BYPASS = "charge_bypass"
    private const val PREF_BOOST = "charge_boost"
    private const val PREF_FORCE_AUTH = "charge_force_auth"
    private const val PREF_TEMP_GUARD = "charge_temp_guard"
    private const val PREF_RESTORE_ON_BOOT = "charge_restore_on_boot"

    const val NODE_INPUT_SUSPEND = "/sys/class/qcom-battery/input_suspend"
    const val NODE_SPORT_MODE = "/sys/class/qcom-battery/sport_mode"
    const val NODE_FASTCHG_MODE = "/sys/class/qcom-battery/fastchg_mode"
    const val NODE_PD_VERIFED = "/sys/class/qcom-battery/pd_verifed"
    const val NODE_QUICK_CHARGE_TYPE = "/sys/class/qcom-battery/quick_charge_type"
    const val NODE_ADAPTER_ID = "/sys/class/qcom-battery/adapter_id"
    const val NODE_CHARGER_TEMP = "/sys/class/thermal/thermal_message/charger_temp"

    /** Optional aggregate node added to our 6.6 driver; individual nodes are used as fallback. */
    private const val NODE_GARNET_STATUS = "/sys/class/qcom-battery/garnet_charge_status"

    // HyperOS tells the thermal driver about boost with charger_temp 8, and 0 without it.
    private const val CHARGER_TEMP_BOOST = 8

    /** Battery temperature in tenths of a degree Celsius. */
    const val TEMP_HOT = 420
    const val TEMP_RESUME = 400

    private val _status = MutableStateFlow(ChargingStatus())
    val status: StateFlow<ChargingStatus> = _status

    /** Temperature-guard state, owned by ChargingService. Volatile because the service writes it. */
    @Volatile
    var hot: Boolean = false
        private set

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun readInt(path: String): Int? = FileUtils.readOneLine(path)?.trim()?.toIntOrNull()

    private fun writeInt(path: String, value: Int): Boolean {
        if (!FileUtils.fileExists(path)) return false
        val ok = FileUtils.writeLine(path, value.toString())
        if (!ok) Log.w(TAG, "Failed to write $path=$value")
        return ok
    }

    private fun isBypassActive(): Boolean = readInt(NODE_INPUT_SUSPEND) == 1

    fun isSupported(): Boolean =
        FileUtils.fileExists(NODE_INPUT_SUSPEND) ||
            FileUtils.fileExists(NODE_SPORT_MODE) ||
            FileUtils.fileExists(NODE_CHARGER_TEMP)

    fun isBoostSupported(): Boolean =
        FileUtils.fileExists(NODE_SPORT_MODE) || FileUtils.fileExists(NODE_CHARGER_TEMP)

    fun isBypassSupported(): Boolean = FileUtils.fileExists(NODE_INPUT_SUSPEND)

    fun hasAggregateStatusNode(): Boolean = FileUtils.fileExists(NODE_GARNET_STATUS)

    /** Clear the nodes that ask the charger/thermal driver for the boosted profile. */
    fun clearBoostNodes() {
        writeInt(NODE_SPORT_MODE, 0)
        writeInt(NODE_CHARGER_TEMP, 0)
    }

    /** Apply the effective boost state to the driver and return true if a node was written. */
    fun applyBoostNodes(context: Context): Boolean {
        val p = prefs(context)
        val thermalBlocked = p.getBoolean(PREF_TEMP_GUARD, true) && hot
        val on = p.getBoolean(PREF_BOOST, true) &&
            !p.getBoolean(PREF_BYPASS, false) &&
            !thermalBlocked &&
            !isBypassActive()

        val sport = if (FileUtils.fileExists(NODE_SPORT_MODE)) {
            writeInt(NODE_SPORT_MODE, if (on) 1 else 0)
        } else {
            false
        }
        val temp = if (FileUtils.fileExists(NODE_CHARGER_TEMP)) {
            writeInt(NODE_CHARGER_TEMP, if (on) CHARGER_TEMP_BOOST else 0)
        } else {
            false
        }
        refresh(context)
        return sport || temp
    }

    /**
     * Marks the charger as verified when a genuine Xiaomi adapter never reached fast mode.
     * Experimental and always opt-in; only runs while boost is allowed.
     */
    fun nudgeAuthentication(context: Context) {
        val p = prefs(context)
        if (!p.getBoolean(PREF_FORCE_AUTH, false)) return
        val thermalBlocked = p.getBoolean(PREF_TEMP_GUARD, true) && hot
        if (!p.getBoolean(PREF_BOOST, true) || thermalBlocked || isBypassActive()) return
        if (!FileUtils.fileExists(NODE_FASTCHG_MODE)) return

        val mode = readInt(NODE_FASTCHG_MODE) ?: return
        if (mode == 1) return
        if (!FileUtils.fileExists(NODE_PD_VERIFED)) return
        writeInt(NODE_PD_VERIFED, 1)
    }

    fun setBypass(context: Context, enabled: Boolean): Boolean {
        if (!isBypassSupported()) return false
        if (!writeInt(NODE_INPUT_SUSPEND, if (enabled) 1 else 0)) return false

        prefs(context).edit().putBoolean(PREF_BYPASS, enabled).apply()
        if (enabled) {
            clearBoostNodes()
        } else {
            applyBoostNodes(context)
        }
        refresh(context)
        ChargingService.startIfNeeded(context)
        return true
    }

    fun setBoost(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(PREF_BOOST, enabled).apply()
        if (enabled && isBypassActive()) {
            // Fast charge and bypass cannot be active at the same time.
            writeInt(NODE_INPUT_SUSPEND, 0)
            prefs(context).edit().putBoolean(PREF_BYPASS, false).apply()
        }
        applyBoostNodes(context)
        ChargingService.startIfNeeded(context)
    }

    fun setForceAuth(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(PREF_FORCE_AUTH, enabled).apply()
        if (enabled) nudgeAuthentication(context)
        refresh(context)
    }

    fun setTempGuard(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(PREF_TEMP_GUARD, enabled).apply()
        if (!enabled) setHot(context, false)
        refresh(context)
    }

    fun setRestoreOnBoot(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(PREF_RESTORE_ON_BOOT, enabled).apply()
        refresh(context)
    }

    fun setHot(context: Context, value: Boolean) {
        if (hot == value) return
        hot = value
        applyBoostNodes(context)
    }

    /** Restore the persisted charging mode. Called from the single boot receiver. */
    fun applyOnBoot(context: Context) {
        if (!KernelUtils.isKernel66()) {
            Log.i(TAG, "Kernel ${System.getProperty("os.version", "unknown")} is not 6.6")
            return
        }
        val p = prefs(context)
        if (p.getBoolean(PREF_RESTORE_ON_BOOT, true)) {
            if (isBypassSupported()) {
                writeInt(NODE_INPUT_SUSPEND, if (p.getBoolean(PREF_BYPASS, false)) 1 else 0)
            }
            if (p.getBoolean(PREF_BYPASS, false)) {
                clearBoostNodes()
            }
        }
        refresh(context)
        ChargingService.startIfNeeded(context)
    }

    fun serviceNeeded(context: Context): Boolean {
        val p = prefs(context)
        return KernelUtils.isKernel66() &&
            (p.getBoolean(PREF_BOOST, true) || p.getBoolean(PREF_BYPASS, false))
    }

    /** Reads all driver properties plus the Android battery state. Safe to call repeatedly. */
    fun refresh(context: Context): ChargingStatus {
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val plugged = (battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) > 0
        val temperature = battery?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
            ?.takeIf { it > 0 }

        var bypass = readInt(NODE_INPUT_SUSPEND) == 1
        var boostActive = readInt(NODE_SPORT_MODE) == 1 ||
            readInt(NODE_CHARGER_TEMP) == CHARGER_TEMP_BOOST
        var fastChargeMode = readInt(NODE_FASTCHG_MODE)

        // Prefer the aggregate node from our kernel when it is available.
        FileUtils.readOneLine(NODE_GARNET_STATUS)?.let { line ->
            val fields = line.split(Regex("\\s+")).associate { field ->
                val parts = field.split('=')
                parts[0] to parts.getOrNull(1)
            }
            fields["bypass"]?.toIntOrNull()?.let { bypass = it == 1 }
            fields["boost"]?.toIntOrNull()?.let { boostActive = it == 1 }
            fields["fastchg"]?.toIntOrNull()?.let { fastChargeMode = it }
        }

        val p = prefs(context)
        val status = ChargingStatus(
            supported = isSupported(),
            kernel66 = KernelUtils.isKernel66(),
            bypass = bypass,
            boostActive = boostActive,
            boostRequested = p.getBoolean(PREF_BOOST, true),
            fastChargeMode = fastChargeMode,
            quickChargeType = readInt(NODE_QUICK_CHARGE_TYPE),
            adapterId = FileUtils.readOneLine(NODE_ADAPTER_ID)?.trim(),
            pdVerified = readInt(NODE_PD_VERIFED)?.let { it == 1 },
            plugged = plugged,
            batteryTemperature = temperature,
            hot = hot,
            restoreOnBoot = p.getBoolean(PREF_RESTORE_ON_BOOT, true),
            tempGuard = p.getBoolean(PREF_TEMP_GUARD, true),
            forceAuth = p.getBoolean(PREF_FORCE_AUTH, false),
        )
        _status.value = status
        return status
    }
}
