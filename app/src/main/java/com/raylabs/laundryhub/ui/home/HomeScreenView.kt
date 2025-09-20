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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
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

@Composable
fun HomeScreenContent(
    state: HomeUiState,
    viewModel: HomeViewModel,
    onOrderCardClick: (String) -> Unit,
    onTodayActivityClick: (String) -> Unit
) {
    val snackBarHostState = remember { SnackbarHostState() }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(state.orderUpdateKey) {
        listOf(state.user, state.todayIncome, state.summary, state.unpaidOrder).forEach {
            it.errorMessage?.let { msg -> snackBarHostState.showSnackbar(msg) }
        }
    }

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

        // Spacer to create space between header and next section
        item { Spacer(Modifier.height(120.dp)) }

        // Today Activity Section
        item {
            Text(
                text = stringResource(R.string.today_activity),
                style = MaterialTheme.typography.h6,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            )
        }

        // Loading or Error Section for Today's Income
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

        // Spacer to create space between sections
        item { Spacer(Modifier.height(24.dp)) }

        // Pending Orders Section Title
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.pending_orders),
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(
                            imageVector = Icons.Filled.List,
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
            if (state.unpaidOrder.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val ordersToDisplay = state.unpaidOrder.data
                if (ordersToDisplay.isNullOrEmpty()) {
                    Text(
                        text = stringResource(R.string.no_data),
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

@Preview
@Composable
fun PreviewHomeScreen() {
    HomeScreenContent(
        state = dummyState, 
        viewModel = hiltViewModel(),
        onOrderCardClick = {},
        onTodayActivityClick = {}
    )
}
