package com.raylabs.laundryhub.ui.outcome

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.ui.component.DateHeader
import com.raylabs.laundryhub.ui.component.DefaultTopAppBar
import com.raylabs.laundryhub.ui.component.EntryItemCard
import com.raylabs.laundryhub.ui.component.InlineAdaptiveBannerAd
import com.raylabs.laundryhub.ui.component.InlineAdaptiveBannerAdState
import com.raylabs.laundryhub.ui.component.OutcomeBottomSheet
import com.raylabs.laundryhub.ui.component.TransactionDeleteConfirmationSheet
import com.raylabs.laundryhub.ui.component.TransactionEntryActionSheet
import com.raylabs.laundryhub.ui.component.rememberInlineAdaptiveBannerAdState
import com.raylabs.laundryhub.ui.home.state.SyncStatus
import com.raylabs.laundryhub.ui.outcome.state.DateListItemUI
import com.raylabs.laundryhub.ui.outcome.state.EntryItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun OutcomeScreenView(
    viewModel: OutcomeViewModel = hiltViewModel(),
    bannerState: InlineAdaptiveBannerAdState? = null,
    onOutcomeChanged: () -> Unit = {}
) {
    val state = viewModel.uiState
    val pagingItems = viewModel.outcomePagingData.collectAsLazyPagingItems()
    val resolvedBannerState = bannerState ?: rememberInlineAdaptiveBannerAdState("outcome_inline")
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedEntry by remember { mutableStateOf<EntryItem?>(null) }
    var pendingDeleteEntry by remember { mutableStateOf<EntryItem?>(null) }

    fun dismissSheet() {
        showBottomSheet = false
        selectedEntry = null
        viewModel.resetForm()
    }

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = { DefaultTopAppBar(title = stringResource(R.string.outcome)) },
        snackbarHost = { SnackbarHost(hostState = scaffoldState.snackbarHostState) },
        floatingActionButton = {
            if (!showBottomSheet) {
                FloatingActionButton(
                    onClick = {
                        viewModel.prepareNewOutcome()
                        showBottomSheet = true
                    },
                    backgroundColor = MaterialTheme.colors.primary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_outcome))
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutcomeContent(
                pagingItems = pagingItems,
                bannerState = resolvedBannerState,
                hiddenOutcomeIds = state.hiddenOutcomeIds,
                optimisticOutcomes = state.optimisticOutcomes,
                optimisticUpdates = state.optimisticUpdates,
                modifier = Modifier.fillMaxSize(),
                onRefresh = { pagingItems.refresh() },
                onEntryClick = { selectedEntry = it },
                onRetrySubmit = { fakeId ->
                    viewModel.retryOptimisticOutcome(
                        fakeId = fakeId,
                        onComplete = { realId ->
                            coroutineScope.launch {
                                scaffoldState.snackbarHostState.showSnackbar(
                                    context.getString(R.string.outcome_submit_success, realId)
                                )
                            }
                            pagingItems.refresh()
                        },
                        onError = { message ->
                            coroutineScope.launch {
                                scaffoldState.snackbarHostState.showSnackbar(
                                    message.ifBlank { "Failed to retry sync" }
                                )
                            }
                        }
                    )
                },
                onCancelSubmit = { fakeId ->
                    viewModel.removeOptimisticOutcome(fakeId)
                },
                onRetryUpdate = { id ->
                    viewModel.retryOptimisticUpdate(
                        id = id,
                        onComplete = {
                            coroutineScope.launch {
                                scaffoldState.snackbarHostState.showSnackbar(
                                    context.getString(R.string.outcome_update_success, id)
                                )
                            }
                            pagingItems.refresh()
                        },
                        onError = { message ->
                            coroutineScope.launch {
                                scaffoldState.snackbarHostState.showSnackbar(
                                    message.ifBlank { "Failed to retry update" }
                                )
                            }
                        }
                    )
                },
                onCancelUpdate = { id ->
                    viewModel.removeOptimisticUpdate(id)
                }
            )

            if (showBottomSheet) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colors.onSurface.copy(alpha = 0.38f))
                        .clickable(enabled = !state.isSubmitting) { dismissSheet() }
                ) {
                    OutcomeBottomSheet(
                        state = state,
                        onPurposeChanged = viewModel::onPurposeChanged,
                        onPriceChanged = viewModel::onPriceChanged,
                        onDateSelected = viewModel::onDateChanged,
                        onPaymentMethodSelected = viewModel::onPaymentMethodSelected,
                        onRemarkChanged = viewModel::onRemarkChanged,
                        onSubmit = {
                            val outcome = viewModel.buildOutcomeDataForSubmit()
                            dismissSheet()
                            viewModel.submitOutcome(
                                outcome = outcome,
                                onComplete = { createdOutcomeId ->
                                    onOutcomeChanged()
                                    coroutineScope.launch {
                                        scaffoldState.snackbarHostState.showSnackbar(
                                            context.getString(R.string.outcome_submit_success, createdOutcomeId)
                                        )
                                    }
                                    pagingItems.refresh()
                                },
                                onError = { message ->
                                    coroutineScope.launch {
                                        val snackbarResult = scaffoldState.snackbarHostState.showSnackbar(
                                            message = message.ifBlank { "Failed to sync outcome" },
                                            actionLabel = "Retry"
                                        )
                                        if (snackbarResult == SnackbarResult.ActionPerformed) {
                                            val failedItem = viewModel.uiState.optimisticOutcomes.lastOrNull { it.syncStatus == SyncStatus.FAILED }
                                            if (failedItem != null) {
                                                viewModel.retryOptimisticOutcome(failedItem.id)
                                            }
                                        }
                                    }
                                }
                            )
                        },
                        onUpdate = {
                            val outcome = viewModel.buildOutcomeDataForUpdate()
                            if (outcome != null) {
                                dismissSheet()
                                viewModel.updateOutcome(
                                    outcome = outcome,
                                    onComplete = {
                                        onOutcomeChanged()
                                        coroutineScope.launch {
                                            scaffoldState.snackbarHostState.showSnackbar(
                                                context.getString(R.string.outcome_update_success, outcome.id)
                                            )
                                        }
                                        pagingItems.refresh()
                                    },
                                    onError = { message ->
                                        coroutineScope.launch {
                                            val snackbarResult = scaffoldState.snackbarHostState.showSnackbar(
                                                message = message.ifBlank { "Failed to update outcome" },
                                                actionLabel = "Retry"
                                            )
                                            if (snackbarResult == SnackbarResult.ActionPerformed) {
                                                viewModel.retryOptimisticUpdate(outcome.id)
                                            }
                                        }
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .clickable {}
                    )
                }
            }

            TransactionEntryActionSheet(
                visible = selectedEntry != null,
                entry = selectedEntry,
                onUpdate = {
                    selectedEntry?.let { entry ->
                        coroutineScope.launch {
                            if (viewModel.onOutcomeEditClick(entry.id)) {
                                showBottomSheet = true
                                selectedEntry = null
                            }
                        }
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
                isDeleting = state.deleteOutcome.isLoading,
                onConfirm = {
                    pendingDeleteEntry?.let { entry ->
                        pendingDeleteEntry = null
                        viewModel.deleteOutcome(
                            outcomeId = entry.id,
                            onComplete = {
                                onOutcomeChanged()
                                coroutineScope.launch {
                                    scaffoldState.snackbarHostState.showSnackbar(
                                        context.getString(R.string.outcome_delete_success, entry.id)
                                    )
                                }
                                pagingItems.refresh()
                            },
                            onError = { message ->
                                coroutineScope.launch {
                                    val snackbarResult = scaffoldState.snackbarHostState.showSnackbar(
                                        message = message.ifBlank {
                                            context.getString(R.string.outcome_delete_failed)
                                        },
                                        actionLabel = "Retry"
                                    )
                                    if (snackbarResult == SnackbarResult.ActionPerformed) {
                                        viewModel.deleteOutcome(outcomeId = entry.id)
                                    }
                                }
                            }
                        )
                    }
                },
                onDismiss = {
                    if (!state.deleteOutcome.isLoading) {
                        pendingDeleteEntry = null
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun OutcomeContent(
    pagingItems: LazyPagingItems<DateListItemUI>,
    bannerState: InlineAdaptiveBannerAdState,
    modifier: Modifier,
    hiddenOutcomeIds: Set<String> = emptySet(),
    optimisticOutcomes: List<EntryItem> = emptyList(),
    optimisticUpdates: Map<String, EntryItem> = emptyMap(),
    onRefresh: () -> Unit = {},
    onEntryClick: (EntryItem) -> Unit = {},
    onRetrySubmit: (String) -> Unit = {},
    onCancelSubmit: (String) -> Unit = {},
    onRetryUpdate: (String) -> Unit = {},
    onCancelUpdate: (String) -> Unit = {}
) {
    val isRefreshing = pagingItems.loadState.refresh is androidx.paging.LoadState.Loading
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = onRefresh
    )

    val pagingIds = remember(pagingItems.itemCount) {
        (0 until pagingItems.itemCount).mapNotNull { index ->
            when (val item = pagingItems.peek(index)) {
                is DateListItemUI.Entry -> item.item.id
                else -> null
            }
        }.toSet()
    }

    val filteredOptimistic = optimisticOutcomes.filter { it.id !in pagingIds }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "outcome_inline_banner") {
                InlineAdaptiveBannerAd(state = bannerState)
            }

            items(
                count = filteredOptimistic.size,
                key = { index -> "opt_${filteredOptimistic[index].id}" }
            ) { index ->
                val item = filteredOptimistic[index]
                if (item.id !in hiddenOutcomeIds) {
                    EntryItemCard(
                        item = item,
                        onClick = { onEntryClick(item) },
                        onRetry = { onRetrySubmit(item.id) },
                        onCancel = { onCancelSubmit(item.id) }
                    )
                }
            }

            items(
                count = pagingItems.itemCount,
                key = { index ->
                    when (val item = pagingItems.peek(index)) {
                        is DateListItemUI.Header -> "header_${item.date}_$index"
                        is DateListItemUI.Entry -> "entry_${item.item.id}"
                        null -> "placeholder_$index"
                    }
                }
            ) { index ->
                when (val item = pagingItems[index]) {
                    is DateListItemUI.Header -> {
                        DateHeader(item.date)
                    }

                    is DateListItemUI.Entry -> {
                        val entryItem = item.item
                        if (entryItem.id !in hiddenOutcomeIds) {
                            val resolvedItem = optimisticUpdates[entryItem.id] ?: entryItem
                            EntryItemCard(
                                item = resolvedItem,
                                onClick = { onEntryClick(resolvedItem) },
                                onRetry = { onRetryUpdate(resolvedItem.id) },
                                onCancel = { onCancelUpdate(resolvedItem.id) }
                            )
                        }
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
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Preview
@Composable
fun PreviewOutcomeScreen() {
    Text("Outcome Preview")
}
