package com.raylabs.laundryhub.ui.common.util

import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Shows a snackbar that dismisses faster than the default [SnackbarDuration.Short].
 * Default [SnackbarDuration.Short] is usually 4 seconds.
 * This function defaults to 1.5 seconds.
 */
suspend fun SnackbarHostState.showQuickSnackbar(
    message: String,
    actionLabel: String? = null,
    durationMs: Long = 2000L
) {
    withTimeoutOrNull(durationMs) {
        showSnackbar(
            message = message,
            actionLabel = actionLabel,
            duration = SnackbarDuration.Short
        )
    }
}
