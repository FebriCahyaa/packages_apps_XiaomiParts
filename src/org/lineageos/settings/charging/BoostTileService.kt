/*
 * Copyright (C) 2026 FEBRIAN RAHMAD CAHYA
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.charging

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.lineageos.settings.R

class BoostTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartListening() {
        super.onStartListening()
        scope.launch {
            updateTile(ChargingRepository.refreshIo(this@BoostTileService))
        }
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            val before = ChargingRepository.refreshIo(this@BoostTileService)
            ChargingRepository.setBoostIo(this@BoostTileService, !before.boostRequested)
            updateTile(ChargingRepository.refreshIo(this@BoostTileService))
        }
    }

    private fun updateTile(state: ChargingStatus) {
        qsTile?.let { tile ->
            tile.state = when {
                !state.kernel66 -> Tile.STATE_UNAVAILABLE
                !state.boostRequested -> Tile.STATE_INACTIVE
                (state.hot && state.tempGuard) || state.bypass -> Tile.STATE_INACTIVE
                else -> Tile.STATE_ACTIVE
            }
            tile.label = getString(R.string.charge_boost_title)
            tile.icon = Icon.createWithResource(this, R.drawable.ic_charge_boost)
            tile.updateTile()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
