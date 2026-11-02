/*
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 *                         2026 zylhdrXP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.thermal.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.lineageos.settings.R
import org.lineageos.settings.ui.components.ModernConfirmDialog

@Composable
fun ResetConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ModernConfirmDialog(
        title = stringResource(R.string.thermal_reset),
        message = stringResource(R.string.thermal_reset_message),
        icon = Icons.Rounded.Restore,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}
