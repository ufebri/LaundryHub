package com.raylabs.laundryhub.ui.history

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.raylabs.laundryhub.ui.common.dummy.history.dummyHistoryUiState
import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.component.DefaultTopAppBar
import com.raylabs.laundryhub.ui.component.HistoryItemCard
import com.raylabs.laundryhub.ui.component.SectionOrLoading
import com.raylabs.laundryhub.ui.history.state.HistoryItem
import com.raylabs.laundryhub.ui.history.state.HistoryUiItem
import com.raylabs.laundryhub.ui.history.state.HistoryUiState

@Composable
fun HistoryScreenView(viewModel: HistoryViewModel = hiltViewModel()) {
    val state = viewModel.uiState

    Scaffold(
        topBar = { DefaultTopAppBar(title = "History") }
    ) { padding ->
        HistoryContent(state, modifier = Modifier.padding(padding))
    }
}

@Composable
fun HistoryContent(state: HistoryUiState, modifier: Modifier) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state) {
        listOf(state.history).forEach {
            it.errorMessage?.let { msg -> snackbarHostState.showSnackbar(msg) }
        }
    }

    SectionOrLoading(
        isLoading = state.history.isLoading,
        error = state.history.errorMessage,
        content = {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
            ) {
                items(
                    items = state.history.data.orEmpty(),
                    key = { item ->
                        when (item) {
                            is HistoryUiItem.Header -> "header_${item.date}"
                            is HistoryUiItem.Entry -> "entry_${item.item.orderId}"
                        }
                    }
                ) { item ->
                    when (item) {
                        is HistoryUiItem.Header -> {
                            HistoryDateHeader(item.date)
                        }

                        is HistoryUiItem.Entry -> {
                            HistoryItemCard(item.item)
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun HistoryDateHeader(date: String) {
    Surface(
        color = Color(0xFF4D455D), // warna ungu gelap
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(
            text = date,
            style = MaterialTheme.typography.body1,
            color = Color.White,
            modifier = Modifier
                .padding(8.dp)
        )
    }
}

@Preview
@Composable
fun PreviewHistoryScreen() {
    Scaffold(
        topBar = { DefaultTopAppBar(title = "History") }
    ) { padding ->
        HistoryContent(dummyHistoryUiState, modifier = Modifier.padding(padding))
    }
}