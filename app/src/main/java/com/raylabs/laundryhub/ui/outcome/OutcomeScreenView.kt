package com.raylabs.laundryhub.ui.outcome

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.ui.component.DateHeader
import com.raylabs.laundryhub.ui.component.DefaultTopAppBar
import com.raylabs.laundryhub.ui.component.EntryItemCard
import com.raylabs.laundryhub.ui.component.SectionOrLoading
import com.raylabs.laundryhub.ui.outcome.state.DateListItemUI
import com.raylabs.laundryhub.ui.outcome.state.OutcomeUiState

@Composable
fun OutcomeScreenView(viewModel: OutcomeViewModel = hiltViewModel()) {
    val state = viewModel.uiState

    Scaffold(
        topBar = { DefaultTopAppBar(title = stringResource(R.string.outcome)) }
    ) { paddingValues ->
        OutcomeContent(state, modifier = Modifier.padding(paddingValues))
    }
}

@Composable
fun OutcomeContent(state: OutcomeUiState, modifier: Modifier) {
    val snackBarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state) {
        listOf(state.outcome).forEach {
            it.errorMessage?.let { msg ->
                snackBarHostState.showSnackbar(msg)
            }
        }
    }

    SectionOrLoading(
        isLoading = state.outcome.isLoading,
        error = state.outcome.errorMessage,
        content = {
            LazyColumn(modifier = modifier.fillMaxSize()) {
                items(
                    items = state.outcome.data.orEmpty(),
                    key = { item ->
                        when (item) {
                            is DateListItemUI.Header -> "header_${item.date}"
                            is DateListItemUI.Entry -> "entry_${item.item.id}"
                        }
                    }
                ) {
                    when (it) {
                        is DateListItemUI.Header -> {
                            DateHeader(it.date)
                        }


                        is DateListItemUI.Entry -> {
                            EntryItemCard(it.item)
                        }
                    }
                }
            }
        }
    )
}

@Preview
@Composable
fun PreviewOutcomeScreen() {
    OutcomeScreenView()
}