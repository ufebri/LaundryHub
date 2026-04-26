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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.SnackbarHostState
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.ui.common.dummy.home.dummyState
import com.raylabs.laundryhub.ui.component.GreetingWithImageBackground
import com.raylabs.laundryhub.ui.component.InfoCard
import com.raylabs.laundryhub.ui.component.InlineAdaptiveBannerAd
import com.raylabs.laundryhub.ui.component.InlineAdaptiveBannerAdState
import com.raylabs.laundryhub.ui.component.OrderStatusCard
import com.raylabs.laundryhub.ui.component.SectionOrLoading
import com.raylabs.laundryhub.ui.component.SelectionSheetInlineOverlay
import com.raylabs.laundryhub.ui.component.Transaction
import com.raylabs.laundryhub.ui.component.rememberInlineAdaptiveBannerAdState
import com.raylabs.laundryhub.ui.home.state.HomeUiState
import com.raylabs.laundryhub.ui.home.state.ReminderDiscoveryUiState
import com.raylabs.laundryhub.ui.home.state.SortOption
import com.raylabs.laundryhub.ui.home.state.SummaryItem
import com.raylabs.laundryhub.ui.home.state.TransactionItem

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
    val resolvedBannerState = bannerState ?: rememberInlineAdaptiveBannerAdState("home_inline")
    HomeScreenContent(
        state = state,
        bannerState = resolvedBannerState,
        onRefresh = viewModel::refreshAllData,
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

    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isRefreshing,
        onRefresh = onRefresh
    )

    LaunchedEffect(state.orderUpdateKey, state.user.errorMessage, state.todayIncome.errorMessage, state.summary.errorMessage, state.gross.errorMessage, state.unpaidOrder.errorMessage) {
        listOf(state.user, state.todayIncome, state.summary, state.gross, state.unpaidOrder).forEach {
            it.errorMessage?.let { msg -> snackBarHostState.showSnackbar(msg) }
        }
    }

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
                        imageSeed = state.user.data?.uid
                            ?.takeIf { it.isNotBlank() }
                            ?: state.user.data?.email
                            ?.takeIf { it.isNotBlank() }
                            ?: state.user.data?.displayName
                            ?: stringResource(R.string.guest)
                    )

                    SectionOrLoading(
                        isLoading = state.summary.isLoading,
                        error = state.summary.errorMessage,
                        hasContent = !state.summary.data.isNullOrEmpty(),
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
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
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
                            Text(
                                text = stringResource(R.string.no_transactions_today),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        } else {
                            CardList(list, onItemClick = onTodayActivityClick)
                        }
                    }
                )
            }

            item { Spacer(Modifier.height(24.dp)) }

            state.reminderDiscovery?.let { discovery ->
                item(key = "home_reminder_discovery") {
                    ReminderDiscoveryCard(
                        state = discovery,
                        onClick = { onReminderDiscoveryClick(discovery.isReminderEnabled) }
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            item(key = "home_inline_banner") {
                InlineAdaptiveBannerAd(state = bannerState)
            }

            // Pending Orders Section Title / Search Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.isSearchActive) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = onSearchQueryChanged,
                            modifier = Modifier
                                .weight(1f)
                                .semantics {
                                    contentDescription = PENDING_ORDER_SEARCH_FIELD_DESCRIPTION
                                },
                            placeholder = { 
                                Text(
                                    stringResource(R.string.search_customer_placeholder),
                                    color = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.medium) // Use onBackground
                                )
                             },
                            leadingIcon = { 
                                Icon( 
                                    Icons.Filled.Search, 
                                    contentDescription = stringResource(R.string.search_icon),
                                    tint = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.high) // Use onBackground
                                )
                            },
                            trailingIcon = {
                                if (state.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchQueryChanged("") }) {
                                        Icon(
                                            Icons.Filled.Close, 
                                            contentDescription = stringResource(R.string.clear_search),
                                            tint = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.high) // Use onBackground
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = MaterialTheme.colors.onBackground, // Use onBackground
                                cursorColor = MaterialTheme.colors.primary,
                                focusedBorderColor = MaterialTheme.colors.primary,
                                unfocusedBorderColor = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.disabled) // Use onBackground
                            )
                        )
                        IconButton(onClick = onToggleSearch) { 
                            Icon(
                                Icons.Filled.Close, 
                                contentDescription = stringResource(R.string.close_search_view),
                                tint = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.high) // Use onBackground
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.pending_orders),
                            style = MaterialTheme.typography.h6,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onToggleSearch) {
                            Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.open_search_view))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = { showSortSheet = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.List,
                                contentDescription = stringResource(R.string.sort_orders)
                            )
                        }
                    }
                }
            }

            // Pending Orders List or Loading Indicator
            val ordersToDisplay = state.unpaidOrder.data.orEmpty()
            val orderRows = ordersToDisplay.chunked(2)

            if (state.unpaidOrder.isLoading && !state.isRefreshing && ordersToDisplay.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            if (!state.isRefreshing && state.unpaidOrder.isLoading && ordersToDisplay.isNotEmpty()) {
                item(key = "pending_orders_loading_overlay") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    }
                }
            }

            if (!state.isRefreshing) {
                if (ordersToDisplay.isEmpty()) {
                    item {
                        Text(
                            text = if (state.isSearchActive && state.searchQuery.isNotEmpty()) stringResource(R.string.no_search_results, state.searchQuery)
                            else stringResource(R.string.no_data),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    }
                } else {
                    items(
                        items = orderRows,
                        key = { row -> row.joinToString(separator = "_") { it.orderID } },
                        contentType = { "pending_order_row" }
                    ) { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { item ->
                                OrderStatusCard(
                                    item = item,
                                    onClick = { onOrderCardClick(item.orderID) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        } 

        PullRefreshIndicator(
            refreshing = state.isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )

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
private fun ReminderDiscoveryCard(
    state: ReminderDiscoveryUiState,
    onClick: () -> Unit
) {
    Card(
        backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.12f),
        elevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = state.headline,
                style = MaterialTheme.typography.subtitle1
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = state.supportingText,
                style = MaterialTheme.typography.body2
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = state.ctaLabel,
                style = MaterialTheme.typography.subtitle2,
                color = MaterialTheme.colors.primary,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun InfoCardSection(
    summary: List<SummaryItem>,
    onGrossCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = {
            items(
                items = summary,
                key = { it.title }
            ) { mData ->
                InfoCard(
                    summaryItem = mData,
                    onClick = if (mData.isInteractive) onGrossCardClick else null
                )
            }
        }
    )
}

@Composable
fun CardList(state: List<TransactionItem>, onItemClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        state.forEach { mData ->
            Transaction(mTransaction = mData, onClick = { onItemClick(mData.id) })
        }
    }
}

@Preview(showBackground = true, name = "Default View")
@Composable
fun PreviewHomeScreenContent_Default() {
    val bannerState = rememberInlineAdaptiveBannerAdState("preview_home_default_inline")
    MaterialTheme {
        HomeScreenContent(
            state = dummyState.copy(isRefreshing = false, isSearchActive = false),
            bannerState = bannerState,
            onRefresh = {},
            onSearchQueryChanged = {},
            onToggleSearch = {},
            onChangeSortOrder = {},
            onOrderCardClick = {},
            onTodayActivityClick = {},
            onGrossCardClick = {},
            onReminderDiscoveryClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Search Active View - Dark Theme")
@Composable
fun PreviewHomeScreenContent_SearchActiveDark() {
    val bannerState = rememberInlineAdaptiveBannerAdState("preview_home_dark_inline")
    MaterialTheme(colors = MaterialTheme.colors.copy(isLight = false)) { // Force dark theme for preview
        HomeScreenContent(
            state = dummyState.copy(isRefreshing = false, isSearchActive = true, searchQuery = "Test Query"),
            bannerState = bannerState,
            onRefresh = {},
            onSearchQueryChanged = {},
            onToggleSearch = {},
            onChangeSortOrder = {},
            onOrderCardClick = {},
            onTodayActivityClick = {},
            onGrossCardClick = {},
            onReminderDiscoveryClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Search Active View - Light Theme")
@Composable
fun PreviewHomeScreenContent_SearchActiveLight() {
    val bannerState = rememberInlineAdaptiveBannerAdState("preview_home_light_inline")
    MaterialTheme(colors = MaterialTheme.colors.copy(isLight = true)) { // Force light theme for preview
        HomeScreenContent(
            state = dummyState.copy(isRefreshing = false, isSearchActive = true, searchQuery = "Test Query"),
            bannerState = bannerState,
            onRefresh = {},
            onSearchQueryChanged = {},
            onToggleSearch = {},
            onChangeSortOrder = {},
            onOrderCardClick = {},
            onTodayActivityClick = {},
            onGrossCardClick = {},
            onReminderDiscoveryClick = {}
        )
    }
}
