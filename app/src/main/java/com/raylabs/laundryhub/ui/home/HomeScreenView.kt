package com.raylabs.laundryhub.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.ui.common.util.showQuickSnackbar
import com.raylabs.laundryhub.ui.component.*
import com.raylabs.laundryhub.ui.home.state.*

private const val PENDING_ORDER_SEARCH_FIELD_DESCRIPTION = "Pending order search field"

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    bannerState: InlineAdaptiveBannerAdState? = null,
    onOrderCardClick: (String) -> Unit,
    onTodayActivityClick: (String) -> Unit,
    onGrossCardClick: () -> Unit,
    onReminderDiscoveryClick: (Boolean) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val pagingItems = viewModel.pendingOrdersPagingData.collectAsLazyPagingItems()
    val resolvedBannerState = bannerState ?: rememberInlineAdaptiveBannerAdState("home_inline")
    
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
        onReminderDiscoveryClick = onReminderDiscoveryClick
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
    onReminderDiscoveryClick: (Boolean) -> Unit
) {
    val snackBarHostState = remember { SnackbarHostState() }
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

    val isRefreshing = state.isRefreshing || pagingItems.loadState.refresh is androidx.paging.LoadState.Loading
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = onRefresh
    )

    Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Header Section
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    GreetingWithImageBackground(
                        username = state.user.data?.displayName ?: stringResource(R.string.guest),
                        imageSeed = state.user.data?.uid ?: "guest"
                    )

                    SectionOrLoading(
                        isLoading = state.summary.isLoading,
                        error = state.summary.errorMessage,
                        hasContent = !state.summary.data.isNullOrEmpty(),
                        showMiniLoading = !state.isRefreshing,
                        content = {
                            InfoCardSection(
                                summary = state.summary.data.orEmpty(),
                                onGrossCardClick = onGrossCardClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 300.dp)
                                    .padding(horizontal = 16.dp)
                                    .offset(y = 90.dp)
                            )
                        }
                    )
                }
            }

            item { Spacer(Modifier.height(120.dp)) }

            item {
                Text(
                    text = stringResource(R.string.today_activity),
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
                )
            }

            item {
                SectionOrLoading(
                    isLoading = state.todayIncome.isLoading,
                    error = state.todayIncome.errorMessage,
                    hasContent = !state.todayIncome.data.isNullOrEmpty(),
                    content = {
                        val list = state.todayIncome.data.orEmpty()
                        if (list.isEmpty()) {
                            Text("No Transactions Today", modifier = Modifier.padding(horizontal = 16.dp))
                        } else {
                            CardList(list, onItemClick = onTodayActivityClick)
                        }
                    }
                )
            }

            item { Spacer(Modifier.height(24.dp)) }

            state.reminderDiscovery?.let { discovery ->
                item {
                    ReminderDiscoveryCard(discovery, onClick = { onReminderDiscoveryClick(discovery.isReminderEnabled) })
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            item { InlineAdaptiveBannerAd(state = bannerState) }

            // Pending Orders Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.pending_orders),
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showSortSheet = true }) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Sort")
                    }
                }
            }

            // Pending Orders List via Paging
            val itemCount = pagingItems.itemCount
            val rowCount = (itemCount + 1) / 2
            
            items(rowCount) { rowIndex ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val firstIndex = rowIndex * 2
                    val secondIndex = firstIndex + 1
                    
                    pagingItems[firstIndex]?.let { item ->
                        OrderStatusCard(
                            item = item,
                            onClick = { onOrderCardClick(item.orderID) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    if (secondIndex < itemCount) {
                        pagingItems[secondIndex]?.let { item ->
                            OrderStatusCard(
                                item = item,
                                onClick = { onOrderCardClick(item.orderID) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            
            if (pagingItems.loadState.append is androidx.paging.LoadState.Loading) {
                item {
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
