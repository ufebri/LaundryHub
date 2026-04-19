package com.raylabs.laundryhub.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.ui.common.dummy.history.dummyHistoryItem
import com.raylabs.laundryhub.ui.outcome.state.EntryItem
import com.raylabs.laundryhub.ui.outcome.state.TypeCard
import com.raylabs.laundryhub.ui.theme.LaundryHubTheme
import com.raylabs.laundryhub.ui.theme.appBorderSoft
import com.raylabs.laundryhub.ui.theme.appErrorContainer
import com.raylabs.laundryhub.ui.theme.appErrorContent
import com.raylabs.laundryhub.ui.theme.appInfoContainer
import com.raylabs.laundryhub.ui.theme.appInfoContent
import com.raylabs.laundryhub.ui.theme.modalSheetTop

private enum class TransactionEntryAction {
    Update,
    Delete
}

@Composable
fun TransactionEntryActionSheet(
    visible: Boolean,
    entry: EntryItem?,
    onUpdate: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible || entry == null) return

    val title = when (entry.typeCard) {
        TypeCard.INCOME -> stringResource(R.string.order_actions_title)
        TypeCard.OUTCOME -> stringResource(R.string.outcome_actions_title)
    }
    val supportingText = when (entry.typeCard) {
        TypeCard.INCOME -> stringResource(R.string.order_actions_supporting, entry.id)
        TypeCard.OUTCOME -> stringResource(R.string.outcome_actions_supporting, entry.id)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.onSurface.copy(alpha = 0.38f))
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    onDismiss()
                }
        )

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding(),
            shape = MaterialTheme.shapes.modalSheetTop,
            color = MaterialTheme.colors.surface,
            contentColor = MaterialTheme.colors.onSurface,
            elevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 40.dp, height = 4.dp)
                        .background(
                            MaterialTheme.colors.onSurface.copy(alpha = 0.18f),
                            shape = CircleShape
                        )
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.h6,
                        color = MaterialTheme.colors.onSurface
                    )
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f)
                    )
                }

                TransactionActionRow(
                    action = TransactionEntryAction.Update,
                    entryType = entry.typeCard,
                    onClick = onUpdate
                )

                TransactionActionRow(
                    action = TransactionEntryAction.Delete,
                    entryType = entry.typeCard,
                    onClick = onDelete
                )
            }
        }
    }
}

@Composable
fun TransactionDeleteConfirmationSheet(
    visible: Boolean,
    entry: EntryItem?,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible || entry == null) return

    val title = when (entry.typeCard) {
        TypeCard.INCOME -> stringResource(R.string.order_delete_confirmation_title)
        TypeCard.OUTCOME -> stringResource(R.string.outcome_delete_confirmation_title)
    }
    val message = when (entry.typeCard) {
        TypeCard.INCOME -> stringResource(R.string.order_delete_confirmation_message, entry.id)
        TypeCard.OUTCOME -> stringResource(R.string.outcome_delete_confirmation_message, entry.id)
    }

    AppConfirmationSheet(
        title = title,
        message = message,
        confirmLabel = stringResource(
            if (isDeleting) R.string.deleting else R.string.delete
        ),
        dismissLabel = stringResource(R.string.cancel),
        onConfirm = {
            if (!isDeleting) onConfirm()
        },
        onDismiss = {
            if (!isDeleting) onDismiss()
        },
        modifier = modifier,
        icon = Icons.Default.Delete,
        bulletPoints = listOf(
            stringResource(R.string.transaction_delete_point_permanent),
            stringResource(R.string.transaction_delete_point_refresh)
        )
    )
}

@Composable
private fun TransactionActionRow(
    action: TransactionEntryAction,
    entryType: TypeCard,
    onClick: () -> Unit
) {
    val isDelete = action == TransactionEntryAction.Delete
    val title = when {
        action == TransactionEntryAction.Update && entryType == TypeCard.INCOME ->
            stringResource(R.string.order_action_update_title)

        action == TransactionEntryAction.Update && entryType == TypeCard.OUTCOME ->
            stringResource(R.string.outcome_action_update_title)

        entryType == TypeCard.INCOME -> stringResource(R.string.order_action_delete_title)
        else -> stringResource(R.string.outcome_action_delete_title)
    }
    val supportingText = when {
        action == TransactionEntryAction.Update && entryType == TypeCard.INCOME ->
            stringResource(R.string.order_action_update_supporting)

        action == TransactionEntryAction.Update && entryType == TypeCard.OUTCOME ->
            stringResource(R.string.outcome_action_update_supporting)

        entryType == TypeCard.INCOME -> stringResource(R.string.order_action_delete_supporting)
        else -> stringResource(R.string.outcome_action_delete_supporting)
    }

    val leadingContainerColor = if (isDelete) {
        MaterialTheme.colors.appErrorContainer
    } else {
        MaterialTheme.colors.appInfoContainer
    }
    val leadingContentColor = if (isDelete) {
        MaterialTheme.colors.appErrorContent
    } else {
        MaterialTheme.colors.appInfoContent
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colors.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colors.appBorderSoft
        ),
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(leadingContainerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDelete) Icons.Default.Delete else Icons.Default.Edit,
                    contentDescription = null,
                    tint = leadingContentColor
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.body1,
                    color = MaterialTheme.colors.onSurface
                )
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f)
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.48f)
            )
        }
    }
}

@Preview
@Composable
private fun PreviewTransactionEntryActionSheet() {
    LaundryHubTheme {
        TransactionEntryActionSheet(
            visible = true,
            entry = dummyHistoryItem,
            onUpdate = {},
            onDelete = {},
            onDismiss = {}
        )
    }
}

@Preview
@Composable
private fun PreviewTransactionDeleteConfirmationSheet() {
    LaundryHubTheme {
        TransactionDeleteConfirmationSheet(
            visible = true,
            entry = dummyHistoryItem,
            isDeleting = false,
            onConfirm = {},
            onDismiss = {}
        )
    }
}
