/*
 * Copyright (C) 2026 FEBRIAN RAHMAD CAHYA
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package org.lineageos.settings.memory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.lineageos.settings.R
import org.lineageos.settings.ui.components.GarnetPageHeading
import org.lineageos.settings.ui.components.GarnetStatusCard
import org.lineageos.settings.ui.components.GarnetStatusRow
import org.lineageos.settings.ui.components.GarnetTopAppBar
import org.lineageos.settings.ui.components.ModernSettingActionRow
import org.lineageos.settings.ui.components.ModernSettingSelector
import org.lineageos.settings.ui.components.SelectOption
import org.lineageos.settings.ui.components.connectedItemShape
import org.lineageos.settings.ui.theme.ExpressiveTheme

class UnifiedMemoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpressiveTheme {
                UnifiedMemoryScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
fun UnifiedMemoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf(MemoryStatus()) }

    LaunchedEffect(Unit) {
        while (true) {
            status = withContext(Dispatchers.IO) { UnifiedMemoryManager.refresh() }
            delay(2000)
        }
    }

    val options = listOf(
        SelectOption(MemoryProfile.CONSERVATIVE.key, stringResource(R.string.umm_conservative)),
        SelectOption(MemoryProfile.BALANCED.key, stringResource(R.string.umm_balanced)),
        SelectOption(MemoryProfile.PERFORMANCE.key, stringResource(R.string.umm_performance)),
    )

    Scaffold(
        topBar = {
            GarnetTopAppBar(
                title = stringResource(R.string.umm_title),
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
                title = stringResource(R.string.umm_title),
                subtitle = stringResource(R.string.umm_summary),
                modifier = Modifier.padding(top = 8.dp),
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = connectedItemShape(0, 2),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                ModernSettingSelector(
                    title = stringResource(R.string.umm_profile_title),
                    selectedValue = UnifiedMemoryManager.current(context).key,
                    options = options,
                    enabled = status.kernel66,
                    onValueSelected = { value ->
                        val profile = MemoryProfile.from(value)
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                UnifiedMemoryManager.setProfile(context, profile)
                            }
                        }
                    },
                    modifier = Modifier.padding(16.dp),
                )
            }

            ModernSettingActionRow(
                title = stringResource(R.string.umm_compact_title),
                summary = stringResource(R.string.umm_compact_summary),
                onClick = {
                    scope.launch { withContext(Dispatchers.IO) { UnifiedMemoryManager.triggerCompaction() } }
                },
                shape = connectedItemShape(1, 2),
            )

            GarnetStatusCard(title = stringResource(R.string.umm_physical_title)) {
                GarnetStatusRow(
                    stringResource(R.string.umm_total),
                    "${status.totalMb} MB",
                )
                GarnetStatusRow(
                    stringResource(R.string.umm_available),
                    "${status.availableMb} MB",
                )
                GarnetStatusRow(
                    stringResource(R.string.umm_dmabuf),
                    if (status.dmabufMb > 0) "${status.dmabufMb} MB"
                    else stringResource(R.string.rde_no),
                )
            }

            GarnetStatusCard(title = stringResource(R.string.umm_zram_title)) {
                GarnetStatusRow(
                    stringResource(R.string.umm_algorithm),
                    status.compAlgorithm.ifBlank { stringResource(R.string.charge_unknown) },
                )
                GarnetStatusRow(
                    stringResource(R.string.umm_orig),
                    "${status.zramOrigMb} MB",
                )
                GarnetStatusRow(
                    stringResource(R.string.umm_compressed),
                    "${status.zramCompressedMb} MB",
                )
                GarnetStatusRow(
                    stringResource(R.string.umm_ratio),
                    if (status.compressionRatio > 0) "%.2fx".format(status.compressionRatio)
                    else stringResource(R.string.charge_unknown),
                )
                GarnetStatusRow(
                    stringResource(R.string.umm_swap_used),
                    "${status.swapUsedMb} / ${status.swapTotalMb} MB",
                )
            }

            GarnetStatusCard(title = stringResource(R.string.umm_ufs_title)) {
                GarnetStatusRow(
                    stringResource(R.string.umm_writeback),
                    if (status.writebackEnabled) stringResource(R.string.rde_yes)
                    else stringResource(R.string.rde_no),
                )
                GarnetStatusRow(
                    stringResource(R.string.umm_backing),
                    status.writebackBacking.ifBlank { stringResource(R.string.umm_none) },
                )
                GarnetStatusRow(
                    stringResource(R.string.umm_limit),
                    "${status.writebackLimitPages / 256} MB",
                )
                GarnetStatusRow(
                    stringResource(R.string.umm_swappiness),
                    status.swappiness.toString(),
                )
                GarnetStatusRow(
                    stringResource(R.string.umm_compaction),
                    status.compaction.toString(),
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Text(
                    text = stringResource(R.string.umm_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(16.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
