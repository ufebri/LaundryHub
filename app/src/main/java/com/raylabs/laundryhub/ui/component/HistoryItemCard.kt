package com.raylabs.laundryhub.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.raylabs.laundryhub.ui.history.state.HistoryItem

@Composable
fun HistoryItemCard(item: HistoryItem, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        backgroundColor = Color(0xFF7E57C2),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Baris atas: Order ID
            Text(
                text = "Order #${item.orderId}",
                style = MaterialTheme.typography.caption,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Baris kedua: Name & Payment Status
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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

            // Baris ketiga: Package & Price
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = item.packageType,
                    style = MaterialTheme.typography.body2,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = item.totalPrice,
                    style = MaterialTheme.typography.body2,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewHistoryItemCard() {
    HistoryItemCard(
        item = HistoryItem(
            orderId = "#INV-001",
            name = "Ny Emy",
            packageType = "Express - 6H",
            paymentStatus = "Lunas",
            totalPrice = "Rp 105.000,-",
            formattedDate = "1 June 2025",
            isPaid = true
        )
    )
}