/*
 * Copyright (C) 2025 The LineageOS Project
 *           (C) 2026 zylhdrXP
 *           (C) 2026 FEBRIAN RAHMAD CAHYA
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package org.lineageos.settings.charge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.lineageos.settings.R
import org.lineageos.settings.charging.ChargingRepository
import org.lineageos.settings.charging.ChargingStatusCard
import org.lineageos.settings.ui.components.GarnetPageHeading
import org.lineageos.settings.ui.components.GarnetTopAppBar
import org.lineageos.settings.ui.components.ModernConfirmDialog
import org.lineageos.settings.ui.components.ModernSettingSwitch
import org.lineageos.settings.ui.components.connectedItemShape
import org.lineageos.settings.ui.components.squishable

@Composable
fun ChargeSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val status by ChargingRepository.status.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    var showWarningDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            ChargingRepository.refresh(context)
            delay(1000)
        }
    }

    val canToggle = status.kernel66 && status.supported

    Scaffold(
        topBar = {
            GarnetTopAppBar(
                title = stringResource(R.string.charge_bypass_title),
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
                title = stringResource(R.string.charge_bypass_title),
                subtitle = stringResource(R.string.charge_bypass_summary),
                modifier = Modifier.padding(top = 8.dp),
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .squishable(enabled = true, scaleDown = 0.985f),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                BypassChargingAnimation(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    isBypassing = status.bypass,
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                ModernSettingSwitch(
                    title = stringResource(R.string.charge_bypass_title),
                    subtitle = when {
                        !status.kernel66 -> stringResource(R.string.charge_requires_66)
                        !status.supported -> stringResource(R.string.charge_bypass_unavailable)
                        else -> stringResource(R.string.charge_bypass_summary)
                    },
                    checked = status.bypass,
                    onCheckedChange = { checked ->
                        if (!canToggle) return@ModernSettingSwitch
                        if (checked) {
                            showWarningDialog = true
                        } else {
                            ChargingRepository.setBypass(context, false)
                        }
                    },
                    shape = connectedItemShape(0, 2),
                )

                ModernSettingSwitch(
                    title = stringResource(R.string.charge_restore_title),
                    subtitle = stringResource(R.string.charge_restore_summary),
                    checked = status.restoreOnBoot,
                    onCheckedChange = { checked ->
                        if (status.kernel66) {
                            ChargingRepository.setRestoreOnBoot(context, checked)
                        }
                    },
                    shape = connectedItemShape(1, 2),
                )
            }

            if (status.bypass && status.boostRequested) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.charge_bypass_boost_suspended),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            ChargingStatusCard(status = status)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.charge_bypass_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showWarningDialog) {
        ModernConfirmDialog(
            title = stringResource(R.string.charge_bypass_title),
            message = stringResource(R.string.charge_bypass_warning),
            icon = Icons.Rounded.Warning,
            onConfirm = {
                ChargingRepository.setBypass(context, true)
                showWarningDialog = false
            },
            onDismiss = {
                showWarningDialog = false
            },
        )
    }
}
