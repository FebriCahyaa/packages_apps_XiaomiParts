/*
 * Copyright (C) 2026 FEBRIAN RAHMAD CAHYA
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.performance

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import org.lineageos.settings.R
import org.lineageos.settings.utils.KernelUtils

/** Cycles battery -> balanced -> performance -> battery. */
class PerformanceTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (!KernelUtils.isKernel66()) return
        val next = when (PerformanceManager.current(this)) {
            PerformanceProfile.BATTERY -> PerformanceProfile.BALANCED
            PerformanceProfile.BALANCED -> PerformanceProfile.PERFORMANCE
            PerformanceProfile.PERFORMANCE -> PerformanceProfile.BATTERY
        }
        PerformanceManager.set(this, next)
        updateTile()
    }

    private fun updateTile() {
        val profile = PerformanceManager.current(this)
        qsTile?.let { tile ->
            tile.state = when {
                !KernelUtils.isKernel66() -> Tile.STATE_UNAVAILABLE
                profile == PerformanceProfile.PERFORMANCE -> Tile.STATE_ACTIVE
                else -> Tile.STATE_INACTIVE
            }
            tile.label = getString(profile.labelRes())
            tile.icon = Icon.createWithResource(this, R.drawable.ic_cpu)
            tile.updateTile()
        }
    }

    private fun PerformanceProfile.labelRes() = when (this) {
        PerformanceProfile.BATTERY -> R.string.perf_battery
        PerformanceProfile.BALANCED -> R.string.perf_balanced
        PerformanceProfile.PERFORMANCE -> R.string.perf_performance
    }
}
