package com.raylabs.laundryhub.ui.history

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.ui.common.dummy.history.dummyHistoryUiState
import com.raylabs.laundryhub.ui.component.DateHeader
import com.raylabs.laundryhub.ui.component.DefaultTopAppBar
import com.raylabs.laundryhub.ui.component.EntryItemCard
import com.raylabs.laundryhub.ui.component.InlineAdaptiveBannerAd
import com.raylabs.laundryhub.ui.component.InlineAdaptiveBannerAdState
import com.raylabs.laundryhub.ui.component.TransactionDeleteConfirmationSheet
import com.raylabs.laundryhub.ui.component.TransactionEntryActionSheet
import com.raylabs.laundryhub.ui.component.rememberInlineAdaptiveBannerAdState
import com.raylabs.laundryhub.ui.history.state.HistoryUiState
import com.raylabs.laundryhub.ui.outcome.state.DateListItemUI
import com.raylabs.laundryhub.ui.outcome.state.EntryItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HistoryScreenView(
    viewModel: HistoryViewModel = hiltViewModel(),
    bannerState: InlineAdaptiveBannerAdState? = null,
    onEditOrderRequest: (String) -> Unit = {},
    onOrderChanged: () -> Unit = {}
) {
    val state = viewModel.uiState
    val pagingItems = viewModel.historyPagingData.collectAsLazyPagingItems()
    val resolvedBannerState = bannerState ?: rememberInlineAdaptiveBannerAdState("history_inline")
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var selectedEntry by remember { mutableStateOf<EntryItem?>(null) }
    var pendingDeleteEntry by remember { mutableStateOf<EntryItem?>(null) }

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = { DefaultTopAppBar(title = stringResource(R.string.history)) },
        snackbarHost = { SnackbarHost(hostState = scaffoldState.snackbarHostState) }
    ) { padding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            HistoryContent(
                state = state,
                pagingItems = pagingItems,
                bannerState = resolvedBannerState,
                modifier = Modifier.fillMaxSize(),
                onRefresh = { pagingItems.refresh() },
                onEntryClick = { selectedEntry = it }
            )

            TransactionEntryActionSheet(
                visible = selectedEntry != null,
                entry = selectedEntry,
                onUpdate = {
                    selectedEntry?.let { entry ->
                        selectedEntry = null
                        onEditOrderRequest(entry.id)
                    }
                },
                onDelete = {
                    pendingDeleteEntry = selectedEntry
                    selectedEntry = null
                },
                onDismiss = { selectedEntry = null }
            )

            TransactionDeleteConfirmationSheet(
                visible = pendingDeleteEntry != null,
                entry = pendingDeleteEntry,
                isDeleting = state.deleteOrder.isLoading,
                onConfirm = {
                    pendingDeleteEntry?.let { entry ->
                        coroutineScope.launch {
                            viewModel.deleteOrder(
                                orderId = entry.id,
                                onComplete = {
                                    pendingDeleteEntry = null
                                    onOrderChanged()
                                    pagingItems.refresh()
                                    scaffoldState.snackbarHostState.showSnackbar(
                                        context.getString(R.string.order_delete_success, entry.id)
                                    )
                                },
                                onError = { message ->
                                    scaffoldState.snackbarHostState.showSnackbar(
                                        message.ifBlank {
                                            context.getString(R.string.order_delete_failed)
                                        }
                                    )
                                }
                            )
                        }
                    }
                },
                onDismiss = {
                    if (!state.deleteOrder.isLoading) {
                        pendingDeleteEntry = null
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HistoryContent(
    state: HistoryUiState,
    pagingItems: LazyPagingItems<DateListItemUI>,
    bannerState: InlineAdaptiveBannerAdState,
    modifier: Modifier,
    onRefresh: () -> Unit = {},
    onEntryClick: (EntryItem) -> Unit = {}
) {
    val isRefreshing = pagingItems.loadState.refresh is androidx.paging.LoadState.Loading
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = onRefresh
    )

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "history_inline_banner") {
                InlineAdaptiveBannerAd(state = bannerState)
            }

            items(
                count = pagingItems.itemCount,
                key = { index ->
                    val item = pagingItems[index]
                    when (item) {
                        is DateListItemUI.Header -> "header_\${item.date}_\$index"
                        is DateListItemUI.Entry -> "entry_\${item.item.id}"
                        null -> "placeholder_\$index"
                    }
                }
            ) { index ->
                val item = pagingItems[index]
                when (item) {
                    is DateListItemUI.Header -> {
                        DateHeader(item.date)
                    }

                    is DateListItemUI.Entry -> {
                        EntryItemCard(
                            item = item.item,
                            onClick = { onEntryClick(item.item) }
                        )
                    }
                    
                    null -> {
                        // Placeholder
                    }
                }
            }
        }

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(androidx.compose.ui.Alignment.TopCenter)
        )
    }
}

@Preview
@Composable
fun PreviewHistoryScreen() {
    // Simplified preview for build stability
    Text("History Preview")
}
