/*
 * Copyright (C) 2026 FEBRIAN RAHMAD CAHYA
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.display

import android.content.Context
import android.content.SharedPreferences
import android.hardware.display.DisplayManager
import android.provider.Settings
import android.view.Display
import org.lineageos.settings.saturation.SaturationController
import org.lineageos.settings.utils.KernelUtils

/** Picture presets coordinated across saturation, refresh rate and HDR output. */
enum class DisplayMode(
    val key: String,
    val saturation: Int,
    val peakRefresh: Float,
    val minRefresh: Float,
    val hdr: Boolean,
) {
    ADAPTIVE("adaptive", 100, 120f, 60f, true),
    VIVID("vivid", 115, 120f, 120f, true),
    NATURAL("natural", 100, 90f, 60f, false),
    CINEMATIC("cinematic", 110, 60f, 60f, true),
    READING("reading", 90, 60f, 60f, false);

    companion object {
        fun from(key: String?): DisplayMode =
            values().firstOrNull { it.key == key } ?: ADAPTIVE
    }
}

data class RealityStatus(
    val mode: DisplayMode = DisplayMode.ADAPTIVE,
    val kernel66: Boolean = false,
    val saturation: Int = 100,
    val peakRefresh: Float = 120f,
    val minRefresh: Float = 60f,
    val hdrOverride: Boolean = false,
    val localHbm: Boolean = false,
    val dynamicFps: String? = null,
)

/**
 * Reality Display Engine (RDE) is the single place where Garnet Parts applies
 * a coherent picture mode. It only writes interfaces already exported by the
 * 6.6 SurfaceFlinger/DisplayManager stack and the Xiaomi display module.
 */
object RealityDisplayEngine {
    private const val PREFS_NAME = "reality_display_prefs"
    private const val PREF_MODE = "mode"
    private const val KEY_PEAK = "peak_refresh_rate"
    private const val KEY_MIN = "min_refresh_rate"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun current(context: Context): DisplayMode = DisplayMode.from(prefs(context).getString(PREF_MODE, null))

    fun isSupported(): Boolean = KernelUtils.isKernel66()

    /** Finds a node exported by the Xiaomi DSI display module without hardcoding a SoC path. */
    private fun findNode(name: String): String? =
        runCatching {
            java.io.File("/sys/class/drm").walkTopDown(
                onFail = { _, _ -> },
                followLinks = true,
            ).maxDepth(4)
                .firstOrNull { it.isFile && it.name == name }
                ?.absolutePath
        }.getOrNull()

    fun apply(context: Context, mode: DisplayMode): Boolean {
        if (!isSupported()) return false
        SaturationController(context).setValue(mode.saturation)
        runCatching {
            Settings.System.putFloat(context.contentResolver, KEY_PEAK, mode.peakRefresh)
            Settings.System.putFloat(context.contentResolver, KEY_MIN, mode.minRefresh)
        }
        applyHdr(context, mode.hdr)
        prefs(context).edit().putString(PREF_MODE, mode.key).apply()
        return true
    }

    private fun applyHdr(context: Context, enabled: Boolean) {
        val dm = context.getSystemService(DisplayManager::class.java) ?: return
        val types = if (enabled) {
            intArrayOf(
                Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION,
                Display.HdrCapabilities.HDR_TYPE_HDR10,
                Display.HdrCapabilities.HDR_TYPE_HLG,
                Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS,
            )
        } else {
            intArrayOf()
        }
        runCatching { dm.overrideHdrTypes(Display.DEFAULT_DISPLAY, types) }
    }

    fun applyOnBoot(context: Context) {
        if (!isSupported()) return
        apply(context, current(context))
    }

    fun refresh(context: Context): RealityStatus {
        val mode = current(context)
        val caps = findNode("garnet_caps")?.let { runCatching { java.io.File(it).readText().trim() }.getOrNull() }
        return RealityStatus(
            mode = mode,
            kernel66 = isSupported(),
            saturation = SaturationController(context).value,
            peakRefresh = runCatching {
                Settings.System.getFloat(context.contentResolver, KEY_PEAK, mode.peakRefresh)
            }.getOrDefault(mode.peakRefresh),
            minRefresh = runCatching {
                Settings.System.getFloat(context.contentResolver, KEY_MIN, mode.minRefresh)
            }.getOrDefault(mode.minRefresh),
            hdrOverride = mode.hdr,
            localHbm = caps?.contains("lhbm=1") == true,
            dynamicFps = findNode("dynamic_fps")?.let {
                runCatching { java.io.File(it).readText().trim() }.getOrNull()
            },
        )
    }
}
