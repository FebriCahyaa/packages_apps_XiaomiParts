/*
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.thermal

import android.app.ActivityTaskManager
import android.app.Service
import android.app.TaskStackListener
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import org.lineageos.settings.utils.Logging

/** Service to monitor current top (foreground) app and set thermal profile accordingly. */
class ThermalService : Service() {

    private lateinit var thermalUtils: ThermalUtils
    private var listenersRegistered = false

    private var currentApp = ""
        set(value) {
            if (field == value) return
            field = value
            Logging.d(TAG, "Top app changed: $value")
            setThermalProfile()
        }

    private var screenOn = true
        set(value) {
            if (field == value) return
            field = value
            Logging.d(TAG, "Screen state changed: $value")
            setThermalProfile()
        }

    private val taskListener =
        object : TaskStackListener() {
            override fun onTaskStackChanged() {
                runCatching {
                    val focusedTask = ActivityTaskManager.getService().focusedRootTaskInfo
                    focusedTask?.topActivity?.let { currentApp = it.packageName }
                }
            }
        }

    private val intentReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_SCREEN_OFF -> screenOn = false
                    Intent.ACTION_SCREEN_ON -> screenOn = true
                }
            }
        }

    override fun onCreate() {
        Logging.d(TAG, "Creating service")
        thermalUtils = ThermalUtils.getInstance(this)
        super.onCreate()
        if (listenersRegistered) return
        runCatching { ActivityTaskManager.getService().registerTaskStackListener(taskListener) }
        registerReceiver(
            intentReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            },
        )
        listenersRegistered = true
    }

    override fun onDestroy() {
        Logging.d(TAG, "Destroying service")
        if (listenersRegistered) {
            unregisterReceiver(intentReceiver)
            runCatching { ActivityTaskManager.getService().unregisterTaskStackListener(taskListener) }
            listenersRegistered = false
        }
        super.onDestroy()
    }

    // START_NOT_STICKY: ThermalUtils.stopService() must actually stop monitoring.
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logging.d(TAG, "Starting service")
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun setThermalProfile() {
        if (screenOn) {
            thermalUtils.setThermalProfile(currentApp)
        } else {
            thermalUtils.setDefaultThermalProfile()
        }
    }

    companion object {
        private const val TAG = "ThermalService"
    }
}
