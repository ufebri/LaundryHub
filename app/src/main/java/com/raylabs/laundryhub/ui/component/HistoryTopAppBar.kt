package com.raylabs.laundryhub.ui.component

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.raylabs.laundryhub.ui.theme.PurpleLaundryHub

@Composable
fun DefaultTopAppBar(title: String) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.h6
            )
        },
        backgroundColor = PurpleLaundryHub,
        contentColor = Color.White,
        elevation = 0.dp
    )
}

@Preview
@Composable
fun PreviewTopAppBar() {
    DefaultTopAppBar("History")
}