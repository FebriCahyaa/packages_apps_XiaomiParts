/*
 * Copyright (C) 2026 FEBRIAN RAHMAD CAHYA
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.memory

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.io.File
import org.lineageos.settings.utils.KernelUtils

/** VM reclaim presets. Tier 3 (zram writeback to UFS) is deliberately manual only. */
enum class MemoryProfile(val key: String, val swappiness: Int, val compaction: Int) {
    CONSERVATIVE("conservative", 60, 10),
    BALANCED("balanced", 100, 20),
    PERFORMANCE("performance", 150, 40);

    companion object {
        fun from(key: String?): MemoryProfile =
            values().firstOrNull { it.key == key } ?: BALANCED
    }
}

data class MemoryStatus(
    val kernel66: Boolean = false,
    val totalMb: Long = 0,
    val availableMb: Long = 0,
    val swapTotalMb: Long = 0,
    val swapUsedMb: Long = 0,
    val zramOrigMb: Long = 0,
    val zramCompressedMb: Long = 0,
    val zramUsedMb: Long = 0,
    val compressionRatio: Float = 0f,
    val swappiness: Int = 100,
    val compaction: Int = 20,
    val writebackEnabled: Boolean = false,
    val writebackBacking: String = "",
    val writebackLimitPages: Long = 0,
    val compAlgorithm: String = "",
    val dmabufMb: Long = 0,
)

/**
 * Unified Memory Manager (UMM) presents LPDDR4X, LZ4 zram, VM reclaim and UFS
 * writeback as one hierarchy. It adjusts only Tier 2; Tier 3 is configured by
 * the kernel's garnet_umm_setup.sh helper and is never automated here.
 */
object UnifiedMemoryManager {
    private const val TAG = "UnifiedMemoryManager"
    private const val PREFS_NAME = "umm_prefs"
    private const val PREF_PROFILE = "profile"

    private const val ZRAM = "/sys/block/zram0"
    private const val SWAPPINESS = "/proc/sys/vm/swappiness"
    private const val COMPACTION = "/proc/sys/vm/compaction_proactiveness"
    private const val COMPACT = "/proc/sys/vm/compact_memory"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun current(context: Context): MemoryProfile =
        MemoryProfile.from(prefs(context).getString(PREF_PROFILE, null))

    fun isSupported(): Boolean = KernelUtils.isKernel66()

    private fun readLine(path: String): String? =
        runCatching { File(path).readText().trim() }.getOrNull()

    private fun readLong(path: String): Long = readLine(path)?.toLongOrNull() ?: 0

    private fun meminfo(): Map<String, Long> = runCatching {
        File("/proc/meminfo").readLines().associate { line ->
            val parts = line.split(':')
            parts[0] to (parts.getOrNull(1)?.trim()?.split(' ')?.firstOrNull()?.toLongOrNull() ?: 0)
        }
    }.getOrDefault(emptyMap())

    private fun swapTotals(): Pair<Long, Long> = runCatching {
        File("/proc/swaps").readLines()
            .drop(1)
            .mapNotNull { line -> line.trim().split(Regex("\\s+")).takeIf { it.size >= 5 } }
            .fold(0L to 0L) { acc, parts ->
                acc.copy(
                    first = acc.first + (parts[2].toLongOrNull() ?: 0),
                    second = acc.second + (parts[3].toLongOrNull() ?: 0),
                )
            }
    }.getOrDefault(0L to 0L)

    fun refresh(): MemoryStatus {
        val info = meminfo()
        val swap = swapTotals()
        val orig = readLong("$ZRAM/orig_data_size")
        val compressed = readLong("$ZRAM/compr_data_size")
        val backing = readLine("$ZRAM/backing_dev").orEmpty()
        return MemoryStatus(
            kernel66 = isSupported(),
            totalMb = (info["MemTotal"] ?: 0) / 1024,
            availableMb = (info["MemAvailable"] ?: info["MemFree"] ?: 0) / 1024,
            swapTotalMb = swap.first / 1024,
            swapUsedMb = swap.second / 1024,
            zramOrigMb = orig / (1024 * 1024),
            zramCompressedMb = compressed / (1024 * 1024),
            zramUsedMb = readLong("$ZRAM/mem_used_total") / (1024 * 1024),
            compressionRatio = if (compressed > 0) orig.toFloat() / compressed else 0f,
            swappiness = readLong(SWAPPINESS).toInt(),
            compaction = readLong(COMPACTION).toInt(),
            writebackEnabled = readLong("$ZRAM/writeback_limit_enable") == 1L,
            writebackBacking = backing,
            writebackLimitPages = readLong("$ZRAM/writeback_limit"),
            compAlgorithm = readLine("$ZRAM/comp_algorithm").orEmpty(),
            dmabufMb = readLong("/sys/kernel/dmabuf/total_size") / (1024 * 1024),
        )
    }

    fun setProfile(context: Context, profile: MemoryProfile): Boolean {
        if (!isSupported()) return false
        val ok = write(SWAPPINESS, profile.swappiness.toString()) &&
            write(COMPACTION, profile.compaction.toString())
        if (!ok) {
            Log.w(TAG, "Unable to write VM tunables; SELinux may deny sysctl access")
            return false
        }
        prefs(context).edit().putString(PREF_PROFILE, profile.key).apply()
        return true
    }

    fun applyOnBoot(context: Context) {
        if (!isSupported()) return
        val profile = current(context)
        setProfile(context, profile)
    }

    fun triggerCompaction(): Boolean = write(COMPACT, "1")

    private fun write(path: String, value: String): Boolean = runCatching {
        File(path).writeText(value)
        true
    }.getOrDefault(false)
}
