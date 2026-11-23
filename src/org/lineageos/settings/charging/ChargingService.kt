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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Keeps the boosted charge profile applied across charger handshakes and pauses it when the
 * battery is hot. Registered once in onCreate so restarts cannot duplicate receivers.
 */
class ChargingService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val applyProfile = Runnable {
        ioScope.launch {
            val state = ChargingRepository.refresh(this@ChargingService)
            if (!state.plugged || state.bypass) return@launch
            ChargingRepository.applyBoostNodes(this@ChargingService)
            ChargingRepository.nudgeAuthentication(this@ChargingService)
        }
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
        ioScope.launch {
            val wasHot = ChargingRepository.hot
            ChargingRepository.setHot(this@ChargingService, false)
            if (wasHot) GpuSafety.restore(this@ChargingService)
            ChargingRepository.clearBoostNodes()
            ChargingRepository.refresh(this@ChargingService)
        }
    }

    private fun checkTemperature(intent: Intent) {
        val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        if (temp <= 0) return
        ioScope.launch {
            val shouldBeHot = when {
                !ChargingRepository.hot && temp >= ChargingRepository.TEMP_HOT -> true
                ChargingRepository.hot && temp <= ChargingRepository.TEMP_RESUME -> false
                else -> return@launch
            }
            Log.i(TAG, "Battery at ${temp / 10f}C, hot state $shouldBeHot")
            ChargingRepository.setHot(this@ChargingService, shouldBeHot)
            if (shouldBeHot) GpuSafety.suspend(this@ChargingService)
            else GpuSafety.restore(this@ChargingService)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        START_NOT_STICKY

    override fun onDestroy() {
        handler.removeCallbacks(applyProfile)
        unregisterReceiver(receiver)
        ioScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "ChargingService"
        private val APPLY_DELAYS_MS = longArrayOf(0, 2000, 5000, 12000, 25000)

        fun startIfNeeded(context: Context) {
            if (ChargingRepository.serviceNeeded(context)) {
                context.startService(Intent(context, ChargingService::class.java))
            }
        }
    }
}
