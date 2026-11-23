/*
 * Copyright (C) 2026 FEBRIAN RAHMAD CAHYA
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.memory

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

/** Cycles the three UMM reclaim profiles. */
class UnifiedMemoryTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (!UnifiedMemoryManager.isSupported()) return
        val current = UnifiedMemoryManager.current(this)
        val next = MemoryProfile.values()[(current.ordinal + 1) % MemoryProfile.values().size]
        scope.launch {
            withContext(Dispatchers.IO) { UnifiedMemoryManager.setProfile(this@UnifiedMemoryTileService, next) }
            updateTile()
        }
    }

    private fun updateTile() {
        val profile = UnifiedMemoryManager.current(this)
        qsTile?.let { tile ->
            tile.state = if (UnifiedMemoryManager.isSupported()) Tile.STATE_ACTIVE
            else Tile.STATE_UNAVAILABLE
            tile.label = getString(labelFor(profile))
            tile.icon = Icon.createWithResource(this, R.drawable.ic_kernel_manager)
            tile.updateTile()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun labelFor(profile: MemoryProfile) = when (profile) {
        MemoryProfile.CONSERVATIVE -> R.string.umm_conservative
        MemoryProfile.BALANCED -> R.string.umm_balanced
        MemoryProfile.PERFORMANCE -> R.string.umm_performance
    }
}
