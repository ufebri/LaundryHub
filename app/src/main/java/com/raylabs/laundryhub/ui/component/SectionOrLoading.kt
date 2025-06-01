package com.raylabs.laundryhub.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun SectionOrLoading(
    isLoading: Boolean,
    error: String?,
    content: @Composable () -> Unit
) {
    when {
        isLoading -> {
            CircularProgressIndicator(modifier = Modifier.padding(vertical = 16.dp))
        }

        error != null -> {
            Text(
                "Error: $error",
                color = Color.Red,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                textAlign = TextAlign.Center
            )
        }

        else -> {
            content()
        }
    }
}