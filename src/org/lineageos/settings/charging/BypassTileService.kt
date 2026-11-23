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

class BypassTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartListening() {
        super.onStartListening()
        scope.launch {
            updateTile(ChargingRepository.refreshIo(this@BypassTileService))
        }
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            val before = ChargingRepository.refreshIo(this@BypassTileService)
            ChargingRepository.setBypassIo(this@BypassTileService, !before.bypass)
            updateTile(ChargingRepository.refreshIo(this@BypassTileService))
        }
    }

    private fun updateTile(state: ChargingStatus) {
        qsTile?.let { tile ->
            tile.state = if (state.bypass) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.label = getString(R.string.charge_bypass_title)
            tile.icon = Icon.createWithResource(this, R.drawable.ic_charge)
            tile.updateTile()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
