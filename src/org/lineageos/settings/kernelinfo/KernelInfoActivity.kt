/*
 * Copyright (C) 2026 FEBRIAN RAHMAD CAHYA
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package org.lineageos.settings.kernelinfo

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.lineageos.settings.R
import org.lineageos.settings.charging.ChargingRepository
import org.lineageos.settings.gpumanager.GpuManagerUtils
import org.lineageos.settings.kernelmanager.KernelManagerUtils
import org.lineageos.settings.ui.components.GarnetPageHeading
import org.lineageos.settings.ui.components.GarnetTopAppBar
import org.lineageos.settings.ui.theme.ExpressiveTheme
import org.lineageos.settings.utils.KernelUtils

class KernelInfoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpressiveTheme {
                KernelInfoScreen(onBack = { finish() })
            }
        }
    }
}

private data class KernelInfo(
    val release: String = "",
    val model: String = "",
    val board: String = "",
    val hardware: String = "",
    val governor: String = "",
    val gpu: String = "",
    val bypass: Boolean = false,
    val boost: Boolean = false,
    val aggregateNode: Boolean = false,
)

@Composable
private fun InfoRow(label: String, value: String) {
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
fun KernelInfoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var info by remember { mutableStateOf(KernelInfo()) }
    val supported = KernelUtils.isKernel66()

    LaunchedEffect(Unit) {
        info = withContext(Dispatchers.IO) {
            KernelInfo(
                release = System.getProperty("os.version", "unknown"),
                model = Build.MODEL,
                board = Build.BOARD,
                hardware = Build.HARDWARE,
                governor = KernelManagerUtils.getCurrentGovernor(),
                gpu = runCatching { GpuManagerUtils().getGpuModel() }.getOrDefault(""),
                bypass = ChargingRepository.isBypassSupported(),
                boost = ChargingRepository.isBoostSupported(),
                aggregateNode = ChargingRepository.hasAggregateStatusNode(),
            )
        }
    }

    val yes = stringResource(R.string.kernel_info_yes)
    val no = stringResource(R.string.kernel_info_no)

    Scaffold(
        topBar = {
            GarnetTopAppBar(
                title = stringResource(R.string.kernel_info_title),
                showTitle = scrollState.value > 120,
                onBack = onBack,
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GarnetPageHeading(
                title = stringResource(R.string.kernel_info_title),
                subtitle = stringResource(R.string.kernel_info_summary),
                modifier = Modifier.padding(top = 8.dp),
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.kernel_info_build),
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    InfoRow(stringResource(R.string.kernel_info_release), info.release)
                    InfoRow(stringResource(R.string.kernel_info_model), info.model)
                    InfoRow(stringResource(R.string.kernel_info_board), info.board)
                    InfoRow(stringResource(R.string.kernel_info_hardware), info.hardware)
                    InfoRow(stringResource(R.string.kernel_info_governor), info.governor)
                    InfoRow(stringResource(R.string.kernel_info_gpu), info.gpu)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.kernel_info_nodes),
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    InfoRow(
                        stringResource(R.string.charge_requires_66),
                        if (supported) yes else no,
                    )
                    InfoRow(
                        stringResource(R.string.kernel_info_bypass_node),
                        if (info.bypass) yes else no,
                    )
                    InfoRow(
                        stringResource(R.string.kernel_info_boost_node),
                        if (info.boost) yes else no,
                    )
                    InfoRow(
                        stringResource(R.string.kernel_info_aggregate_node),
                        if (info.aggregateNode) yes else no,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
