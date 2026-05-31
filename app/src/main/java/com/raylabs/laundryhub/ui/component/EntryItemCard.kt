package com.raylabs.laundryhub.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.ui.common.dummy.history.dummyHistoryItem
import com.raylabs.laundryhub.ui.home.state.SyncStatus
import com.raylabs.laundryhub.ui.outcome.state.EntryItem
import com.raylabs.laundryhub.ui.outcome.state.TypeCard
import com.raylabs.laundryhub.ui.theme.Purple300

@Composable
fun EntryItemCard(
    item: EntryItem,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null
) {
    val isPending = item.syncStatus == SyncStatus.PENDING
    val isFailed = item.syncStatus == SyncStatus.FAILED

    val bgColor = if (isFailed) Color(0xFF5E2E3A) else Purple300
    val border = if (isFailed) BorderStroke(1.dp, Color.Red) else null
    val clickModifier = if (!isPending && !isFailed && onClick != null) {
        Modifier.clickable { onClick() }
    } else {
        Modifier
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        backgroundColor = bgColor,
        border = border,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .then(clickModifier)
            .let {
                if (isPending) it.alpha(0.6f) else it
            }
    ) {
        val formattedId = when (item.typeCard) {
            TypeCard.INCOME -> stringResource(id = R.string.order_id, item.id)
            TypeCard.OUTCOME -> {
                if (isPending || isFailed) {
                    stringResource(id = R.string.outcome)
                } else {
                    stringResource(id = R.string.outcome_id, item.id)
                }
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            // Baris atas: Order/Outcome ID & status/spinner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedId,
                    style = MaterialTheme.typography.caption,
                    color = Color.White
                )

                if (isPending) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(14.dp)
                    )
                } else if (isFailed) {
                    Text(
                        text = "Sync Failed",
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE57373)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Baris kedua: Name & Payment Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = item.paymentStatus,
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Baris ketiga: Package/Remark & Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.remark,
                    style = MaterialTheme.typography.body2,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = item.price,
                    style = MaterialTheme.typography.body2,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            // Baris keempat: Retry & Cancel Actions for Failed states
            if (isFailed) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onCancel?.invoke() },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White,
                            backgroundColor = Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cancel",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Cancel", style = MaterialTheme.typography.caption)
                    }

                    Button(
                        onClick = { onRetry?.invoke() },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFE57373),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Retry",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Retry", style = MaterialTheme.typography.caption)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewEntryItemCard() {
    EntryItemCard(
        item = dummyHistoryItem
    )
}
