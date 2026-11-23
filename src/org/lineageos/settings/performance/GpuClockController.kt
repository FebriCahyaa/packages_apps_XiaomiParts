/*
 * Copyright (C) 2026 FEBRIAN RAHMAD CAHYA
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.performance

import java.io.File

/**
 * Adreno 710 clock control for the 6.6 KGSL driver on SM7435.
 *
 * The GPU governor (msm-adreno-tz) is flagged DEVFREQ_GOV_FLAG_IMMUTABLE in
 * this kernel, so it cannot be switched from userspace and must not be treated
 * as a setting. Performance is controlled with the devfreq frequency bounds
 * and the KGSL pwrlevel window instead; the TrustZone governor keeps thermal
 * control. All frequencies come from gpu_available_frequencies, never from a
 * hardcoded table, so the same code works with any GPU speed bin.
 */
object GpuClockController {
    private const val BASE = "/sys/class/kgsl/kgsl-3d0"
    private const val AVAILABLE = "$BASE/gpu_available_frequencies"
    private const val CURRENT = "$BASE/gpuclk"
    private const val GOVERNOR = "$BASE/devfreq/governor"
    private const val MIN_FREQ = "$BASE/devfreq/min_freq"
    private const val MAX_FREQ = "$BASE/devfreq/max_freq"
    private const val TEMP = "$BASE/temp"
    private const val BUSY = "$BASE/gpu_busy_percentage"
    private const val NUM_LEVELS = "$BASE/num_pwrlevels"
    private const val BUS_SPLIT = "$BASE/bus_split"
    private const val FORCE_CLK = "$BASE/force_clk_on"

    /** Millidegrees Celsius, from the GPU thermal zones used by the driver. */
    private const val THERMAL_LIMIT_MC = 70_000

    data class Snapshot(
        val governor: String = "",
        val available: List<Long> = emptyList(),
        val currentHz: Long = 0,
        val minHz: Long = 0,
        val maxHz: Long = 0,
        val tempMilliC: Long = 0,
        val busyPercent: Long = 0,
        val powerLevels: Long = 0,
    )

    private fun read(path: String): String =
        runCatching { File(path).readText().trim() }.getOrDefault("")

    private fun readLong(path: String): Long = read(path).toLongOrNull() ?: 0

    private fun write(path: String, value: String): Boolean = runCatching {
        File(path).writeText(value)
        true
    }.getOrDefault(false)

    fun snapshot(): Snapshot = Snapshot(
        governor = read(GOVERNOR),
        available = read(AVAILABLE).split(Regex("\\s+"))
            .mapNotNull { it.toLongOrNull() }
            .sorted(),
        currentHz = readLong(CURRENT),
        minHz = readLong(MIN_FREQ),
        maxHz = readLong(MAX_FREQ),
        tempMilliC = readLong(TEMP),
        busyPercent = readLong(BUSY),
        powerLevels = readLong(NUM_LEVELS),
    )

    /** Returns false when the GPU is too hot for the requested upper bound. */
    fun apply(profile: PerformanceProfile): Boolean {
        val s = snapshot()
        if (s.available.isEmpty()) return false
        if (profile == PerformanceProfile.PERFORMANCE && s.tempMilliC > THERMAL_LIMIT_MC) {
            return false
        }

        val min = when (profile) {
            PerformanceProfile.BATTERY -> s.available.first()
            PerformanceProfile.BALANCED -> s.available.first()
            // Raise the floor without locking the clock: the TZ governor still
            // decides the exact level from GPU utilisation.
            PerformanceProfile.PERFORMANCE ->
                s.available[(s.available.size * 3 / 5).coerceIn(0, s.available.lastIndex)]
        }
        val max = when (profile) {
            PerformanceProfile.BATTERY -> s.available[(s.available.size / 3).coerceIn(0, s.available.lastIndex)]
            else -> s.available.last()
        }

        // Max first so the kernel never sees min > current max.
        if (!write(MAX_FREQ, max.toString())) return false
        if (!write(MIN_FREQ, min.toString())) return false
        // Independent bus votes help sustained rendering; clock forcing stays
        // off so idle GPU can still drop its rail.
        write(BUS_SPLIT, "1")
        write(FORCE_CLK, "0")
        return true
    }

    fun reset(): Boolean {
        val s = snapshot()
        if (s.available.isEmpty()) return false
        return write(MAX_FREQ, s.available.last().toString()) &&
            write(MIN_FREQ, s.available.first().toString())
    }
}
