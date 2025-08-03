package com.raylabs.laundryhub.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.raylabs.laundryhub.ui.home.state.UnpaidOrderItem

@Composable
fun OrderStatusCard(
    modifier: Modifier = Modifier,
    item: UnpaidOrderItem,
    onClick: (() -> Unit)? = null
) {
    Card(
        backgroundColor = Color(0xFF3E3750),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .height(200.dp)
            .let { if (onClick != null) it.clickable { onClick() } else it }
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .fillMaxHeight(), // pastikan konten memenuhi tinggi card
            verticalArrangement = Arrangement.SpaceBetween // ratakan konten atas-bawah
        ) {
            // Header
            Text(
                "Order #${item.orderID}",
                color = Color.White,
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Light
            )
            Text(
                item.customerName,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                style = MaterialTheme.typography.h6
            )
            Text(
                item.packageType,
                color = Color.White,
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.Light
            )

            // Status
            Text(
                item.nowStatus,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(top = 12.dp)
            )

            // Due Date
            Text(
                "Due Date",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                style = MaterialTheme.typography.subtitle2,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                item.dueDate,
                color = Color.White,
                fontWeight = FontWeight.Light,
                style = MaterialTheme.typography.body2
            )
        }
    }
}

@Preview
@Composable
fun PreviewOrderStatusCard() {
    OrderStatusCard(
        Modifier,
        UnpaidOrderItem(
            "3",
            "Arifin",
            "Regular",
            "Unpaid",
            "17 Sep 25, 16.40 PM"
        )
    )
}
