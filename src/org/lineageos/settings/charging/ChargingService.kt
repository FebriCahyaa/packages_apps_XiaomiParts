/*
 * Copyright (C) 2026 FEBRIAN RAHMAD CAHYA
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.charging

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log

/**
 * Keeps the boosted charge profile applied across charger handshakes and pauses it when the
 * battery is hot. Registered once in onCreate so restarts cannot duplicate receivers.
 */
class ChargingService : Service() {
    private val handler = Handler(Looper.getMainLooper())

    private val applyProfile = Runnable {
        val state = ChargingRepository.refresh(this)
        if (!state.plugged || state.bypass) return@Runnable
        ChargingRepository.applyBoostNodes(this)
        ChargingRepository.nudgeAuthentication(this)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> scheduleApply()
                Intent.ACTION_POWER_DISCONNECTED -> onUnplugged()
                Intent.ACTION_BATTERY_CHANGED -> checkTemperature(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        val sticky = registerReceiver(receiver, filter)
        if (sticky != null) {
            checkTemperature(sticky)
            if (sticky.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) > 0) scheduleApply()
        }
    }

    /** The charger renegotiates after plug-in, so the nodes need several writes. */
    private fun scheduleApply() {
        handler.removeCallbacks(applyProfile)
        APPLY_DELAYS_MS.forEach { handler.postDelayed(applyProfile, it) }
    }

    private fun onUnplugged() {
        handler.removeCallbacks(applyProfile)
        ChargingRepository.setHot(this, false)
        ChargingRepository.clearBoostNodes()
        ChargingRepository.refresh(this)
    }

    private fun checkTemperature(intent: Intent) {
        val guardEnabled = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getBoolean(PREF_TEMP_GUARD, true)
        if (!guardEnabled) return

        val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        if (temp <= 0) return

        val shouldBeHot = when {
            !ChargingRepository.hot && temp >= ChargingRepository.TEMP_HOT -> true
            ChargingRepository.hot && temp <= ChargingRepository.TEMP_RESUME -> false
            else -> return
        }
        Log.i(TAG, "Battery at ${temp / 10f}C, boost ${if (shouldBeHot) "held off" else "resumed"}")
        ChargingRepository.setHot(this, shouldBeHot)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        START_NOT_STICKY

    override fun onDestroy() {
        handler.removeCallbacks(applyProfile)
        unregisterReceiver(receiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "ChargingService"
        private const val PREFS_NAME = "charging_prefs"
        private const val PREF_TEMP_GUARD = "charge_temp_guard"
        private val APPLY_DELAYS_MS = longArrayOf(0, 2000, 5000, 12000, 25000)

        fun startIfNeeded(context: Context) {
            if (ChargingRepository.serviceNeeded(context)) {
                context.startService(Intent(context, ChargingService::class.java))
            }
        }
    }
}
