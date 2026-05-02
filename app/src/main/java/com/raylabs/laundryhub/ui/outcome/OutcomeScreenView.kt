package com.raylabs.laundryhub.ui.outcome

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.ui.component.*
import com.raylabs.laundryhub.ui.outcome.state.DateListItemUI
import com.raylabs.laundryhub.ui.outcome.state.EntryItem
import com.raylabs.laundryhub.ui.outcome.state.OutcomeUiState
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
            FloatingActionButton(
                onClick = {
                    viewModel.prepareNewOutcome()
                    showBottomSheet = true
                },
                backgroundColor = MaterialTheme.colors.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Outcome")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutcomeContent(
                state = state,
                pagingItems = pagingItems,
                bannerState = resolvedBannerState,
                modifier = Modifier.fillMaxSize(),
                onRefresh = { pagingItems.refresh() },
                onEntryClick = { selectedEntry = it }
            )

            if (showBottomSheet) {
                // Using a simple Column for the bottom sheet for stability
                OutcomeBottomSheet(
                    state = state,
                    onPurposeChanged = viewModel::onPurposeChanged,
                    onPriceChanged = viewModel::onPriceChanged,
                    onDateSelected = viewModel::onDateChanged,
                    onPaymentMethodSelected = viewModel::onPaymentMethodSelected,
                    onRemarkChanged = viewModel::onRemarkChanged,
                    onSubmit = {
                        val outcome = viewModel.buildOutcomeDataForSubmit()
                        if (outcome != null) {
                            coroutineScope.launch {
                                viewModel.submitOutcome(outcome) {
                                    dismissSheet()
                                    pagingItems.refresh()
                                    onOutcomeChanged()
                                }
                            }
                        }
                    },
                    onUpdate = {
                        val outcome = viewModel.buildOutcomeDataForUpdate()
                        if (outcome != null) {
                            coroutineScope.launch {
                                viewModel.updateOutcome(outcome) {
                                    dismissSheet()
                                    pagingItems.refresh()
                                    onOutcomeChanged()
                                }
                            }
                        }
                    }
                )
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
                        coroutineScope.launch {
                            viewModel.deleteOutcome(
                                outcomeId = entry.id,
                                onComplete = {
                                    pendingDeleteEntry = null
                                    pagingItems.refresh()
                                    onOutcomeChanged()
                                    scaffoldState.snackbarHostState.showSnackbar(
                                        context.getString(R.string.outcome_delete_success, entry.id)
                                    )
                                },
                                onError = { message ->
                                    scaffoldState.snackbarHostState.showSnackbar(
                                        message.ifBlank {
                                            context.getString(R.string.outcome_delete_failed)
                                        }
                                    )
                                }
                            )
                        }
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
    state: OutcomeUiState,
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
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Preview
@Composable
fun PreviewOutcomeScreen() {
    Text("Outcome Preview")
}
