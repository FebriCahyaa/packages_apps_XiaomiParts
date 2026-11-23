/*
 * Copyright (C) 2025 The LineageOS Project
 *           (C) 2026 zylhdrXP
 *           (C) 2026 FEBRIAN RAHMAD CAHYA
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.corecontrol

import android.content.Context
import java.io.File

/** Core online state with cluster safety rules, backed by the CPU hotplug sysfs interface. */
object CoreControlUtils {
    private const val ONLINE_PATH = "/sys/devices/system/cpu/cpu%d/online"
    private const val POSSIBLE_PATH = "/sys/devices/system/cpu/possible"
    private const val PREFS_NAME = "core_control_prefs"
    private const val PREF_STATES = "core_states"

    /** SM7435 has 4 little and 4 big CPUs; fall back to that when sysfs is unavailable. */
    val coreCount: Int
        get() = runCatching {
            val range = File(POSSIBLE_PATH).readText().trim()
            val last = range.substringAfterLast('-', range)
            last.toIntOrNull()?.plus(1) ?: 8
        }.getOrDefault(8)

    val littleCores: IntRange get() = 0 until minOf(4, coreCount)
    val bigCores: IntRange get() = minOf(4, coreCount) until coreCount

    fun onlinePath(core: Int): String = ONLINE_PATH.format(core)

    fun isCoreOnline(core: Int): Boolean {
        val path = onlinePath(core)
        if (!File(path).exists()) return true // CPU0 usually has no online node
        return runCatching { File(path).readText().trim() == "1" }.getOrDefault(true)
    }

    fun setCoreOnline(core: Int, online: Boolean): Boolean {
        val path = onlinePath(core)
        if (!File(path).exists()) return false
        return runCatching {
            File(path).writeText(if (online) "1" else "0")
            true
        }.getOrDefault(false)
    }

    /** Keep at least two little cores and one big core online. */
    fun canOffline(core: Int, states: Map<Int, Boolean>): Boolean = when (core) {
        in littleCores -> littleCores.count { index ->
            index != core && (states[index] ?: isCoreOnline(index))
        } >= 2
        in bigCores -> bigCores.count { index ->
            index != core && (states[index] ?: isCoreOnline(index))
        } >= 1
        else -> false
    }

    fun saveStates(context: Context, states: Map<Int, Boolean>) {
        val encoded = (0 until coreCount).joinToString(",") { states[it]?.let { b -> if (b) "1" else "0" } ?: "1" }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(PREF_STATES, encoded).apply()
    }

    fun applySaved(context: Context) {
        val encoded = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PREF_STATES, null) ?: return
        val values = encoded.split(',')
        for (core in 0 until minOf(coreCount, values.size)) {
            val desired = values[core] == "1"
            if (desired != isCoreOnline(core)) setCoreOnline(core, desired)
        }
    }
}
