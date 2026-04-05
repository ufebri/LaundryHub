package com.raylabs.laundryhub.ui.history

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.ui.common.dummy.history.dummyHistoryUiState
import com.raylabs.laundryhub.ui.component.DateHeader
import com.raylabs.laundryhub.ui.component.DefaultTopAppBar
import com.raylabs.laundryhub.ui.component.EntryItemCard
import com.raylabs.laundryhub.ui.component.InlineAdaptiveBannerAd
import com.raylabs.laundryhub.ui.component.InlineAdaptiveBannerAdState
import com.raylabs.laundryhub.ui.component.SectionOrLoading
import com.raylabs.laundryhub.ui.component.rememberInlineAdaptiveBannerAdState
import com.raylabs.laundryhub.ui.history.state.HistoryUiState
import com.raylabs.laundryhub.ui.outcome.state.DateListItemUI

@Composable
fun HistoryScreenView(
    viewModel: HistoryViewModel = hiltViewModel(),
    bannerState: InlineAdaptiveBannerAdState? = null
) {
    val state = viewModel.uiState
    val resolvedBannerState = bannerState ?: rememberInlineAdaptiveBannerAdState("history_inline")

    Scaffold(
        topBar = { DefaultTopAppBar(title = stringResource(R.string.history)) }
    ) { padding ->
        HistoryContent(
            state,
            bannerState = resolvedBannerState,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
fun HistoryContent(
    state: HistoryUiState,
    bannerState: InlineAdaptiveBannerAdState,
    modifier: Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state) {
        listOf(state.history).forEach {
            it.errorMessage?.let { msg -> snackbarHostState.showSnackbar(msg) }
        }
    }

    SectionOrLoading(
        isLoading = state.history.isLoading,
        error = state.history.errorMessage,
        hasContent = !state.history.data.isNullOrEmpty(),
        content = {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
            ) {
                item(key = "history_inline_banner") {
                    InlineAdaptiveBannerAd(state = bannerState)
                }

                items(
                    items = state.history.data.orEmpty(),
                    key = { item ->
                        when (item) {
                            is DateListItemUI.Header -> "header_${item.date}"
                            is DateListItemUI.Entry -> "entry_${item.item.id}"
                        }
                    }
                ) { item ->
                    when (item) {
                        is DateListItemUI.Header -> {
                            DateHeader(item.date)
                        }

                        is DateListItemUI.Entry -> {
                            EntryItemCard(item.item)
                        }
                    }
                }
            }
        }
    )
}

@Preview
@Composable
fun PreviewHistoryScreen() {
    val bannerState = rememberInlineAdaptiveBannerAdState("preview_history_inline")
    Scaffold(
        topBar = { DefaultTopAppBar(title = "History") }
    ) { padding ->
        HistoryContent(
            dummyHistoryUiState,
            bannerState = bannerState,
            modifier = Modifier.padding(padding)
        )
    }
}
