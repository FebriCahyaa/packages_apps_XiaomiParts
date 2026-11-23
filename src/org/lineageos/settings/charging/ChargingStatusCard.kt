/*
 * Copyright (C) 2026 FEBRIAN RAHMAD CAHYA
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.charging

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.lineageos.settings.R

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
private fun StatusRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun ChargingStatusCard(status: ChargingStatus, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.charge_status_title),
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
            )
            StatusRow(
                label = stringResource(R.string.charge_status_source),
                value = stringResource(
                    if (status.plugged) R.string.charge_status_plugged
                    else R.string.charge_status_unplugged
                ),
            )
            StatusRow(
                label = stringResource(R.string.charge_status_quick_charge),
                value = quickChargeLabel(status.quickChargeType),
            )
            StatusRow(
                label = stringResource(R.string.charge_status_fast_mode),
                value = status.fastChargeMode?.toString()
                    ?: stringResource(R.string.charge_unknown),
            )
            StatusRow(
                label = stringResource(R.string.charge_status_adapter),
                value = status.adapterId?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.charge_unknown),
            )
            StatusRow(
                label = stringResource(R.string.charge_status_temperature),
                value = status.batteryTemperature?.let { "%.1f°C".format(it / 10f) }
                    ?: stringResource(R.string.charge_unknown),
            )
            StatusRow(
                label = stringResource(R.string.charge_status_kernel),
                value = System.getProperty("os.version", "unknown"),
            )
        }
    }
}
