package com.raylabs.laundryhub.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raylabs.laundryhub.ui.common.util.TextUtil.capitalizeFirstLetter
import com.raylabs.laundryhub.ui.home.state.TodayActivityItem
import com.raylabs.laundryhub.ui.theme.PurpleLaundryHub

@Composable
fun Transaction(mTransaction: TodayActivityItem) {
    Card(
        shape = RoundedCornerShape(8.dp), modifier = Modifier
            .padding(vertical = 4.dp)
            .shadow(8.dp)
            .border(
                BorderStroke(width = 1.dp, color = Color.Black),
                shape = RoundedCornerShape(8.dp)
            )
            .fillMaxWidth(),
        backgroundColor = Color(0xFFFEF7FF)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .weight(1f)
            ) {
                Text(
                    color = Color.Black,
                    style = MaterialTheme.typography.subtitle1,
                    text = mTransaction.name.capitalizeFirstLetter(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(8.dp)
                )
                Text(
                    text = mTransaction.packageDuration,
                    modifier = Modifier.padding(bottom = 8.dp, start = 8.dp),
                    color = Color.Black
                )
            }
            Column(
                modifier = Modifier
                    .padding(8.dp)
            ) {
                Text(
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold,
                    text = mTransaction.status.capitalizeFirstLetter(),
                    color = mTransaction.statusColor,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(8.dp)
                )
                Text(
                    text = mTransaction.totalPrice,
                    modifier = Modifier
                        .padding(bottom = 8.dp, end = 8.dp)
                        .align(Alignment.End),
                    color = Color.Black
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewHomeScreen() {
    Transaction(
        TodayActivityItem(
            id = "1",
            name = "Customer A",
            totalPrice = "Rp 105.000,-",
            status = "Paid by Cash",
            statusColor = PurpleLaundryHub,
            packageDuration = "Express - 24H"
        )
    )
}