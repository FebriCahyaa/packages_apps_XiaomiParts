/*
 * Copyright (C) 2026 FEBRIAN RAHMAD CAHYA
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.charging

import android.content.Context
import org.lineageos.settings.gpumanager.GpuManagerUtils

/**
 * Pauses the GPU "force on" flags while the battery is hot.
 *
 * Those flags keep clocks, bus and rail powered, so leaving them enabled during a hot charging
 * session can overheat the device. The user's choices are stored before clearing the nodes and
 * restored when the battery cools down.
 */
object GpuSafety {
    private const val PREFS_NAME = "gpu_safety_prefs"
    private const val KEY_SUSPENDED = "suspended_flags"

    private const val FLAG_CLK = "clk"
    private const val FLAG_BUS = "bus"
    private const val FLAG_RAIL = "rail"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun suspendedFlags(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_SUSPENDED, emptySet()) ?: emptySet()

    fun suspend(context: Context) {
        if (suspendedFlags(context).isNotEmpty()) return
        val gpu = GpuManagerUtils()
        val flags = mutableSetOf<String>()
        if (gpu.getForceClkOn()) flags += FLAG_CLK
        if (gpu.getForceBusOn()) flags += FLAG_BUS
        if (gpu.getForceRailOn()) flags += FLAG_RAIL
        if (flags.isEmpty()) return

        prefs(context).edit().putStringSet(KEY_SUSPENDED, flags).apply()
        if (FLAG_CLK in flags) gpu.setForceClkOn(false)
        if (FLAG_BUS in flags) gpu.setForceBusOn(false)
        if (FLAG_RAIL in flags) gpu.setForceRailOn(false)
    }

    fun restore(context: Context) {
        val flags = suspendedFlags(context)
        if (flags.isEmpty()) return
        val gpu = GpuManagerUtils()
        if (FLAG_CLK in flags) gpu.setForceClkOn(true)
        if (FLAG_BUS in flags) gpu.setForceBusOn(true)
        if (FLAG_RAIL in flags) gpu.setForceRailOn(true)
        prefs(context).edit().remove(KEY_SUSPENDED).apply()
    }
}
