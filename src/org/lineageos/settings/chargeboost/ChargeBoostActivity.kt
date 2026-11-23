/*
 * SPDX-FileCopyrightText: The Evolution X Project
 *                         2026 zylhdrXP
 *                         2026 FEBRIAN RAHMAD CAHYA
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.chargeboost

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import org.lineageos.settings.ui.components.ModernSettingSwitch
import org.lineageos.settings.ui.components.connectedItemShape
import org.lineageos.settings.ui.components.squishable
import org.lineageos.settings.ui.theme.ExpressiveShapes
import org.lineageos.settings.ui.theme.ExpressiveTheme

class ChargeBoostActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpressiveTheme {
                ChargeBoostScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
private fun boostHeadline(boostRequested: Boolean, active: Boolean, hot: Boolean): String {
    if (!boostRequested) return stringResource(R.string.charge_boost_off)
    if (hot) return stringResource(R.string.charge_boost_paused)
    if (!active) return stringResource(R.string.charge_boost_waiting)
    return stringResource(R.string.charge_boost_on)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargeBoostScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val status by ChargingRepository.status.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        while (true) {
            ChargingRepository.refresh(context)
            delay(1000)
        }
    }

    val supported = status.kernel66 && ChargingRepository.isBoostSupported()
    val thermalBlocked = status.hot && status.tempGuard
    val active = status.boostActive && !status.bypass && !thermalBlocked
    val headline by animateColorAsState(
        targetValue = if (active) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHighest,
        label = "boost_headline",
    )

    Scaffold(
        topBar = {
            GarnetTopAppBar(
                title = stringResource(R.string.charge_boost_title),
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
                title = stringResource(R.string.charge_boost_title),
                subtitle = stringResource(R.string.charge_boost_summary),
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(color = headline, shape = ExpressiveShapes.ShapeMedium),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = boostHeadline(status.boostRequested, active, thermalBlocked),
                            style = MaterialTheme.typography.titleMediumEmphasized,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when {
                                !status.kernel66 -> stringResource(R.string.charge_requires_66)
                                !supported -> stringResource(R.string.charge_boost_unsupported)
                                status.bypass -> stringResource(R.string.charge_bypass_boost_suspended)
                                else -> stringResource(R.string.charge_boost_guard_hint)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                ModernSettingSwitch(
                    title = stringResource(R.string.charge_boost_title),
                    subtitle = when {
                        !status.kernel66 -> stringResource(R.string.charge_requires_66)
                        !supported -> stringResource(R.string.charge_boost_unsupported)
                        status.bypass -> stringResource(R.string.charge_bypass_boost_suspended)
                        else -> stringResource(R.string.charge_boost_switch_summary)
                    },
                    checked = supported && status.boostRequested && !status.bypass,
                    onCheckedChange = { checked ->
                        if (supported) ChargingRepository.setBoost(context, checked)
                    },
                    shape = connectedItemShape(0, 3),
                )

                ModernSettingSwitch(
                    title = stringResource(R.string.charge_temp_guard_title),
                    subtitle = stringResource(R.string.charge_temp_guard_summary),
                    checked = status.tempGuard,
                    onCheckedChange = { checked ->
                        if (supported) ChargingRepository.setTempGuard(context, checked)
                    },
                    shape = connectedItemShape(1, 3),
                )

                ModernSettingSwitch(
                    title = stringResource(R.string.charge_boost_force_auth_title),
                    subtitle = stringResource(R.string.charge_boost_force_auth_summary),
                    checked = status.boostRequested && status.forceAuth,
                    onCheckedChange = { checked ->
                        if (supported && status.boostRequested) {
                            ChargingRepository.setForceAuth(context, checked)
                        }
                    },
                    shape = connectedItemShape(2, 3),
                )
            }

            ChargingStatusCard(status = status)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.charge_boost_footer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
