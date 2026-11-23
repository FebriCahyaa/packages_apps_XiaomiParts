/*
 * Copyright (C) 2026 FEBRIAN RAHMAD CAHYA
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.performance

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.lineageos.settings.R
import org.lineageos.settings.ui.components.GarnetStatusCard
import org.lineageos.settings.ui.components.GarnetStatusRow

private fun mhz(hz: Long): String = if (hz > 0) "${hz / 1_000_000} MHz" else "--"

@Composable
fun GpuStatusCard(snapshot: GpuClockController.Snapshot, modifier: Modifier = Modifier) {
    GarnetStatusCard(
        title = stringResource(R.string.perf_hw_title),
        modifier = modifier,
    ) {
        GarnetStatusRow(
            stringResource(R.string.perf_gpu_model),
            stringResource(R.string.perf_gpu_adreno710),
        )
        GarnetStatusRow(
            stringResource(R.string.perf_gpu_current),
            mhz(snapshot.currentHz),
        )
        GarnetStatusRow(
            stringResource(R.string.perf_gpu_range),
            "${mhz(snapshot.minHz)} - ${mhz(snapshot.maxHz)}",
        )
        GarnetStatusRow(
            stringResource(R.string.perf_gpu_governor),
            snapshot.governor.ifBlank { stringResource(R.string.charge_unknown) },
        )
        GarnetStatusRow(
            stringResource(R.string.perf_gpu_levels),
            snapshot.powerLevels.toString(),
        )
        GarnetStatusRow(
            stringResource(R.string.perf_gpu_temp),
            if (snapshot.tempMilliC > 0) "${snapshot.tempMilliC / 1000}°C"
            else stringResource(R.string.charge_unknown),
        )
    }
}
