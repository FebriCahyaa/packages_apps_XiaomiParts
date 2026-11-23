/*
 * Copyright (C) 2025 KamiKaonashi
 *           (C) 2026 zylhdrXP
 *           (C) 2026 FEBRIAN RAHMAD CAHYA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.kernelmanager

import java.io.File

object KernelManagerUtils {
    const val EFFICIENCY_CLUSTER = 0
    const val PERFORMANCE_CLUSTER = 4

    /** schedutil is the governor shipped by the AOSP 6.6 kernel we build. */
    internal const val DEFAULT_GOVERNOR = "schedutil"

    private val POLICIES = intArrayOf(EFFICIENCY_CLUSTER, PERFORMANCE_CLUSTER)
    private val FALLBACK_GOVERNORS = listOf(DEFAULT_GOVERNOR, "performance", "powersave")

    private const val CPU_BASE_PATH = "/sys/devices/system/cpu/cpufreq/policy"
    private const val SCALING_GOVERNOR = "/scaling_governor"
    private const val SCALING_MIN_FREQ = "/scaling_min_freq"
    private const val SCALING_MAX_FREQ = "/scaling_max_freq"
    private const val SCALING_AVAILABLE_GOVERNORS = "/scaling_available_governors"
    private const val SCALING_AVAILABLE_FREQUENCIES = "/scaling_available_frequencies"

    private fun readList(path: String): List<String> =
        runCatching { File(path).readText().trim().split(Regex("\\s+")).filter { it.isNotEmpty() } }
            .getOrDefault(emptyList())

    private fun readValue(path: String): String =
        runCatching { File(path).readText().trim() }.getOrDefault("")

    private fun writeFile(path: String, value: String) {
        File(path).writeText(value)
    }

    fun getAvailableGovernors(cluster: Int = EFFICIENCY_CLUSTER): List<String> =
        readList(CPU_BASE_PATH + cluster + SCALING_AVAILABLE_GOVERNORS)
            .ifEmpty { FALLBACK_GOVERNORS }

    /** First available governor that is safe as a default for this cluster. */
    fun defaultGovernor(cluster: Int = EFFICIENCY_CLUSTER): String {
        val available = getAvailableGovernors(cluster)
        return if (DEFAULT_GOVERNOR in available) DEFAULT_GOVERNOR
        else available.firstOrNull() ?: DEFAULT_GOVERNOR
    }

    fun getAvailableFrequencies(cluster: Int): List<String>? =
        readList(CPU_BASE_PATH + cluster + SCALING_AVAILABLE_FREQUENCIES).ifEmpty { null }

    private fun sortedFrequencies(cluster: Int): List<Long> =
        getAvailableFrequencies(cluster)
            ?.mapNotNull { it.toLongOrNull() }
            ?.sorted()
            ?: emptyList()

    fun defaultMinFrequency(cluster: Int): String =
        sortedFrequencies(cluster).firstOrNull()?.toString() ?: "0"

    fun defaultMaxFrequency(cluster: Int): String =
        sortedFrequencies(cluster).lastOrNull()?.toString() ?: "0"

    fun getCurrentGovernor(cluster: Int): String =
        readValue(CPU_BASE_PATH + cluster + SCALING_GOVERNOR)
            .ifEmpty { defaultGovernor(cluster) }

    fun getCurrentMinFrequency(cluster: Int): String =
        readValue(CPU_BASE_PATH + cluster + SCALING_MIN_FREQ)
            .ifEmpty { defaultMinFrequency(cluster) }

    fun getCurrentMaxFrequency(cluster: Int): String =
        readValue(CPU_BASE_PATH + cluster + SCALING_MAX_FREQ)
            .ifEmpty { defaultMaxFrequency(cluster) }

    fun setGovernor(governor: String) {
        for (cluster in POLICIES) {
            val available = getAvailableGovernors(cluster)
            val target = if (governor in available) governor else defaultGovernor(cluster)
            runCatching { writeFile(CPU_BASE_PATH + cluster + SCALING_GOVERNOR, target) }
        }
    }

    fun setFrequencyRange(cluster: Int, minFreq: String, maxFreq: String) {
        var min = minFreq.toLongOrNull() ?: return
        var max = maxFreq.toLongOrNull() ?: return

        // Never write a frequency the cluster cannot use, and never let min > max.
        val valid = sortedFrequencies(cluster)
        if (valid.isNotEmpty()) {
            min = min.coerceIn(valid.first(), valid.last())
            max = max.coerceIn(valid.first(), valid.last())
        }
        if (min > max) return

        runCatching {
            // Write max before min to avoid the kernel rejecting min > current_max.
            writeFile(CPU_BASE_PATH + cluster + SCALING_MAX_FREQ, max.toString())
            writeFile(CPU_BASE_PATH + cluster + SCALING_MIN_FREQ, min.toString())
        }
    }

    fun resetToDefaults() {
        setGovernor(DEFAULT_GOVERNOR)
        for (cluster in POLICIES) {
            val frequencies = getAvailableFrequencies(cluster) ?: continue
            val min = frequencies.mapNotNull { it.toLongOrNull() }.minOrNull()?.toString()
                ?: continue
            val max = frequencies.mapNotNull { it.toLongOrNull() }.maxOrNull()?.toString()
                ?: continue
            setFrequencyRange(cluster, min, max)
        }
    }
}
