package com.raylabs.laundryhub.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.raylabs.laundryhub.ui.theme.PurpleLaundryHub

@Composable
fun DefaultTopAppBar(title: String) {
    Box(
        modifier = Modifier
            .background(PurpleLaundryHub)
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.h6
                )
            },
            backgroundColor = PurpleLaundryHub,
            contentColor = Color.White,
            elevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
    }
}

@Preview
@Composable
fun PreviewTopAppBar() {
    DefaultTopAppBar("History")
}