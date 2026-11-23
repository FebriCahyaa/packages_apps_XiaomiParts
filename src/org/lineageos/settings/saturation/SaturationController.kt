/*
 * Copyright (C) 2025 kenway214
 *           (C) 2026 zylhdrXP
 *           (C) 2026 FEBRIAN RAHMAD CAHYA
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.saturation

import android.content.Context
import android.os.IBinder
import android.os.Parcel
import android.os.RemoteException
import android.os.ServiceManager
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import org.lineageos.settings.Constants

/** Shared SurfaceFlinger saturation control for the settings screen and boot restore. */
class SaturationController(context: Context) {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
    private val surfaceFlinger: IBinder? = ServiceManager.getService("SurfaceFlinger")

    var value: Int = preferences.getInt(Constants.KEY_SATURATION, DEFAULT_VALUE)
        private set

    fun restore() = setValue(value)

    fun setValue(newValue: Int) {
        value = newValue.coerceIn(MIN_VALUE, MAX_VALUE)
        preferences.edit { putInt(Constants.KEY_SATURATION, value) }
        val saturation = if (value == DEFAULT_VALUE) 1.001f else value / 100.0f
        surfaceFlinger?.let { binder ->
            val data = Parcel.obtain()
            try {
                data.writeInterfaceToken("android.ui.ISurfaceComposer")
                data.writeFloat(saturation)
                binder.transact(TRANSACTION_SET_SATURATION, data, null, 0)
            } catch (exception: RemoteException) {
                Log.e(TAG, "Unable to update display saturation", exception)
            } finally {
                data.recycle()
            }
        }
    }

    fun reset() = setValue(DEFAULT_VALUE)

    companion object {
        const val DEFAULT_VALUE = 100
        const val MIN_VALUE = 0
        const val MAX_VALUE = 200
        private const val TRANSACTION_SET_SATURATION = 1022
        private const val TAG = "SaturationController"
    }
}
