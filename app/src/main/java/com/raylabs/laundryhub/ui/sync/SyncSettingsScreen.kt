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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.raylabs.laundryhub.core.domain.model.sheets.ReverseSyncSchedule
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
        onIntervalSelected = viewModel::updateAutoSyncInterval,
        onScheduleSelected = viewModel::updateReverseSyncSchedule,
        onSyncNowClick = viewModel::triggerManualSync,
        onClearMessages = viewModel::clearMessages
    )
}

@Composable
fun SyncSettingsScreenContent(
    state: SyncSettingsUiState,
    onNavigateBack: () -> Unit,
    onIntervalSelected: (Int) -> Unit,
    onScheduleSelected: (ReverseSyncSchedule) -> Unit,
    onSyncNowClick: () -> Unit,
    onClearMessages: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LocalContext.current

    LaunchedEffect(state.errorMessage, state.successMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearMessages()
        }
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearMessages()
        }
    }

    Scaffold(
        topBar = {
            DefaultTopAppBar(
                title = "Sync Master Data",
                onBackClick = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Box(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = onSyncNowClick,
                    enabled = !state.isSyncing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.primary,
                        contentColor = Color.White
                    )
                ) {
                    if (state.isSyncing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Sinkronisasi Sekarang", style = MaterialTheme.typography.button)
                    }
                }
            }
        }
    ) { padding ->
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
                changesCount = state.changesCount
            )

            SyncIntervalSection(
                selectedMinutes = state.autoSyncIntervalMinutes,
                onIntervalSelected = onIntervalSelected
            )

            ReverseSyncScheduleSection(
                selectedSchedule = state.reverseSyncSchedule,
                onScheduleSelected = onScheduleSelected
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SyncStatusCard(lastSyncTime: String?, changesCount: Int) {
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
                    text = "Status Sinkronisasi",
                    style = MaterialTheme.typography.subtitle1,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Terakhir Sinkronisasi", color = ProfileMutedText, style = MaterialTheme.typography.body2)
                Text(formatRelativeTime(lastSyncTime), color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.body2)
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Perubahan Terkirim", color = ProfileMutedText, style = MaterialTheme.typography.body2)
                Text("$changesCount item", color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.body2)
            }
        }
    }
}

@Composable
private fun SyncIntervalSection(selectedMinutes: Int, onIntervalSelected: (Int) -> Unit) {
    Column {
        Text(
            text = "Interval Sinkronisasi Otomatis",
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
                val options = listOf(15, 30, 60)
                options.forEach { minutes ->
                    val label = if (minutes == 60) "1 Jam" else "$minutes Menit"
                    RadioOptionRow(
                        label = label,
                        selected = selectedMinutes == minutes,
                        onClick = { onIntervalSelected(minutes) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReverseSyncScheduleSection(selectedSchedule: ReverseSyncSchedule, onScheduleSelected: (ReverseSyncSchedule) -> Unit) {
    Column {
        Text(
            text = "Jadwal Tarik Data (Sheets -> App)",
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
                    label = "23:00 WIB",
                    selected = selectedSchedule == ReverseSyncSchedule.DEFAULT_23,
                    onClick = { onScheduleSelected(ReverseSyncSchedule.DEFAULT_23) }
                )
                RadioOptionRow(
                    label = "12:00 & 23:00 WIB",
                    selected = selectedSchedule == ReverseSyncSchedule.TWICE_DAILY,
                    onClick = { onScheduleSelected(ReverseSyncSchedule.TWICE_DAILY) }
                )
                RadioOptionRow(
                    label = "Hanya Manual",
                    selected = selectedSchedule == ReverseSyncSchedule.MANUAL,
                    onClick = { onScheduleSelected(ReverseSyncSchedule.MANUAL) }
                )
            }
        }
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

private fun formatRelativeTime(isoString: String?): String {
    if (isoString.isNullOrBlank()) return "Belum pernah"
    return try {
        val parsed = LocalDateTime.parse(isoString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val now = LocalDateTime.now()
        val diffMinutes = ChronoUnit.MINUTES.between(parsed, now)
        when {
            diffMinutes < 1 -> "Baru saja"
            diffMinutes < 60 -> "$diffMinutes menit yang lalu"
            diffMinutes < 1440 -> "${diffMinutes / 60} jam yang lalu"
            else -> parsed.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale("id", "ID")))
        }
    } catch (e: Exception) {
        "Tidak diketahui"
    }
}
