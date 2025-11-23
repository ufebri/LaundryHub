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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.ui.common.dummy.history.dummyHistoryItem
import com.raylabs.laundryhub.ui.outcome.state.EntryItem
import com.raylabs.laundryhub.ui.outcome.state.TypeCard
import com.raylabs.laundryhub.ui.theme.Purple300

@Composable
fun EntryItemCard(item: EntryItem, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        backgroundColor = Purple300,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {

        val formattedId = when (item.typeCard) {
            TypeCard.INCOME -> stringResource(id = R.string.order_id, item.id)
            TypeCard.OUTCOME -> stringResource(id = R.string.outcome_id, item.id)
        }

        Column(modifier = Modifier.padding(16.dp)) {
            // Baris atas: Order ID
            Text(
                text = formattedId,
                style = MaterialTheme.typography.caption,
                color = Color.White
            )

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

            // Baris ketiga: Package & Price
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