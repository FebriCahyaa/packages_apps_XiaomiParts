/*
 * Copyright (C) 2026 FEBRIAN RAHMAD CAHYA
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.charging

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import org.lineageos.settings.R

class BypassTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        ChargingRepository.setBypass(this, !ChargingRepository.refresh(this).bypass)
        updateTile()
    }

    private fun updateTile() {
        val state = ChargingRepository.refresh(this)
        qsTile?.let { tile ->
            tile.state = if (state.bypass) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.label = getString(R.string.charge_bypass_title)
            tile.icon = Icon.createWithResource(this, R.drawable.ic_charge)
            tile.updateTile()
        }
    }
}
