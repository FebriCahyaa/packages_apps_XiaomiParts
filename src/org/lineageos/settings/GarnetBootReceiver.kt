/*
 * Copyright (C) 2015 The CyanogenMod Project
 *               2017-2019 The LineageOS Project
 *           (C) 2024 The LineageOS Project
 *           (C) 2025 KamiKaonashi
 *           (C) 2026 zylhdrXP
 *           (C) 2026 FEBRIAN RAHMAD CAHYA
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.util.Log
import android.view.Display
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.lineageos.settings.charging.ChargingRepository
import org.lineageos.settings.corecontrol.CoreControlUtils
import org.lineageos.settings.display.RealityDisplayEngine
import org.lineageos.settings.gpumanager.GpuManagerUtils
import org.lineageos.settings.gpumanager.GpuManagerViewModel
import org.lineageos.settings.kernelmanager.KernelManagerUtils
import org.lineageos.settings.kernelmanager.KernelManagerViewModel
import org.lineageos.settings.memory.UnifiedMemoryManager
import org.lineageos.settings.refreshrate.RefreshUtils
import org.lineageos.settings.saturation.SaturationController
import org.lineageos.settings.thermal.ThermalUtils
import org.lineageos.settings.utils.KernelUtils

/**
 * Single boot entry point for Garnet Parts.
 *
 * Everything here is skipped when the running kernel is not 6.6: sysfs nodes with overlapping
 * names have different semantics on older kernels, and this build only targets 6.6.
 */
class GarnetBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }

        val release = System.getProperty("os.version", "unknown")
        if (!KernelUtils.isKernel66()) {
            Log.i(TAG, "Kernel $release is not 6.6, skipping Garnet Parts startup")
            return
        }

        // sysfs and binder writes must not block the main thread during boot.
        CoroutineScope(Dispatchers.IO).launch {
            if (action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
                // Services and charging mode must be active before the user unlocks.
                ThermalUtils.getInstance(context).startService()
                RefreshUtils.startService(context)
                ChargingRepository.applyOnBoot(context)
                overrideHdrTypes(context)
            } else {
                // SurfaceFlinger and cpufreq sysfs are ready after unlock.
                SaturationController(context).restore()
                RealityDisplayEngine.applyOnBoot(context)
                UnifiedMemoryManager.applyOnBoot(context)
                CoreControlUtils.applySaved(context)
                applyKernelSettings(context)
                applyGpuSettings(context)
            }
        }
    }

    private fun applyKernelSettings(context: Context) {
        val prefs = context.getSharedPreferences(
            KernelManagerViewModel.PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KernelManagerViewModel.PREF_APPLY_ON_BOOT, false)) return

        val governor = prefs.getString(KernelManagerViewModel.PREF_GOVERNOR, null) ?: return
        KernelManagerUtils.setGovernor(governor)

        val effMin = prefs.getString(KernelManagerViewModel.PREF_EFF_MIN, null)
        val effMax = prefs.getString(KernelManagerViewModel.PREF_EFF_MAX, null)
        if (effMin != null && effMax != null) {
            KernelManagerUtils.setFrequencyRange(
                KernelManagerUtils.EFFICIENCY_CLUSTER, effMin, effMax)
        }

        val perfMin = prefs.getString(KernelManagerViewModel.PREF_PERF_MIN, null)
        val perfMax = prefs.getString(KernelManagerViewModel.PREF_PERF_MAX, null)
        if (perfMin != null && perfMax != null) {
            KernelManagerUtils.setFrequencyRange(
                KernelManagerUtils.PERFORMANCE_CLUSTER, perfMin, perfMax)
        }
    }

    private fun applyGpuSettings(context: Context) {
        val prefs = context.getSharedPreferences(
            GpuManagerViewModel.PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(GpuManagerViewModel.PREF_APPLY_ON_BOOT, false)) return

        val utils = GpuManagerUtils()
        val governor = prefs.getString(GpuManagerViewModel.PREF_GOVERNOR, null) ?: return
        utils.setGovernor(governor)

        val minFreq = prefs.getString(GpuManagerViewModel.PREF_MIN_FREQ, null)
        val maxFreq = prefs.getString(GpuManagerViewModel.PREF_MAX_FREQ, null)
        if (minFreq != null && maxFreq != null) {
            utils.setFrequencyRange(minFreq, maxFreq)
        }

        utils.setForceClkOn(prefs.getBoolean(GpuManagerViewModel.PREF_FORCE_CLK_ON, false))
        utils.setForceBusOn(prefs.getBoolean(GpuManagerViewModel.PREF_FORCE_BUS_ON, false))
        utils.setForceRailOn(prefs.getBoolean(GpuManagerViewModel.PREF_FORCE_RAIL_ON, false))
        utils.setBusSplit(prefs.getBoolean(GpuManagerViewModel.PREF_BUS_SPLIT, true))
    }

    @SuppressWarnings("AndroidFrameworkRequiresPermission")
    private fun overrideHdrTypes(context: Context) {
        try {
            val dm = context.getSystemService(DisplayManager::class.java) ?: return
            dm.overrideHdrTypes(
                Display.DEFAULT_DISPLAY,
                intArrayOf(
                    Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION,
                    Display.HdrCapabilities.HDR_TYPE_HDR10,
                    Display.HdrCapabilities.HDR_TYPE_HLG,
                    Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS,
                ),
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error overriding HDR types", e)
        }
    }

    companion object {
        private const val TAG = "GarnetBootReceiver"
    }
}
