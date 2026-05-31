package com.raylabs.laundryhub.ui.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.raylabs.laundryhub.core.domain.model.sheets.MasterSourceOfTruth
import com.raylabs.laundryhub.core.domain.model.sheets.SyncEntityPreview
import com.raylabs.laundryhub.core.domain.model.sheets.SyncPreviewResponse
import com.raylabs.laundryhub.core.domain.model.sheets.SyncRunStatusResponse
import com.raylabs.laundryhub.ui.component.AppConfirmationSheet
import com.raylabs.laundryhub.ui.component.DefaultTopAppBar
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private val ProfileCardColor = Color(0xFF43365F)
private val ProfileMutedText = Color(0xFFE0D7F5)

@Composable
fun SyncSettingsScreen(
    viewModel: SyncSettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    
    SyncSettingsScreenContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onMasterSourceSelected = viewModel::selectSourceOfTruth,
        onCheckDifferencesClick = viewModel::checkDifferences,
        onConfirmSyncNow = viewModel::confirmSyncNow,
        onDismissPreview = viewModel::dismissPreview,
        onClearMessages = viewModel::clearMessages
    )
}

@Composable
fun SyncSettingsScreenContent(
    state: SyncSettingsUiState,
    onNavigateBack: () -> Unit,
    onMasterSourceSelected: (MasterSourceOfTruth) -> Unit,
    onCheckDifferencesClick: () -> Unit,
    onConfirmSyncNow: () -> Unit,
    onDismissPreview: () -> Unit,
    onClearMessages: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    SyncSnackbarEffect(
        errorMessage = state.errorMessage,
        successMessage = state.successMessage,
        snackbarHostState = snackbarHostState,
        onClearMessages = onClearMessages
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                DefaultTopAppBar(
                    title = "Sync Master Data",
                    onBackClick = onNavigateBack
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                SyncBottomBar(
                    isChecking = state.isCheckingDifferences,
                    isSyncing = state.isSyncing,
                    onClick = onCheckDifferencesClick
                )
            }
        ) { padding ->
            SyncSettingsScrollableContent(
                state = state,
                padding = padding,
                onMasterSourceSelected = onMasterSourceSelected
            )
        }

        SyncConfirmationOverlay(
            preview = state.syncPreview,
            onConfirm = onConfirmSyncNow,
            onDismiss = onDismissPreview
        )
    }
}

@Composable
private fun SyncSettingsScrollableContent(
    state: SyncSettingsUiState,
    padding: PaddingValues,
    onMasterSourceSelected: (MasterSourceOfTruth) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        SyncStatusCard(
            lastSyncTime = state.lastSyncTime,
            changesCount = state.changesCount,
            syncStatus = state.lastSyncStatus,
            pendingPushCount = state.pendingPushCount,
            pendingDeleteCount = state.pendingDeleteCount,
            dataDifferenceCount = state.dataDifferenceCount,
            reportingDifferenceCount = state.reportingDifferenceCount,
            lastSyncError = state.lastSyncError
        )

        MasterSourceOfTruthSection(
            selectedSource = state.selectedSourceOfTruth,
            onSourceSelected = onMasterSourceSelected
        )

        state.syncPreview?.takeIf { it.totalDifferences == 0 }?.let { preview ->
            SyncPreviewCard(preview = preview)
        }

        state.activeRun?.let { run ->
            SyncRunProgressCard(run = run)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SyncSnackbarEffect(
    errorMessage: String?,
    successMessage: String?,
    snackbarHostState: SnackbarHostState,
    onClearMessages: () -> Unit
) {
    LaunchedEffect(errorMessage, successMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearMessages()
        }
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearMessages()
        }
    }
}

@Composable
private fun SyncBottomBar(
    isChecking: Boolean,
    isSyncing: Boolean,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.padding(16.dp)) {
        Button(
            onClick = onClick,
            enabled = !isChecking && !isSyncing,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = Color.White
            )
        ) {
            if (isChecking || isSyncing) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isChecking) "Checking differences..." else "Syncing...",
                    style = MaterialTheme.typography.button
                )
            } else {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Check differences", style = MaterialTheme.typography.button)
            }
        }
    }
}

@Composable
private fun SyncConfirmationOverlay(
    preview: SyncPreviewResponse?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    preview?.takeIf { it.totalDifferences > 0 }?.let { syncPreview ->
        val isReportingRefresh = syncPreview.isReportingRefreshOnly()
        AppConfirmationSheet(
            title = if (isReportingRefresh) {
                "Refresh reporting cache?"
            } else {
                "Sync ${syncPreview.totalDifferences} differences?"
            },
            message = if (isReportingRefresh) {
                "Refresh reporting cache from Sheet"
            } else {
                syncPreview.recommendedAction
            },
            confirmLabel = if (isReportingRefresh) {
                "Refresh cache"
            } else {
                "Sync now"
            },
            dismissLabel = "Not now",
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            icon = Icons.Default.Refresh,
            bulletPoints = syncPreview.actionableEntities()
                .filter { it.totalDifferences > 0 }
                .map { entity -> entity.previewBullet() }
        )
    }
}

@Composable
private fun MasterSourceOfTruthSection(selectedSource: MasterSourceOfTruth, onSourceSelected: (MasterSourceOfTruth) -> Unit) {
    Column {
        Text(
            text = "Master Data Source",
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.colors.onBackground,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Card(
            backgroundColor = ProfileCardColor,
            shape = RoundedCornerShape(12.dp),
            elevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                RadioOptionRow(
                    label = "Google Sheets",
                    selected = selectedSource == MasterSourceOfTruth.SHEETS,
                    onClick = { onSourceSelected(MasterSourceOfTruth.SHEETS) }
                )
                RadioOptionRow(
                    label = "App Database",
                    selected = selectedSource == MasterSourceOfTruth.SUPABASE,
                    onClick = { onSourceSelected(MasterSourceOfTruth.SUPABASE) }
                )
            }
        }
    }
}

@Composable
private fun SyncStatusCard(
    lastSyncTime: String?,
    changesCount: Int,
    syncStatus: String,
    pendingPushCount: Int,
    pendingDeleteCount: Int,
    dataDifferenceCount: Int,
    reportingDifferenceCount: Int,
    lastSyncError: String?
) {
    Card(
        backgroundColor = ProfileCardColor,
        shape = RoundedCornerShape(12.dp),
        elevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = ProfileMutedText, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Sync Status",
                    style = MaterialTheme.typography.subtitle1,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Last Synced", color = ProfileMutedText, style = MaterialTheme.typography.body2)
                Text(formatRelativeTime(lastSyncTime), color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.body2)
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Changes Synced", color = ProfileMutedText, style = MaterialTheme.typography.body2)
                Text("$changesCount items", color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.body2)
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Pending Sync", color = ProfileMutedText, style = MaterialTheme.typography.body2)
                Text("${pendingPushCount + pendingDeleteCount} items", color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.body2)
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("App Data Differences", color = ProfileMutedText, style = MaterialTheme.typography.body2)
                Text("$dataDifferenceCount items", color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.body2)
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Reporting Cache", color = ProfileMutedText, style = MaterialTheme.typography.body2)
                Text("$reportingDifferenceCount items", color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.body2)
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Last Outcome", color = ProfileMutedText, style = MaterialTheme.typography.body2)
                val statusColor = when (syncStatus) {
                    "SUCCESS" -> Color(0xFF4CAF50)
                    "FAILED" -> Color(0xFFF44336)
                    else -> ProfileMutedText
                }
                Text(syncStatus, color = statusColor, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.body2)
            }
            if (!lastSyncError.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(lastSyncError, color = Color(0xFFF44336), style = MaterialTheme.typography.caption)
            }
        }
    }
}

private fun SyncPreviewResponse.actionableEntities(): List<SyncEntityPreview> {
    return if (sourceOfTruth == MasterSourceOfTruth.SUPABASE) {
        entities.filterNot { it.isReportingEntity() }
    } else {
        entities
    }
}

private fun SyncPreviewResponse.isReportingRefreshOnly(): Boolean {
    return sourceOfTruth == MasterSourceOfTruth.SHEETS &&
        appOwnedDifferenceCount == 0 &&
        reportingDifferenceCount > 0
}

private fun SyncEntityPreview.isReportingEntity(): Boolean {
    return entity == "Gross" || entity == "Summary"
}

private fun SyncEntityPreview.previewBullet(): String {
    val keySummary = listOfNotNull(
        onlyInDatabaseKeys.takeIf { it.isNotEmpty() }?.let { "DB only ${it.previewKeys()}" },
        onlyInSheetKeys.takeIf { it.isNotEmpty() }?.let { "Sheet only ${it.previewKeys()}" },
        changedRowKeys.takeIf { it.isNotEmpty() }?.let { "changed ${it.previewKeys()}" }
    ).joinToString("; ")

    return if (keySummary.isBlank()) {
        "$entity: $totalDifferences differences"
    } else {
        "$entity: $totalDifferences differences ($keySummary)"
    }
}

private fun List<String>.previewKeys(): String {
    val visibleKeys = take(5).joinToString(", ")
    val remaining = size - 5
    return if (remaining > 0) {
        "$visibleKeys +$remaining"
    } else {
        visibleKeys
    }
}

@Composable
private fun RadioOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.White, style = MaterialTheme.typography.body1)
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colors.secondary,
                unselectedColor = ProfileMutedText
            )
        )
    }
}

@Composable
private fun SyncPreviewCard(preview: SyncPreviewResponse) {
    Card(
        backgroundColor = ProfileCardColor,
        shape = RoundedCornerShape(12.dp),
        elevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Latest check", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.subtitle1)
            Text("App-owned data is in sync", color = ProfileMutedText, style = MaterialTheme.typography.body2)
            Text("Checked ${formatRelativeTime(preview.generatedAt)}", color = ProfileMutedText, style = MaterialTheme.typography.caption)
        }
    }
}

@Composable
private fun SyncRunProgressCard(run: SyncRunStatusResponse) {
    val progress = if (run.totalItems <= 0) {
        0f
    } else {
        run.processedItems.toFloat() / run.totalItems.toFloat()
    }.coerceIn(0f, 1f)

    Card(
        backgroundColor = ProfileCardColor,
        shape = RoundedCornerShape(12.dp),
        elevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Sync progress", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.subtitle1)
            Text(run.message, color = ProfileMutedText, style = MaterialTheme.typography.body2)
            LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
            Text(
                text = "${run.processedItems} of ${run.totalItems} changes",
                color = ProfileMutedText,
                style = MaterialTheme.typography.caption
            )
            run.finalDifferenceCount?.let { count ->
                Text(
                    text = "$count differences remaining",
                    color = if (count == 0) Color(0xFF4CAF50) else Color(0xFFFFC107),
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun formatRelativeTime(isoString: String?): String {
    if (isoString.isNullOrBlank()) return "Never"
    return try {
        val parsed = LocalDateTime.parse(isoString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val now = LocalDateTime.now()
        val diffMinutes = ChronoUnit.MINUTES.between(parsed, now)
        when {
            diffMinutes < 1 -> "Just now"
            diffMinutes < 60 -> "$diffMinutes mins ago"
            diffMinutes < 1440 -> "${diffMinutes / 60} hours ago"
            else -> parsed.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.forLanguageTag("en")))
        }
    } catch (e: Exception) {
        "Unknown"
    }
}
