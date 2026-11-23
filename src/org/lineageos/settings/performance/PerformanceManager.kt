/*
 * Copyright (C) 2026 FEBRIAN RAHMAD CAHYA
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.performance

import android.content.Context
import org.lineageos.settings.gpumanager.GpuManagerUtils
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
        val cpuGovernor = when (profile) {
            PerformanceProfile.PERFORMANCE ->
                if ("performance" in KernelManagerUtils.getAvailableGovernors()) "performance"
                else KernelManagerUtils.defaultGovernor()
            else -> KernelManagerUtils.defaultGovernor()
        }

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
                PerformanceProfile.PERFORMANCE -> frequencies.getOrNull(frequencies.size / 2)
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
        val gpu = GpuManagerUtils()
        val frequencies = gpu.getAvailableFrequencies()
            ?.mapNotNull { it.toLongOrNull() }
            ?.sorted()
            ?: emptyList()
        val governors = gpu.getAvailableGovernors()

        val governor = when (profile) {
            PerformanceProfile.PERFORMANCE ->
                if ("performance" in governors) "performance" else governors.firstOrNull()
            else -> governors.firstOrNull { it == "msm-adreno-tz" } ?: governors.firstOrNull()
        } ?: return

        val min = when (profile) {
            PerformanceProfile.PERFORMANCE -> frequencies.lastOrNull()
            else -> frequencies.firstOrNull()
        }?.toString() ?: "0"
        val max = when (profile) {
            PerformanceProfile.BATTERY -> frequencies.getOrNull(frequencies.size / 3)
            else -> frequencies.lastOrNull()
        }?.toString() ?: "0"

        gpu.setGovernor(governor)
        gpu.setFrequencyRange(min, max)

        context.getSharedPreferences(
            GpuManagerViewModel.PREFS_NAME, Context.MODE_PRIVATE
        ).edit()
            .putString(GpuManagerViewModel.PREF_GOVERNOR, governor)
            .putString(GpuManagerViewModel.PREF_MIN_FREQ, min)
            .putString(GpuManagerViewModel.PREF_MAX_FREQ, max)
            .putBoolean(GpuManagerViewModel.PREF_APPLY_ON_BOOT, true)
            .apply()
    }
}
