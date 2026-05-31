package com.raylabs.laundryhub.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.raylabs.laundryhub.R

@Composable
fun CardItem() {
    Card(
        shape = RoundedCornerShape(size = 8.dp), modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row {
                Text(text = stringResource(R.string.order_date), style = MaterialTheme.typography.subtitle1)
                Text(text = stringResource(R.string.order_date))
            }
            Text(text = stringResource(R.string.order_date), style = MaterialTheme.typography.subtitle1)
            Text(text = stringResource(R.string.order_date))
        }
    }
}

@Preview
@Composable
fun PreviewCardItem() {
    CardItem()
}