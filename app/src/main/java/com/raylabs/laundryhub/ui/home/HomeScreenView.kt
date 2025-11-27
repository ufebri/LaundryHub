package com.raylabs.laundryhub.ui.home

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.ui.common.dummy.home.dummyState
import com.raylabs.laundryhub.ui.component.GreetingWithImageBackground
import com.raylabs.laundryhub.ui.component.InfoCard
import com.raylabs.laundryhub.ui.component.OrderStatusCard
import com.raylabs.laundryhub.ui.component.SectionOrLoading
import com.raylabs.laundryhub.ui.component.Transaction
import com.raylabs.laundryhub.ui.home.state.HomeUiState
import com.raylabs.laundryhub.ui.home.state.SortOption
import com.raylabs.laundryhub.ui.home.state.SummaryItem
import com.raylabs.laundryhub.ui.home.state.TransactionItem

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onOrderCardClick: (String) -> Unit,
    onTodayActivityClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    HomeScreenContent(
        state = state,
        viewModel = viewModel,
        onOrderCardClick = onOrderCardClick,
        onTodayActivityClick = onTodayActivityClick
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomeScreenContent(
    state: HomeUiState,
    viewModel: HomeViewModel,
    onOrderCardClick: (String) -> Unit,
    onTodayActivityClick: (String) -> Unit
) {
    val snackBarHostState = remember { SnackbarHostState() }
    var showMenu by remember { mutableStateOf(false) }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isRefreshing,
        onRefresh = { viewModel.refreshAllData() }
    )

    LaunchedEffect(state.orderUpdateKey, state.user.errorMessage, state.todayIncome.errorMessage, state.summary.errorMessage, state.unpaidOrder.errorMessage) {
        listOf(state.user, state.todayIncome, state.summary, state.unpaidOrder).forEach {
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
                        username = state.user.data?.displayName ?: stringResource(R.string.guest)
                    )

                    SectionOrLoading(
                        isLoading = state.summary.isLoading,
                        error = state.summary.errorMessage,
                        content = {
                            InfoCardSection(
                                summary = state.summary.data.orEmpty(),
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
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            modifier = Modifier.weight(1f),
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
                                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
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
                        IconButton(onClick = { viewModel.toggleSearch() }) { 
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
                        IconButton(onClick = { viewModel.toggleSearch() }) {
                            Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.open_search_view))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.List,
                                contentDescription = stringResource(R.string.sort_orders)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(onClick = {
                                viewModel.changeSortOrder(SortOption.DUE_DATE_ASC)
                                showMenu = false
                            }) {
                                Text(stringResource(R.string.due_date_earliest))
                            }
                            DropdownMenuItem(onClick = {
                                viewModel.changeSortOrder(SortOption.DUE_DATE_DESC)
                                showMenu = false
                            }) {
                                Text(stringResource(R.string.due_date_latest))
                            }
                            DropdownMenuItem(onClick = {
                                viewModel.changeSortOrder(SortOption.ORDER_DATE_ASC)
                                showMenu = false
                            }) {
                                Text(stringResource(R.string.order_date_oldest))
                            }
                            DropdownMenuItem(onClick = {
                                viewModel.changeSortOrder(SortOption.ORDER_DATE_DESC)
                                showMenu = false
                            }) {
                                Text(stringResource(R.string.order_date_newest))
                            }
                        }
                    }
                }
            }

            // Pending Orders List or Loading Indicator
            item {
                if (state.unpaidOrder.isLoading && !state.isRefreshing) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (!state.isRefreshing) {
                    val ordersToDisplay = state.unpaidOrder.data
                    if (ordersToDisplay.isNullOrEmpty()) {
                        Text(
                            text = if (state.isSearchActive && state.searchQuery.isNotEmpty()) stringResource(R.string.no_search_results, state.searchQuery)
                                   else stringResource(R.string.no_data),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    } else {
                        val chunked = ordersToDisplay.chunked(2)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            chunked.forEach { rowItems ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowItems.forEach { item ->
                                        OrderStatusCard(
                                            item = item, onClick = { onOrderCardClick(item.orderID) },
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
            }
        } 

        PullRefreshIndicator(
            refreshing = state.isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
fun InfoCardSection(summary: List<SummaryItem>, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = {
            items(summary) { mData ->
                InfoCard(summaryItem = mData)
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
    MaterialTheme {
        HomeScreenContent(
            state = dummyState.copy(isRefreshing = false, isSearchActive = false),
            viewModel = hiltViewModel(), 
            onOrderCardClick = {},
            onTodayActivityClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Search Active View - Dark Theme")
@Composable
fun PreviewHomeScreenContent_SearchActiveDark() {
    MaterialTheme(colors = MaterialTheme.colors.copy(isLight = false)) { // Force dark theme for preview
        HomeScreenContent(
            state = dummyState.copy(isRefreshing = false, isSearchActive = true, searchQuery = "Test Query"),
            viewModel = hiltViewModel(), 
            onOrderCardClick = {},
            onTodayActivityClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Search Active View - Light Theme")
@Composable
fun PreviewHomeScreenContent_SearchActiveLight() {
    MaterialTheme(colors = MaterialTheme.colors.copy(isLight = true)) { // Force light theme for preview
        HomeScreenContent(
            state = dummyState.copy(isRefreshing = false, isSearchActive = true, searchQuery = "Test Query"),
            viewModel = hiltViewModel(), 
            onOrderCardClick = {},
            onTodayActivityClick = {}
        )
    }
}
