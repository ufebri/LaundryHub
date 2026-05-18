package com.raylabs.laundryhub.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.shared.util.PlatformDate
import com.raylabs.laundryhub.ui.common.util.TextUtil.toRupiahFormat
import com.raylabs.laundryhub.ui.component.GreetingWithImageBackground
import com.raylabs.laundryhub.ui.component.InfoCard
import com.raylabs.laundryhub.ui.component.InlineAdaptiveBannerAd
import com.raylabs.laundryhub.ui.component.InlineAdaptiveBannerAdState
import com.raylabs.laundryhub.ui.component.OrderStatusCard
import com.raylabs.laundryhub.ui.component.SelectionSheetInlineOverlay
import com.raylabs.laundryhub.ui.component.Transaction
import com.raylabs.laundryhub.ui.component.rememberInlineAdaptiveBannerAdState
import com.raylabs.laundryhub.ui.home.state.HomeUiState
import com.raylabs.laundryhub.ui.home.state.ReminderDiscoveryUiState
import com.raylabs.laundryhub.ui.home.state.SortOption
import com.raylabs.laundryhub.ui.home.state.SummaryItem
import com.raylabs.laundryhub.ui.home.state.TransactionItem
import com.raylabs.laundryhub.ui.home.state.UnpaidOrderItem
import com.raylabs.laundryhub.ui.home.state.toColor

private const val PENDING_ORDER_SEARCH_FIELD_DESCRIPTION = "Pending order search field"

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    bannerState: InlineAdaptiveBannerAdState? = null,
    onOrderCardClick: (String) -> Unit,
    onTodayActivityClick: (String) -> Unit,
    onGrossCardClick: () -> Unit,
    onReminderDiscoveryClick: (Boolean) -> Unit,
    onRetryOptimisticOrder: (String) -> Unit,
    onCancelOptimisticOrder: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val pagingItems = viewModel.pendingOrdersPagingData.collectAsLazyPagingItems()
    val resolvedBannerState = bannerState ?: rememberInlineAdaptiveBannerAdState("home_inline")

    // Trigger Paging 3 refresh when the counter changes, WITHOUT recreating the whole flow
    androidx.compose.runtime.LaunchedEffect(state.refreshCounter) {
        if (state.refreshCounter > 0) {
            pagingItems.refresh()
        }
    }

    HomeScreenContent(
        state = state,
        pagingItems = pagingItems,
        bannerState = resolvedBannerState,
        onRefresh = {
            viewModel.refreshAllData()
            pagingItems.refresh()
        },
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onToggleSearch = viewModel::toggleSearch,
        onChangeSortOrder = viewModel::changeSortOrder,
        onOrderCardClick = onOrderCardClick,
        onTodayActivityClick = onTodayActivityClick,
        onGrossCardClick = onGrossCardClick,
        onReminderDiscoveryClick = onReminderDiscoveryClick,
        onRetryOptimisticOrder = onRetryOptimisticOrder,
        onCancelOptimisticOrder = onCancelOptimisticOrder
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomeScreenContent(
    state: HomeUiState,
    pagingItems: LazyPagingItems<UnpaidOrderItem>,
    bannerState: InlineAdaptiveBannerAdState,
    onRefresh: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onChangeSortOrder: (SortOption) -> Unit,
    onOrderCardClick: (String) -> Unit,
    onTodayActivityClick: (String) -> Unit,
    onGrossCardClick: () -> Unit,
    onReminderDiscoveryClick: (Boolean) -> Unit,
    onRetryOptimisticOrder: (String) -> Unit,
    onCancelOptimisticOrder: (String) -> Unit
) {
    var showSortSheet by remember { mutableStateOf(false) }
    val sortOptions = remember {
        listOf(
            SortOption.DUE_DATE_ASC,
            SortOption.DUE_DATE_DESC,
            SortOption.ORDER_DATE_ASC,
            SortOption.ORDER_DATE_DESC
        )
    }
    val sortLabels = mapOf(
        SortOption.DUE_DATE_ASC to stringResource(R.string.due_date_earliest),
        SortOption.DUE_DATE_DESC to stringResource(R.string.due_date_latest),
        SortOption.ORDER_DATE_ASC to stringResource(R.string.order_date_oldest),
        SortOption.ORDER_DATE_DESC to stringResource(R.string.order_date_newest)
    )
    val searchIconDescription = stringResource(R.string.search_icon)
    val clearSearchDescription = stringResource(R.string.clear_search)
    val closeSearchDescription = stringResource(R.string.close_search_view)
    val openSearchDescription = stringResource(R.string.open_search_view)
    val sortOrdersDescription = stringResource(R.string.sort_orders)

    // Only show the large refresh spinner if it's a manual refresh OR the very first load (itemCount == 0)
    val isRefreshing = state.isRefreshing || (pagingItems.loadState.refresh is androidx.paging.LoadState.Loading && pagingItems.itemCount == 0)
    val isPendingOrdersRefreshing =
        pagingItems.loadState.refresh is androidx.paging.LoadState.Loading && pagingItems.itemCount > 0
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = onRefresh
    )

    // Clean up synced orders from ViewModel if they are already in Paging
    val pagingIds = mutableSetOf<String>()
    for (i in 0 until pagingItems.itemCount) {
        pagingItems.peek(i)?.orderID?.let { pagingIds.add(it) }
    }
    val syncedAndPresent = state.optimisticOrders.filter {
        it.syncStatus == com.raylabs.laundryhub.ui.home.state.SyncStatus.SYNCED && it.orderID in pagingIds
    }
    if (syncedAndPresent.isNotEmpty()) {
        androidx.compose.runtime.LaunchedEffect(syncedAndPresent) {
            syncedAndPresent.forEach { onCancelOptimisticOrder(it.orderID) }
        }
    }

    Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Section
            item(span = { GridItemSpan(2) }) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    GreetingWithImageBackground(
                        username = state.user.data?.displayName ?: stringResource(R.string.guest),
                        imageSeed = state.user.data?.uid ?: "guest"
                    )

                    // Tetap tampilkan konten summary meskipun sedang refresh agar layar tidak loncat
                    InfoCardSection(
                        summary = state.summary.data.orEmpty(),
                        onGrossCardClick = onGrossCardClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .padding(horizontal = 16.dp)
                            .offset(y = 90.dp)
                    )
                    
                    if (state.summary.isLoading && state.summary.data.isNullOrEmpty()) {
                        // Hanya tampilkan loading spinner jika data benar-benar kosong (first load)
                        Box(modifier = Modifier.fillMaxWidth().height(300.dp).offset(y = 90.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }

            item(span = { GridItemSpan(2) }) { Spacer(Modifier.height(120.dp)) }

            item(span = { GridItemSpan(2) }) {
                Text(
                    text = stringResource(R.string.today_activity),
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
                )
            }

            item(span = { GridItemSpan(2) }) {
                val todayDate = PlatformDate.getTodayDate("dd/MM/yyyy")
                val optimisticForToday = state.optimisticOrders
                    .filter { it.orderDate == todayDate }
                    .map {
                        TransactionItem(
                            id = it.orderID,
                            name = it.customerName,
                            totalPrice = it.rawPayload?.totalPrice?.toRupiahFormat() ?: "",
                            status = it.nowStatus,
                            statusColor = it.nowStatus.toColor(),
                            packageDuration = it.packageType
                        )
                    }
                val realList = state.todayIncome.data.orEmpty()
                val combinedList = (optimisticForToday + realList).distinctBy { it.id }

                Box(modifier = Modifier.fillMaxWidth()) {
                    if (combinedList.isEmpty() && !state.todayIncome.isLoading) {
                        Text(stringResource(R.string.no_transactions_today), modifier = Modifier.padding(horizontal = 16.dp))
                    } else {
                        CardList(combinedList, onItemClick = onTodayActivityClick)
                    }

                    if (state.todayIncome.isLoading && combinedList.isEmpty()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center).padding(16.dp))
                    }
                }
            }

            item(span = { GridItemSpan(2) }) { Spacer(Modifier.height(24.dp)) }

            state.reminderDiscovery?.let { discovery ->
                item(span = { GridItemSpan(2) }) {
                    ReminderDiscoveryCard(discovery, onClick = { onReminderDiscoveryClick(discovery.isReminderEnabled) })
                }
                item(span = { GridItemSpan(2) }) { Spacer(Modifier.height(8.dp)) }
            }

            item(span = { GridItemSpan(2) }) {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    InlineAdaptiveBannerAd(state = bannerState)
                }
            }

            // Pending Orders Title
            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.isSearchActive) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = onSearchQueryChanged,
                            enabled = !isPendingOrdersRefreshing,
                            modifier = Modifier
                                .weight(1f)
                                .semantics {
                                    contentDescription = PENDING_ORDER_SEARCH_FIELD_DESCRIPTION
                                },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.search_customer_placeholder),
                                    style = MaterialTheme.typography.body2,
                                    color = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.medium)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = searchIconDescription,
                                    tint = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.high)
                                )
                            },
                            trailingIcon = {
                                if (state.searchQuery.isNotEmpty()) {
                                    IconButton(
                                        enabled = !isPendingOrdersRefreshing,
                                        onClick = { onSearchQueryChanged("") }
                                    ) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = clearSearchDescription,
                                            tint = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.high)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                backgroundColor = MaterialTheme.colors.surface,
                                cursorColor = MaterialTheme.colors.primary,
                                focusedBorderColor = MaterialTheme.colors.primary,
                                unfocusedBorderColor = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.disabled)
                            )
                        )
                        if (isPendingOrdersRefreshing) {
                            Spacer(modifier = Modifier.width(8.dp))
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        }
                        IconButton(enabled = !isPendingOrdersRefreshing, onClick = onToggleSearch) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = closeSearchDescription,
                                tint = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.high)
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.pending_orders),
                            style = MaterialTheme.typography.h6,
                            modifier = Modifier.weight(1f)
                        )
                        if (isPendingOrdersRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        IconButton(enabled = !isPendingOrdersRefreshing, onClick = onToggleSearch) {
                            Icon(Icons.Filled.Search, contentDescription = openSearchDescription)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(enabled = !isPendingOrdersRefreshing, onClick = { showSortSheet = true }) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = sortOrdersDescription)
                        }
                    }
                }
            }

            // Unified Orders Grid (Optimistic + Paging)
            val filteredOptimistic = state.optimisticOrders.filter { it.orderID !in pagingIds }

            // 1. Filtered Optimistic Items
            items(
                count = filteredOptimistic.size,
                key = { index -> filteredOptimistic[index].orderID }
            ) { index ->
                val item = filteredOptimistic[index]
                Box(Modifier.padding(start = if (index % 2 == 0) 16.dp else 0.dp, end = if (index % 2 == 0) 0.dp else 16.dp, bottom = 12.dp)) {
                    OrderStatusCard(
                        item = item,
                        onClick = { onOrderCardClick(item.orderID) },
                        onRetry = { onRetryOptimisticOrder(item.orderID) },
                        onCancel = { onCancelOptimisticOrder(item.orderID) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 2. Real Paging Items
            items(
                count = pagingItems.itemCount,
                key = { index -> pagingItems.peek(index)?.orderID ?: "paging_$index" }
            ) { index ->
                val item = pagingItems[index]
                if (item != null) {
                    val globalIndex = filteredOptimistic.size + index
                    Box(Modifier.padding(start = if (globalIndex % 2 == 0) 16.dp else 0.dp, end = if (globalIndex % 2 == 0) 0.dp else 16.dp, bottom = 12.dp)) {
                        OrderStatusCard(
                            item = item,
                            onClick = { onOrderCardClick(item.orderID) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (pagingItems.loadState.append is androidx.paging.LoadState.Loading) {
                item(span = { GridItemSpan(2) }) {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }
        }

        PullRefreshIndicator(isRefreshing, pullRefreshState, Modifier.align(Alignment.TopCenter))

        SelectionSheetInlineOverlay(
            visible = showSortSheet,
            title = stringResource(R.string.sort_orders),
            options = sortOptions,
            selectedOption = state.currentSortOption,
            onDismiss = { showSortSheet = false },
            onOptionSelected = onChangeSortOrder,
            optionTitle = { sortLabels.getValue(it) }
        )
    }
}

@Composable
private fun ReminderDiscoveryCard(state: ReminderDiscoveryUiState, onClick: () -> Unit) {
    Card(
        backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.12f),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(state.headline, style = MaterialTheme.typography.subtitle1)
            Text(state.supportingText, style = MaterialTheme.typography.body2)
            Text(state.ctaLabel, color = MaterialTheme.colors.primary, modifier = Modifier.align(Alignment.End))
        }
    }
}

@Composable
fun InfoCardSection(summary: List<SummaryItem>, onGrossCardClick: () -> Unit, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = {
            items(items = summary, key = { it.title }) { mData ->
                InfoCard(summaryItem = mData, onClick = if (mData.isInteractive) onGrossCardClick else null)
            }
        }
    )
}

@Composable
fun CardList(state: List<TransactionItem>, onItemClick: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.forEach { Transaction(it, onClick = { onItemClick(it.id) }) }
    }
}
