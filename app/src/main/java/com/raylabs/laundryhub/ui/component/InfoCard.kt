package com.raylabs.laundryhub.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.raylabs.laundryhub.ui.home.state.SummaryItem

@Composable
fun InfoCard(
    summaryItem: SummaryItem,
    modifier: Modifier = Modifier
) {
    Card(
        backgroundColor = summaryItem.backgroundColor,
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.Black),
        modifier = modifier
            .height(110.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = summaryItem.title,
                style = MaterialTheme.typography.subtitle1,
                color = summaryItem.textColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = summaryItem.body,
                style = MaterialTheme.typography.body2,
                color = summaryItem.textColor
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = summaryItem.footer,
                    style = MaterialTheme.typography.caption.copy(
                        fontStyle = FontStyle.Italic
                    ),
                    color = summaryItem.textColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}