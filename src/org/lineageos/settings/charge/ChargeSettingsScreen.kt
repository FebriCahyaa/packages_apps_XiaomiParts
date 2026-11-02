/*
 * Copyright (C) 2025 The LineageOS Project
 *           (C) 2026 zylhdrXP
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.lineageos.settings.R
import org.lineageos.settings.ui.components.GarnetPageHeading
import org.lineageos.settings.ui.components.GarnetTopAppBar
import org.lineageos.settings.ui.components.ModernConfirmDialog
import org.lineageos.settings.ui.components.ModernSettingSwitch
import org.lineageos.settings.ui.components.squishable

@Composable
fun ChargeSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val chargeUtils = remember { ChargeUtils(context) }
    val scrollState = rememberScrollState()

    var isBypassEnabled by remember { mutableStateOf(chargeUtils.isBypassChargeEnabled()) }
    val isSupported = remember { chargeUtils.isBypassChargeSupported() }
    var showWarningDialog by remember { mutableStateOf(false) }

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
                    isBypassing = isBypassEnabled,
                )
            }

            ModernSettingSwitch(
                title = stringResource(R.string.charge_bypass_title),
                subtitle = if (isSupported) {
                    stringResource(R.string.charge_bypass_summary)
                } else {
                    stringResource(R.string.charge_bypass_unavailable)
                },
                checked = isBypassEnabled,
                onCheckedChange = { checked ->
                    if (checked) {
                        showWarningDialog = true
                    } else {
                        isBypassEnabled = false
                        chargeUtils.enableBypassCharge(false)
                    }
                },
                shape = MaterialTheme.shapes.extraLarge,
            )

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
                isBypassEnabled = true
                chargeUtils.enableBypassCharge(true)
                showWarningDialog = false
            },
            onDismiss = {
                showWarningDialog = false
            },
        )
    }
}
