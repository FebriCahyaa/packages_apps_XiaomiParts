/*
 * Copyright (C) 2026 FEBRIAN RAHMAD CAHYA
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.charging

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.lineageos.settings.R
import org.lineageos.settings.ui.components.GarnetStatusCard
import org.lineageos.settings.ui.components.GarnetStatusRow

@Composable
private fun quickChargeLabel(type: Int?): String = when (type) {
    null -> stringResource(R.string.charge_unknown)
    0 -> stringResource(R.string.charge_quick_normal)
    1 -> stringResource(R.string.charge_quick_fast)
    2 -> stringResource(R.string.charge_quick_flash)
    3 -> stringResource(R.string.charge_quick_turbo)
    4 -> stringResource(R.string.charge_quick_super)
    5 -> stringResource(R.string.charge_quick_max)
    else -> type.toString()
}

@Composable
fun ChargingStatusCard(status: ChargingStatus, modifier: Modifier = Modifier) {
    GarnetStatusCard(
        title = stringResource(R.string.charge_status_title),
        modifier = modifier,
    ) {
        GarnetStatusRow(
            stringResource(R.string.charge_status_source),
            stringResource(
                if (status.plugged) R.string.charge_status_plugged
                else R.string.charge_status_unplugged
            ),
        )
        GarnetStatusRow(
            stringResource(R.string.charge_status_quick_charge),
            quickChargeLabel(status.quickChargeType),
        )
        GarnetStatusRow(
            stringResource(R.string.charge_status_fast_mode),
            status.fastChargeMode?.toString() ?: stringResource(R.string.charge_unknown),
        )
        GarnetStatusRow(
            stringResource(R.string.charge_status_adapter),
            status.adapterId?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.charge_unknown),
        )
        GarnetStatusRow(
            stringResource(R.string.charge_status_temperature),
            status.batteryTemperature?.let { "%.1f°C".format(it / 10f) }
                ?: stringResource(R.string.charge_unknown),
        )
        GarnetStatusRow(
            stringResource(R.string.charge_status_kernel),
            System.getProperty("os.version", "unknown"),
        )
    }
}
