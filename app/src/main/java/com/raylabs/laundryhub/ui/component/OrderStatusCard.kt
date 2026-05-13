package com.raylabs.laundryhub.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.ui.common.dummy.home.DUMMY_UNPAID_ORDER_ITEM_EMY
import com.raylabs.laundryhub.ui.home.state.SyncStatus
import com.raylabs.laundryhub.ui.home.state.UnpaidOrderItem

@Composable
fun OrderStatusCard(
    modifier: Modifier = Modifier,
    item: UnpaidOrderItem,
    onClick: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null
) {
    val isPending = item.syncStatus == SyncStatus.PENDING
    val isFailed = item.syncStatus == SyncStatus.FAILED

    val bgColor = if (isFailed) Color(0xFF5E2E3A) else Color(0xFF3E3750)
    val border = if (isFailed) BorderStroke(1.dp, Color.Red) else null
    val clickModifier = if (!isPending && !isFailed && onClick != null) Modifier.clickable { onClick() } else Modifier

    Card(
        backgroundColor = bgColor,
        shape = RoundedCornerShape(16.dp),
        border = border,
        modifier = modifier
            .height(if (isFailed) 250.dp else 200.dp)
            .then(clickModifier)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isPending || isFailed) "Order" else "Order #${item.orderID}",
                    color = Color.White,
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Light
                )
            }

            Text(
                text = item.customerName,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                style = MaterialTheme.typography.h6
            )

            Text(
                text = item.packageType,
                color = Color.White,
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.Light
            )

            // Status
            Text(
                text = item.nowStatus,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(top = 12.dp)
            )

            // Bottom Info (Date or Actions + Sync Icon)
            Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                if (isFailed) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onCancel?.invoke() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White, backgroundColor = Color.Transparent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Cancel", style = MaterialTheme.typography.caption)
                        }

                        Button(
                            onClick = { onRetry?.invoke() },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFE57373), contentColor = Color.White),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Retry", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Retry", style = MaterialTheme.typography.caption)
                        }
                    }
                } else {
                    Column(modifier = Modifier.align(Alignment.BottomStart)) {
                        Text(
                            text = stringResource(R.string.order_date),
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            style = MaterialTheme.typography.subtitle2
                        )
                        Text(
                            text = item.orderDate,
                            color = Color.White,
                            fontWeight = FontWeight.Light,
                            style = MaterialTheme.typography.body2
                        )
                    }
                }

                // Small Sync Indicator at bottom right
                if (isPending) {
                    CircularProgressIndicator(
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp).align(Alignment.BottomEnd),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewOrderStatusCard() {
    OrderStatusCard(
        item = DUMMY_UNPAID_ORDER_ITEM_EMY
    )
}
