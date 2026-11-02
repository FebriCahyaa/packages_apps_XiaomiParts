/*
 * Copyright (C) 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class MorphShape(
    private val morph: Morph,
    private val progress: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = morph.toPath(progress = progress, path = Path())
        val matrix = Matrix().apply {
            scale(size.width, size.height)
        }
        path.transform(matrix)
        val center = Offset(size.width / 2f, size.height / 2f)
        val pathCenter = path.getBounds().center
        path.translate(center - pathCenter)
        return Outline.Generic(path)
    }
}

fun connectedItemShape(index: Int, count: Int): RoundedCornerShape {
    if (count <= 1) return RoundedCornerShape(20.dp)
    return when (index) {
        0 -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        count - 1 -> RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp, topStart = 4.dp, topEnd = 4.dp)
        else -> RoundedCornerShape(4.dp)
    }
}

fun connectedGridShape(index: Int): RoundedCornerShape {
    return when (index) {
        0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 4.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        2 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 4.dp)
        3 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 4.dp, bottomEnd = 24.dp)
        else -> RoundedCornerShape(16.dp)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Modifier.squishable(
    enabled: Boolean = true,
    scaleDown: Float = 0.93f
): Modifier {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) scaleDown else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "squish_scale"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(enabled) {
            if (enabled) {
                try {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        isPressed = true
                        try {
                            waitForUpOrCancellation()
                        } finally {
                            isPressed = false
                        }
                    }
                } finally {
                    isPressed = false
                }
            }
        }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun tileSelectionBorder(selected: Boolean): BorderStroke {
    val width by animateDpAsState(
        targetValue = if (selected) 1.2.dp else 0.8.dp,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "tile_border_width"
    )
    val color by animateColorAsState(
        targetValue = (if (selected) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.outlineVariant).copy(alpha = 0.8f),
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "tile_border_color"
    )
    return BorderStroke(width, color)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object GarnetExpressiveShapes {
    val GpuPolygon: RoundedPolygon = MaterialShapes.Cookie9Sided
    val ThermalPolygon: RoundedPolygon = MaterialShapes.SoftBurst
    val CoreControlPolygon: RoundedPolygon = MaterialShapes.Sunny
    val KernelPolygon: RoundedPolygon = MaterialShapes.Clover4Leaf
    val DisplayPolygon: RoundedPolygon = MaterialShapes.Flower
    val SpeakerPolygon: RoundedPolygon = MaterialShapes.Boom
    val RefreshPolygon: RoundedPolygon = MaterialShapes.Pill
    val ChargePolygon: RoundedPolygon = MaterialShapes.PuffyDiamond
}
