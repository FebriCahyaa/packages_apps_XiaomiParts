/*
 * Copyright (C) 2026 FEBRIAN RAHMAD CAHYA
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package org.lineageos.settings.performance

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.lineageos.settings.R
import org.lineageos.settings.ui.components.GarnetPageHeading
import org.lineageos.settings.ui.components.GarnetTopAppBar
import org.lineageos.settings.ui.components.ModernSettingSelector
import org.lineageos.settings.ui.components.SelectOption
import org.lineageos.settings.ui.components.connectedItemShape
import org.lineageos.settings.ui.theme.ExpressiveTheme
import org.lineageos.settings.utils.KernelUtils

class PerformanceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpressiveTheme {
                PerformanceScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
fun PerformanceScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var profile by remember { mutableStateOf(PerformanceManager.current(context)) }
    val supported = KernelUtils.isKernel66()

    val options = listOf(
        SelectOption(PerformanceProfile.BATTERY.key, stringResource(R.string.perf_battery)),
        SelectOption(PerformanceProfile.BALANCED.key, stringResource(R.string.perf_balanced)),
        SelectOption(PerformanceProfile.PERFORMANCE.key, stringResource(R.string.perf_performance)),
    )

    Scaffold(
        topBar = {
            GarnetTopAppBar(
                title = stringResource(R.string.perf_title),
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
                title = stringResource(R.string.perf_title),
                subtitle = stringResource(R.string.perf_summary),
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
                    title = stringResource(R.string.perf_profile_title),
                    selectedValue = profile.key,
                    options = options,
                    enabled = supported,
                    onValueSelected = { value ->
                        val selected = PerformanceProfile.from(value)
                        if (PerformanceManager.set(context, selected)) {
                            profile = selected
                        }
                    },
                    modifier = Modifier.padding(16.dp),
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
                    text = when {
                        !supported -> stringResource(R.string.charge_requires_66)
                        profile == PerformanceProfile.BATTERY ->
                            stringResource(R.string.perf_desc_battery)
                        profile == PerformanceProfile.PERFORMANCE ->
                            stringResource(R.string.perf_desc_performance)
                        else -> stringResource(R.string.perf_desc_balanced)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
