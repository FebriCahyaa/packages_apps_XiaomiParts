/*
 * Copyright (C) 2026 The LineageOS Project
 *           (C) 2026 zylhdrXP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.garnetparts

import android.content.Context
import android.content.Intent
import android.os.SystemProperties
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import kotlinx.coroutines.launch
import org.lineageos.settings.R
import org.lineageos.settings.charge.ChargeActivity
import org.lineageos.settings.chargeboost.ChargeBoostActivity
import org.lineageos.settings.corecontrol.CoreControlActivity
import org.lineageos.settings.gpumanager.GpuManagerActivity
import org.lineageos.settings.kernelmanager.KernelManagerActivity
import org.lineageos.settings.refreshrate.RefreshSettingsActivity
import org.lineageos.settings.saturation.SaturationActivity
import org.lineageos.settings.speaker.ClearSpeakerActivity
import org.lineageos.settings.thermal.ThermalComposeActivity
import org.lineageos.settings.ui.components.GarnetExpressiveShapes
import org.lineageos.settings.ui.components.GarnetPageHeading
import org.lineageos.settings.ui.components.GarnetTopAppBar
import org.lineageos.settings.ui.components.ModernSettingActionRow
import org.lineageos.settings.ui.components.MorphShape
import org.lineageos.settings.ui.components.connectedGridShape
import org.lineageos.settings.ui.components.connectedItemShape
import org.lineageos.settings.ui.components.squishable
import org.lineageos.settings.ui.theme.ExpressiveTheme
import org.lineageos.settings.utils.rememberHapticFeedback

data class GarnetFeature(
    val title: String,
    val summary: String,
    val iconRes: Int,
    val activityClass: Class<*>
)

private fun getSupportedDeviceName(): String? =
    SystemProperties.get("bluetooth.device.default_name").takeIf { it.isNotBlank() }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GarnetDashboard(onBackPressed: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val gpuManager = GarnetFeature(
        stringResource(R.string.gpu_manager_title),
        stringResource(R.string.garnet_gpu_manager_summary),
        R.drawable.ic_gpu_manager,
        GpuManagerActivity::class.java
    )
    val thermalEngine = GarnetFeature(
        stringResource(R.string.thermal_title),
        stringResource(R.string.garnet_thermal_engine_summary),
        R.drawable.ic_thermal_settings,
        ThermalComposeActivity::class.java
    )
    val coreControl = GarnetFeature(
        stringResource(R.string.core_control_title),
        stringResource(R.string.garnet_core_control_summary),
        R.drawable.ic_cpu,
        CoreControlActivity::class.java
    )
    val kernelManager = GarnetFeature(
        stringResource(R.string.kernel_manager_title),
        stringResource(R.string.garnet_kernel_manager_summary),
        R.drawable.ic_kernel_manager,
        KernelManagerActivity::class.java
    )

    val utilities = remember {
        listOf(
            GarnetFeature(
                context.getString(R.string.garnet_display_labs_title),
                context.getString(R.string.garnet_display_labs_summary),
                R.drawable.ic_saturation_tile,
                SaturationActivity::class.java
            ),
            GarnetFeature(
                context.getString(R.string.clear_speaker_title),
                context.getString(R.string.garnet_clear_speaker_summary),
                R.drawable.ic_clear_speaker,
                ClearSpeakerActivity::class.java
            ),
            GarnetFeature(
                context.getString(R.string.refresh_title),
                context.getString(R.string.garnet_smooth_display_summary),
                R.drawable.ic_refresh_default,
                RefreshSettingsActivity::class.java
            ),
            GarnetFeature(
                context.getString(R.string.charge_bypass_title),
                context.getString(R.string.garnet_bypass_charge_summary),
                R.drawable.ic_charge,
                ChargeActivity::class.java
            ),
            GarnetFeature(
                context.getString(R.string.charge_boost_title),
                context.getString(R.string.charge_boost_summary),
                R.drawable.ic_charge_boost,
                ChargeBoostActivity::class.java
            ),
        )
    }

    ExpressiveTheme {
        Scaffold(
            topBar = {
                GarnetTopAppBar(
                    title = stringResource(R.string.garnet_parts_title),
                    showTitle = scrollState.value > 120,
                    onBack = onBackPressed,
                )
            },
            containerColor = MaterialTheme.colorScheme.surface,
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                GarnetPageHeading(
                    title = stringResource(R.string.garnet_parts_title),
                    subtitle = stringResource(R.string.garnet_parts_summary),
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )

                GarnetHeroBanner()

                Text(
                    text = stringResource(R.string.garnet_hardware_engines),
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        HardwareEngineGridCard(
                            feature = gpuManager,
                            polygon = GarnetExpressiveShapes.GpuPolygon,
                            shape = connectedGridShape(0),
                            modifier = Modifier.weight(1f),
                        )
                        HardwareEngineGridCard(
                            feature = thermalEngine,
                            polygon = GarnetExpressiveShapes.ThermalPolygon,
                            shape = connectedGridShape(1),
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        HardwareEngineGridCard(
                            feature = coreControl,
                            polygon = GarnetExpressiveShapes.CoreControlPolygon,
                            shape = connectedGridShape(2),
                            modifier = Modifier.weight(1f),
                        )
                        HardwareEngineGridCard(
                            feature = kernelManager,
                            polygon = GarnetExpressiveShapes.KernelPolygon,
                            shape = connectedGridShape(3),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.garnet_system_utilities),
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    utilities.forEachIndexed { index, util ->
                        ModernSettingActionRow(
                            title = util.title,
                            summary = util.summary,
                            iconPainter = painterResource(id = util.iconRes),
                            shape = connectedItemShape(index, utilities.size),
                            onClick = {
                                context.startActivity(Intent(context, util.activityClass))
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GarnetHeroBanner(modifier: Modifier = Modifier) {
    val supportedDeviceName = remember { getSupportedDeviceName() }
    val infiniteTransition = rememberInfiniteTransition(label = "hero_banner")
    val emblemRotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "hero_rotation",
    )

    val heroMorph = remember {
        Morph(MaterialShapes.Circle, MaterialShapes.Sunny)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .squishable(enabled = true, scaleDown = 0.985f),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiary),
                        )
                        Text(
                            text = stringResource(R.string.garnet_hero_prompt),
                            style = MaterialTheme.typography.labelSmallEmphasized.copy(
                                fontFamily = FontFamily.Monospace,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(R.string.garnet_hero_terminal),
                            style = MaterialTheme.typography.labelSmallEmphasized.copy(
                                fontFamily = FontFamily.Monospace,
                            ),
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }

                Text(
                    text = supportedDeviceName ?: stringResource(R.string.garnet_supported_devices),
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = stringResource(R.string.garnet_system_performance_optimized),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Box(
                modifier = Modifier
                    .size(76.dp)
                    .graphicsLayer { rotationZ = emblemRotation }
                    .clip(MorphShape(heroMorph, 1f))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_garnet),
                    contentDescription = stringResource(R.string.garnet_engine_content_description),
                    modifier = Modifier.size(44.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HardwareEngineGridCard(
    feature: GarnetFeature,
    polygon: RoundedPolygon,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()
    val badgeMorph = remember(polygon) {
        Morph(MaterialShapes.Circle, polygon)
    }

    Card(
        modifier = modifier
            .squishable(enabled = true, scaleDown = 0.95f),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = {
            scope.launch { haptic.click() }
            context.startActivity(Intent(context, feature.activityClass))
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(MorphShape(badgeMorph, 1f))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = feature.iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }

            Text(
                text = feature.title,
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
