package com.raylabs.laundryhub.ui.reminder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.ui.common.ApplyStatusBarStyle
import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.component.SectionOrLoading
import com.raylabs.laundryhub.ui.reminder.state.ReminderInboxUiState
import com.raylabs.laundryhub.ui.reminder.state.ReminderItemUi
import com.raylabs.laundryhub.ui.reminder.state.ReminderSectionUi

@Composable
fun ReminderInboxScreen(
    viewModel: ReminderViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onOpenOrder: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    ReminderInboxContent(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onMarkChecked = viewModel::markChecked,
        onMarkAssumedPickedUp = viewModel::markAssumedPickedUp,
        onSnooze = viewModel::snooze,
        onDismiss = viewModel::dismiss,
        onOpenOrder = onOpenOrder
    )
}

@Composable
fun ReminderInboxContent(
    state: ReminderInboxUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onMarkChecked: (String) -> Unit,
    onMarkAssumedPickedUp: (String) -> Unit,
    onSnooze: (String) -> Unit,
    onDismiss: (String) -> Unit,
    onOpenOrder: (String) -> Unit
) {
    val surfaceColor = MaterialTheme.colors.surface

    ApplyStatusBarStyle(backgroundColor = surfaceColor)

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
                    title = { Text(stringResource(R.string.reminder_inbox_title)) },
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
        if (!state.reminderSettings.isReminderEnabled) {
            ReminderDisabledState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            SectionOrLoading(
                isLoading = state.reminders.isLoading,
                error = state.reminders.errorMessage,
                hasContent = !state.reminders.data.isNullOrEmpty(),
                content = {
                    val sections = state.reminders.data.orEmpty()
                    if (sections.isEmpty()) {
                        ReminderEmptyState(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding),
                            onRefresh = onRefresh
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                top = 16.dp,
                                end = 16.dp,
                                bottom = 24.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item(key = "reminder_overview") {
                                ReminderOverviewCard(
                                    totalItems = sections.sumOf { it.items.size },
                                    isDailyNotificationEnabled = state.reminderSettings.isDailyNotificationEnabled
                                )
                            }
                            items(sections, key = { it.title }) { section ->
                                ReminderSection(
                                    section = section,
                                    onMarkChecked = onMarkChecked,
                                    onMarkAssumedPickedUp = onMarkAssumedPickedUp,
                                    onSnooze = onSnooze,
                                    onDismiss = onDismiss,
                                    onOpenOrder = onOpenOrder
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun ReminderDisabledState(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.padding(24.dp),
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.reminder_disabled_title),
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.reminder_disabled_body),
                style = MaterialTheme.typography.body2
            )
        }
    }
}

@Composable
private fun ReminderEmptyState(
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit
) {
    Card(
        modifier = modifier.padding(24.dp),
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.reminder_empty_title),
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.reminder_empty_body),
                style = MaterialTheme.typography.body2
            )
            OutlinedButton(onClick = onRefresh) {
                Text(stringResource(R.string.reminder_refresh))
            }
        }
    }
}

@Composable
private fun ReminderOverviewCard(
    totalItems: Int,
    isDailyNotificationEnabled: Boolean
) {
    Card(
        backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.10f),
        elevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.reminder_overview_title),
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.reminder_overview_body, totalItems),
                style = MaterialTheme.typography.body2
            )
            Text(
                text = stringResource(R.string.reminder_overview_hint),
                style = MaterialTheme.typography.caption
            )
            Text(
                text = if (isDailyNotificationEnabled) {
                    stringResource(R.string.reminder_overview_notifications_on)
                } else {
                    stringResource(R.string.reminder_overview_notifications_off)
                },
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.primary
            )
        }
    }
}

@Composable
private fun ReminderSection(
    section: ReminderSectionUi,
    onMarkChecked: (String) -> Unit,
    onMarkAssumedPickedUp: (String) -> Unit,
    onSnooze: (String) -> Unit,
    onDismiss: (String) -> Unit,
    onOpenOrder: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(
                R.string.reminder_section_title_with_count,
                section.title,
                section.items.size
            ),
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold
        )
        section.items.forEach { item ->
            ReminderItemCard(
                item = item,
                onMarkChecked = { onMarkChecked(item.orderId) },
                onMarkAssumedPickedUp = { onMarkAssumedPickedUp(item.orderId) },
                onSnooze = { onSnooze(item.orderId) },
                onDismiss = { onDismiss(item.orderId) },
                onOpenOrder = { onOpenOrder(item.orderId) }
            )
        }
    }
}

@Composable
private fun ReminderItemCard(
    item: ReminderItemUi,
    onMarkChecked: () -> Unit,
    onMarkAssumedPickedUp: () -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
    onOpenOrder: () -> Unit
) {
    Card(
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Order #${item.orderId}",
                style = MaterialTheme.typography.caption
            )
            Text(
                text = item.customerName,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = item.packageName,
                style = MaterialTheme.typography.body2
            )
            Text(
                text = item.paymentStatus,
                style = MaterialTheme.typography.body2
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${stringResource(R.string.order_date)}: ${item.orderDate}",
                    style = MaterialTheme.typography.caption
                )
                Text(
                    text = "${stringResource(R.string.reminder_due_date_label)}: ${item.dueDate}",
                    style = MaterialTheme.typography.caption
                )
            }
            Text(
                text = item.overdueLabel,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReminderActionChip(
                    label = stringResource(R.string.reminder_action_checked),
                    onClick = onMarkChecked
                )
                ReminderActionChip(
                    label = stringResource(R.string.reminder_action_snooze),
                    onClick = onSnooze
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReminderActionChip(
                    label = stringResource(R.string.reminder_action_assumed_picked),
                    onClick = onMarkAssumedPickedUp
                )
                ReminderActionChip(
                    label = stringResource(R.string.reminder_action_dismiss),
                    onClick = onDismiss
                )
            }

            Button(
                onClick = onOpenOrder,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.primary,
                    contentColor = MaterialTheme.colors.onPrimary
                )
            ) {
                Text(text = stringResource(R.string.reminder_action_open_order))
            }
        }
    }
}

@Composable
private fun ReminderActionChip(
    label: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(44.dp)
    ) {
        Text(text = label)
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewReminderInboxContent() {
    ReminderInboxContent(
        state = ReminderInboxUiState(
            reminders = SectionState(
                data = listOf(
                    ReminderSectionUi(
                        bucket = com.raylabs.laundryhub.core.domain.model.reminder.ReminderBucket.DUE_TODAY,
                        title = "Due today",
                        items = listOf(
                            ReminderItemUi(
                                orderId = "14",
                                customerName = "Ny Emy",
                                packageName = "Express - 6H",
                                paymentStatus = "Paid by Cash",
                                orderDate = "08/04/2026",
                                dueDate = "08/04/2026",
                                bucketLabel = "Due today",
                                overdueLabel = "Due today, needs a cross-check"
                            )
                        )
                    )
                )
            ),
            reminderSettings = com.raylabs.laundryhub.core.domain.model.reminder.ReminderSettings(
                isReminderEnabled = true
            )
        ),
        onBack = {},
        onRefresh = {},
        onMarkChecked = {},
        onMarkAssumedPickedUp = {},
        onSnooze = {},
        onDismiss = {},
        onOpenOrder = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewReminderInboxEmptyContent() {
    ReminderInboxContent(
        state = ReminderInboxUiState(
            reminderSettings = com.raylabs.laundryhub.core.domain.model.reminder.ReminderSettings(
                isReminderEnabled = true
            ),
            reminders = SectionState(data = emptyList(), isLoading = false)
        ),
        onBack = {},
        onRefresh = {},
        onMarkChecked = {},
        onMarkAssumedPickedUp = {},
        onSnooze = {},
        onDismiss = {},
        onOpenOrder = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewReminderInboxDisabledContent() {
    ReminderInboxContent(
        state = ReminderInboxUiState(
            reminderSettings = com.raylabs.laundryhub.core.domain.model.reminder.ReminderSettings(
                isReminderEnabled = false
            ),
            reminders = SectionState(data = emptyList(), isLoading = false)
        ),
        onBack = {},
        onRefresh = {},
        onMarkChecked = {},
        onMarkAssumedPickedUp = {},
        onSnooze = {},
        onDismiss = {},
        onOpenOrder = {}
    )
}
