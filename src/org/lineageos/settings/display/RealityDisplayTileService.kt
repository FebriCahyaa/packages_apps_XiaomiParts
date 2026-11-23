/*
 * Copyright (C) 2026 FEBRIAN RAHMAD CAHYA
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.display

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.lineageos.settings.R

/** Cycles the five Reality Display Engine presets. */
class RealityDisplayTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (!RealityDisplayEngine.isSupported()) return
        val current = RealityDisplayEngine.current(this)
        val next = DisplayMode.values()[(current.ordinal + 1) % DisplayMode.values().size]
        scope.launch {
            withContext(Dispatchers.IO) { RealityDisplayEngine.apply(this@RealityDisplayTileService, next) }
            updateTile()
        }
    }

    private fun updateTile() {
        val mode = RealityDisplayEngine.current(this)
        qsTile?.let { tile ->
            tile.state = if (RealityDisplayEngine.isSupported()) Tile.STATE_ACTIVE
            else Tile.STATE_UNAVAILABLE
            tile.label = getString(labelFor(mode))
            tile.icon = Icon.createWithResource(this, R.drawable.ic_saturation_tile)
            tile.updateTile()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun labelFor(mode: DisplayMode) = when (mode) {
        DisplayMode.ADAPTIVE -> R.string.rde_mode_adaptive
        DisplayMode.VIVID -> R.string.rde_mode_vivid
        DisplayMode.NATURAL -> R.string.rde_mode_natural
        DisplayMode.CINEMATIC -> R.string.rde_mode_cinematic
        DisplayMode.READING -> R.string.rde_mode_reading
    }
}
