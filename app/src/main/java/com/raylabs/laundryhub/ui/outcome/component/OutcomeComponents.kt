package com.raylabs.laundryhub.ui.outcome.component

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
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.raylabs.laundryhub.ui.outcome.state.OutcomeHistoryItem

@Composable
fun OutcomeDateHeader(dateLabel: String, modifier: Modifier = Modifier) {
    Surface(
        color = Color(0xFF4D455D),
        modifier = modifier
            .fillMaxWidth()
    ) {
        Text(
            text = dateLabel,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun OutcomeHistoryCard(
    item: OutcomeHistoryItem,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        backgroundColor = Color(0xFF7E57C2),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Outcome #${item.id}",
                    style = MaterialTheme.typography.caption,
                    color = Color.White
                )
                Text(
                    text = item.paymentLabel,
                    style = MaterialTheme.typography.caption,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.purpose,
                    style = MaterialTheme.typography.body1,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = item.price,
                    style = MaterialTheme.typography.body1,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.remark.ifBlank { "-" },
                style = MaterialTheme.typography.body2,
                color = Color.White.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
