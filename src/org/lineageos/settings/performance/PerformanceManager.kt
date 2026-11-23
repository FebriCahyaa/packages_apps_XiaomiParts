/*
 * Copyright (C) 2026 FEBRIAN RAHMAD CAHYA
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.performance

import android.content.Context
import org.lineageos.settings.gpumanager.GpuManagerViewModel
import org.lineageos.settings.kernelmanager.KernelManagerUtils
import org.lineageos.settings.kernelmanager.KernelManagerViewModel
import org.lineageos.settings.utils.KernelUtils

enum class PerformanceProfile(val key: String) {
    BATTERY("battery"),
    BALANCED("balanced"),
    PERFORMANCE("performance");

    companion object {
        fun from(key: String?): PerformanceProfile =
            values().firstOrNull { it.key == key } ?: BALANCED
    }
}

/** Presets that apply and persist a coherent CPU+GPU configuration in one action. */
object PerformanceManager {
    private const val PREFS_NAME = "performance_prefs"
    private const val PREF_PROFILE = "profile"

    fun current(context: Context): PerformanceProfile =
        PerformanceProfile.from(
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(PREF_PROFILE, null)
        )

    fun set(context: Context, profile: PerformanceProfile): Boolean {
        if (!KernelUtils.isKernel66()) return false
        apply(context, profile)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(PREF_PROFILE, profile.key).apply()
        return true
    }

    private fun apply(context: Context, profile: PerformanceProfile) {
        applyCpu(context, profile)
        applyGpu(context, profile)
    }

    private fun applyCpu(context: Context, profile: PerformanceProfile) {
        val clusters = listOf(
            KernelManagerUtils.EFFICIENCY_CLUSTER,
            KernelManagerUtils.PERFORMANCE_CLUSTER,
        )
        // Keep schedutil for every profile. A static "performance" governor
        // pins the highest CPU OPP even when the screen is off or the phone is
        // idle, which only adds heat on a 7s Gen 2. We raise the floor instead
        // so the cores are already warm when work arrives.
        val cpuGovernor = KernelManagerUtils.defaultGovernor()

        val prefs = context.getSharedPreferences(
            KernelManagerViewModel.PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
            .putString(KernelManagerViewModel.PREF_GOVERNOR, cpuGovernor)
            .putBoolean(KernelManagerViewModel.PREF_APPLY_ON_BOOT, true)

        for (cluster in clusters) {
            val frequencies = KernelManagerUtils.getAvailableFrequencies(cluster)
                ?.mapNotNull { it.toLongOrNull() }
                ?.sorted()
                ?: emptyList()

            val min = when (profile) {
                PerformanceProfile.PERFORMANCE ->
                    frequencies.getOrNull((frequencies.size * 3 / 5).coerceIn(0, frequencies.lastIndex))
                else -> frequencies.firstOrNull()
            }?.toString() ?: KernelManagerUtils.defaultMinFrequency(cluster)

            val max = when (profile) {
                PerformanceProfile.BATTERY -> frequencies.getOrNull(frequencies.size / 3)
                else -> frequencies.lastOrNull()
            }?.toString() ?: KernelManagerUtils.defaultMaxFrequency(cluster)

            KernelManagerUtils.setFrequencyRange(cluster, min, max)
            if (cluster == KernelManagerUtils.EFFICIENCY_CLUSTER) {
                editor.putString(KernelManagerViewModel.PREF_EFF_MIN, min)
                editor.putString(KernelManagerViewModel.PREF_EFF_MAX, max)
            } else {
                editor.putString(KernelManagerViewModel.PREF_PERF_MIN, min)
                editor.putString(KernelManagerViewModel.PREF_PERF_MAX, max)
            }
        }
        editor.apply()

        // setGovernor writes both policies after the frequency ranges are valid.
        KernelManagerUtils.setGovernor(cpuGovernor)
    }

    private fun applyGpu(context: Context, profile: PerformanceProfile) {
        GpuClockController.apply(profile)
        val gpu = GpuClockController.snapshot()

        // Mirror the result into the legacy GPU Manager prefs so both screens
        // show the same limits. The governor is read-only in 6.6 KGSL and is
        // therefore persisted as-is instead of being "set" to a fake value.
        context.getSharedPreferences(
            GpuManagerViewModel.PREFS_NAME, Context.MODE_PRIVATE
        ).edit()
            .putString(GpuManagerViewModel.PREF_GOVERNOR, gpu.governor)
            .putString(GpuManagerViewModel.PREF_MIN_FREQ, gpu.minHz.toString())
            .putString(GpuManagerViewModel.PREF_MAX_FREQ, gpu.maxHz.toString())
            .putBoolean(GpuManagerViewModel.PREF_BUS_SPLIT, true)
            .putBoolean(GpuManagerViewModel.PREF_APPLY_ON_BOOT, true)
            .apply()

        // When apply() is refused (hot GPU or missing sysfs) the current limits
        // are mirrored instead, so no unsafe value is persisted.
    }
}
