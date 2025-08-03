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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.ui.common.dummy.dummyState
import com.raylabs.laundryhub.ui.component.InfoCard
import com.raylabs.laundryhub.ui.component.OrderStatusCard
import com.raylabs.laundryhub.ui.component.SectionOrLoading
import com.raylabs.laundryhub.ui.component.Transaction
import com.raylabs.laundryhub.ui.home.state.HomeUiState
import com.raylabs.laundryhub.ui.home.state.SummaryItem
import com.raylabs.laundryhub.ui.home.state.TransactionItem

@Composable
fun HomeScreen(viewModel: HomeViewModel, onOrderCardClick: (String) -> Unit) {
    val state by viewModel.uiState.collectAsState()
    HomeScreenContent(state = state, onOrderCardClick = onOrderCardClick)
}

@Composable
fun HomeScreenContent(state: HomeUiState, onOrderCardClick: (String) -> Unit) {
    val snackBarHostState = remember { SnackbarHostState() }

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
                GreetingWithImageBackground(username = state.user.data?.displayName ?: "Guest")

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
                text = "Today Activity",
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
                            text = "No transactions today.",
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    } else {
                        CardList(list)
                    }
                }
            )
        }

        // Spacer to create space between sections
        item { Spacer(Modifier.height(24.dp)) }

        // Pending Orders Section
        item {
            Text(
                text = "Pending Orders",
                style = MaterialTheme.typography.h6,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            )
        }

        item {
            if (state.unpaidOrder.data.isNullOrEmpty()) {
                Text(
                    text = "Belum ada data",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                val chunked = state.unpaidOrder.data.chunked(2)
                val detailOrder = state.detailOrder.data

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
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GreetingWithImageBackground(username: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        AsyncImage(
            model = "https://picsum.photos/id/${(1..999).random()}/800/300",
            contentDescription = null,
            placeholder = painterResource(R.drawable.gradient_img),
            alpha = 0.5f,
            error = painterResource(R.drawable.gradient_img),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Column(modifier = Modifier.padding(vertical = 30.dp, horizontal = 16.dp)) {
            Text("Hello,", style = MaterialTheme.typography.body1)
            Text(username, style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
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
fun CardList(state: List<TransactionItem>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        state.forEach { mData ->
            Transaction(mTransaction = mData)
        }
    }
}

@Preview
@Composable
fun PreviewHomeScreen() {
    HomeScreenContent(dummyState, onOrderCardClick = {})
}
