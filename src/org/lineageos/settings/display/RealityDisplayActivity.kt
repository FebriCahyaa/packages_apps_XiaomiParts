/*
 * Copyright (C) 2026 FEBRIAN RAHMAD CAHYA
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package org.lineageos.settings.display

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
import org.lineageos.settings.ui.components.ModernSettingSelector
import org.lineageos.settings.ui.components.SelectOption
import org.lineageos.settings.ui.components.connectedItemShape
import org.lineageos.settings.ui.theme.ExpressiveTheme

class RealityDisplayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpressiveTheme {
                RealityDisplayScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
fun RealityDisplayScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf(RealityStatus()) }

    LaunchedEffect(Unit) {
        while (true) {
            status = withContext(Dispatchers.IO) { RealityDisplayEngine.refresh(context) }
            delay(2000)
        }
    }

    val options = listOf(
        SelectOption(DisplayMode.ADAPTIVE.key, stringResource(R.string.rde_mode_adaptive)),
        SelectOption(DisplayMode.VIVID.key, stringResource(R.string.rde_mode_vivid)),
        SelectOption(DisplayMode.NATURAL.key, stringResource(R.string.rde_mode_natural)),
        SelectOption(DisplayMode.CINEMATIC.key, stringResource(R.string.rde_mode_cinematic)),
        SelectOption(DisplayMode.READING.key, stringResource(R.string.rde_mode_reading)),
    )

    Scaffold(
        topBar = {
            GarnetTopAppBar(
                title = stringResource(R.string.rde_title),
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
                title = stringResource(R.string.rde_title),
                subtitle = stringResource(R.string.rde_summary),
                modifier = Modifier.padding(top = 8.dp),
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = connectedItemShape(0, 1),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                ModernSettingSelector(
                    title = stringResource(R.string.rde_mode_title),
                    selectedValue = status.mode.key,
                    options = options,
                    enabled = status.kernel66,
                    onValueSelected = { value ->
                        val mode = DisplayMode.from(value)
                        scope.launch {
                            val applied = withContext(Dispatchers.IO) {
                                RealityDisplayEngine.apply(context, mode)
                            }
                            if (applied) {
                                status = withContext(Dispatchers.IO) {
                                    RealityDisplayEngine.refresh(context)
                                }
                            }
                        }
                    },
                    modifier = Modifier.padding(16.dp),
                )
            }

            GarnetStatusCard(title = stringResource(R.string.rde_status_title)) {
                GarnetStatusRow(
                    stringResource(R.string.rde_status_kernel),
                    if (status.kernel66) stringResource(R.string.rde_yes)
                    else stringResource(R.string.charge_requires_66),
                )
                GarnetStatusRow(
                    stringResource(R.string.rde_status_saturation),
                    "${status.saturation}%",
                )
                GarnetStatusRow(
                    stringResource(R.string.rde_status_peak),
                    "${status.peakRefresh.toInt()} Hz",
                )
                GarnetStatusRow(
                    stringResource(R.string.rde_status_min),
                    "${status.minRefresh.toInt()} Hz",
                )
                GarnetStatusRow(
                    stringResource(R.string.rde_status_hdr),
                    if (status.hdrOverride) stringResource(R.string.rde_yes)
                    else stringResource(R.string.rde_no),
                )
                GarnetStatusRow(
                    stringResource(R.string.rde_status_lhbm),
                    if (status.localHbm) stringResource(R.string.rde_yes)
                    else stringResource(R.string.rde_no),
                )
                GarnetStatusRow(
                    stringResource(R.string.rde_status_fps),
                    status.dynamicFps ?: stringResource(R.string.charge_unknown),
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = connectedItemShape(1, 1),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Text(
                    text = stringResource(R.string.rde_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
