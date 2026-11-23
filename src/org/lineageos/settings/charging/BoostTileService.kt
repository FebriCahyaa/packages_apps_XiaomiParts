/*
 * Copyright (C) 2026 FEBRIAN RAHMAD CAHYA
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.charging

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import org.lineageos.settings.R

class BoostTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val state = ChargingRepository.refresh(this)
        ChargingRepository.setBoost(this, !state.boostRequested)
        updateTile()
    }

    private fun updateTile() {
        val state = ChargingRepository.refresh(this)
        qsTile?.let { tile ->
            tile.state = when {
                !state.kernel66 -> Tile.STATE_UNAVAILABLE
                !state.boostRequested -> Tile.STATE_INACTIVE
                state.hot || state.bypass -> Tile.STATE_INACTIVE
                else -> Tile.STATE_ACTIVE
            }
            tile.label = getString(R.string.charge_boost_title)
            tile.icon = Icon.createWithResource(this, R.drawable.ic_charge_boost)
            tile.updateTile()
        }
    }
}
