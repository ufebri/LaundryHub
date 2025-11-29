package com.raylabs.laundryhub.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.raylabs.laundryhub.ui.common.util.DateUtil
import com.raylabs.laundryhub.ui.theme.Purple900

@Composable
fun DateHeader(date: String) {
    Surface(
        color = Purple900,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(
            text = DateUtil.formatToLongDate(
                dateString = date,
                inputFormat = DateUtil.STANDARD_DATE_FORMATED
            ),
            style = MaterialTheme.typography.body1,
            color = Color.White,
            modifier = Modifier
                .padding(8.dp)
        )
    }
}

@Preview
@Composable
fun PreviewDateHeader() {
    DateHeader("12/08/2023")
}