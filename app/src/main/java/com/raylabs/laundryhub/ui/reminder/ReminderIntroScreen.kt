package com.raylabs.laundryhub.ui.reminder

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.core.domain.model.reminder.ReminderSettings
import com.raylabs.laundryhub.core.reminder.ReminderNotificationPublisher
import com.raylabs.laundryhub.core.reminder.hasReminderNotificationPermission
import com.raylabs.laundryhub.ui.common.ApplyStatusBarStyle
import com.raylabs.laundryhub.ui.reminder.state.ReminderIntroUiState
import java.util.Calendar

private enum class ReminderPermissionRequest {
    NONE,
    ENABLE_DAILY,
    SEND_TEST
}

@Composable
fun ReminderIntroScreen(
    viewModel: ReminderIntroViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onOpenInbox: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var pendingPermissionRequest by rememberSaveable {
        mutableStateOf(ReminderPermissionRequest.NONE)
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        when {
            granted && pendingPermissionRequest == ReminderPermissionRequest.ENABLE_DAILY -> {
                viewModel.setDailyNotificationsEnabled(true)
            }

            granted && pendingPermissionRequest == ReminderPermissionRequest.SEND_TEST -> {
                ReminderNotificationPublisher.showTestNotification(context)
            }
        }
        pendingPermissionRequest = ReminderPermissionRequest.NONE
    }

    ReminderIntroContent(
        state = state,
        onBack = onBack,
        onReminderEnabledChanged = viewModel::setReminderEnabled,
        onDailyNotificationsChanged = { enabled ->
            if (!enabled) {
                viewModel.setDailyNotificationsEnabled(false)
            } else if (
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                context.hasReminderNotificationPermission()
            ) {
                viewModel.setDailyNotificationsEnabled(true)
            } else {
                pendingPermissionRequest = ReminderPermissionRequest.ENABLE_DAILY
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onNotificationTimeChanged = viewModel::setDailyNotificationTime,
        onOpenInbox = onOpenInbox,
        onSendTestNotification = {
            if (
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                context.hasReminderNotificationPermission()
            ) {
                ReminderNotificationPublisher.showTestNotification(context)
            } else {
                pendingPermissionRequest = ReminderPermissionRequest.SEND_TEST
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    )
}

@Composable
fun ReminderIntroContent(
    state: ReminderIntroUiState,
    onBack: () -> Unit,
    onReminderEnabledChanged: (Boolean) -> Unit,
    onDailyNotificationsChanged: (Boolean) -> Unit,
    onNotificationTimeChanged: (Int, Int) -> Unit,
    onOpenInbox: () -> Unit,
    onSendTestNotification: () -> Unit
) {
    val context = LocalContext.current
    val surfaceColor = MaterialTheme.colors.surface
    val timeLabel = rememberReminderTimeLabel(
        hourOfDay = state.reminderSettings.notificationHour,
        minute = state.reminderSettings.notificationMinute
    )
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    ApplyStatusBarStyle(backgroundColor = surfaceColor)

    if (showTimePicker) {
        DisposableEffect(
            state.reminderSettings.notificationHour,
            state.reminderSettings.notificationMinute
        ) {
            val dialog = TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    onNotificationTimeChanged(hourOfDay, minute)
                    showTimePicker = false
                },
                state.reminderSettings.notificationHour,
                state.reminderSettings.notificationMinute,
                DateFormat.is24HourFormat(context)
            )
            dialog.setOnDismissListener { showTimePicker = false }
            dialog.show()
            onDispose { dialog.dismiss() }
        }
    }

    Scaffold(
        topBar = {
            Column {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surfaceColor)
                        .statusBarsPadding()
                )
                TopAppBar(
                    title = { Text(stringResource(R.string.reminder_intro_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    backgroundColor = surfaceColor,
                    elevation = 0.dp
                )
            }
        }
    ) { padding ->
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item("reminder_settings_header") {
                ReminderSettingsHeader()
            }

            item("reminder_settings_card") {
                Card(
                    backgroundColor = MaterialTheme.colors.surface,
                    elevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        ReminderSwitchRow(
                            icon = Icons.Default.Build,
                            title = stringResource(R.string.reminder_enabled_title),
                            description = stringResource(R.string.reminder_enabled_description),
                            checked = state.reminderSettings.isReminderEnabled,
                            onCheckedChange = onReminderEnabledChanged
                        )
                        Divider()
                        ReminderSwitchRow(
                            icon = Icons.Default.Notifications,
                            title = stringResource(R.string.reminder_daily_notifications_title),
                            description = stringResource(R.string.reminder_daily_notifications_description),
                            checked = state.reminderSettings.isDailyNotificationEnabled,
                            onCheckedChange = onDailyNotificationsChanged
                        )
                        Divider()
                        ReminderActionRow(
                            icon = Icons.Default.DateRange,
                            title = stringResource(R.string.reminder_notification_time_title),
                            description = if (state.reminderSettings.isDailyNotificationEnabled) {
                                stringResource(
                                    R.string.reminder_notification_time_description,
                                    timeLabel
                                )
                            } else {
                                stringResource(
                                    R.string.reminder_notification_time_inactive_description,
                                    timeLabel
                                )
                            },
                            trailingText = timeLabel,
                            onClick = { showTimePicker = true }
                        )
                        Divider()
                        ReminderActionRow(
                            icon = Icons.Default.Notifications,
                            title = stringResource(R.string.reminder_send_test_notification_title),
                            description = stringResource(
                                R.string.reminder_send_test_notification_description
                            ),
                            onClick = onSendTestNotification
                        )
                        Divider()
                        ReminderActionRow(
                            icon = Icons.Default.Build,
                            title = stringResource(R.string.reminder_open_inbox_title),
                            description = if (state.reminderSettings.isReminderEnabled) {
                                stringResource(R.string.reminder_open_inbox_description)
                            } else {
                                stringResource(R.string.reminder_open_inbox_disabled_description)
                            },
                            onClick = onOpenInbox
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderSettingsHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.reminder_settings_heading),
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.onBackground
        )
        Text(
            text = stringResource(R.string.reminder_settings_body),
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onBackground.copy(alpha = 0.74f)
        )
    }
}

@Composable
private fun ReminderSwitchRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ReminderRowIcon(icon = icon)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.body1,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.70f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colors.primary,
                checkedTrackColor = MaterialTheme.colors.primary.copy(alpha = 0.40f),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colors.onSurface.copy(alpha = 0.22f)
            )
        )
    }
}

@Composable
private fun ReminderActionRow(
    icon: ImageVector,
    title: String,
    description: String,
    trailingText: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ReminderRowIcon(icon = icon)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.body1,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.70f)
            )
        }
        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary
            )
        }
    }
}

@Composable
private fun ReminderRowIcon(icon: ImageVector) {
    androidx.compose.material.Surface(
        color = MaterialTheme.colors.primary.copy(alpha = 0.10f),
        shape = MaterialTheme.shapes.medium
    ) {
        Box(
            modifier = Modifier.padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colors.primary
            )
        }
    }
}

@Composable
private fun rememberReminderTimeLabel(
    hourOfDay: Int,
    minute: Int
): String {
    val context = LocalContext.current
    return remember(hourOfDay, minute, context) {
        formatReminderTime(
            context = context,
            hourOfDay = hourOfDay,
            minute = minute
        )
    }
}

fun formatReminderTime(
    context: Context,
    hourOfDay: Int,
    minute: Int
): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hourOfDay.coerceIn(0, 23))
        set(Calendar.MINUTE, minute.coerceIn(0, 59))
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return DateFormat.getTimeFormat(context).format(calendar.time)
}

@Preview(showBackground = true)
@Composable
private fun PreviewReminderIntroContent() {
    ReminderIntroContent(
        state = ReminderIntroUiState(ReminderSettings()),
        onBack = {},
        onReminderEnabledChanged = {},
        onDailyNotificationsChanged = {},
        onNotificationTimeChanged = { _, _ -> },
        onOpenInbox = {},
        onSendTestNotification = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewReminderIntroEnabledContent() {
    ReminderIntroContent(
        state = ReminderIntroUiState(
            ReminderSettings(
                isReminderEnabled = true,
                isDailyNotificationEnabled = true,
                notificationHour = 10,
                notificationMinute = 30
            )
        ),
        onBack = {},
        onReminderEnabledChanged = {},
        onDailyNotificationsChanged = {},
        onNotificationTimeChanged = { _, _ -> },
        onOpenInbox = {},
        onSendTestNotification = {}
    )
}
