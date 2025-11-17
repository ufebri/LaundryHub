package com.raylabs.laundryhub.ui.outcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.ui.common.dummy.outcome.dummyOutcomeUiState
import com.raylabs.laundryhub.ui.component.DefaultTopAppBar
import com.raylabs.laundryhub.ui.outcome.component.AddOutcomeSheet
import com.raylabs.laundryhub.ui.outcome.component.OutcomeDateHeader
import com.raylabs.laundryhub.ui.outcome.component.OutcomeHistoryCard
import com.raylabs.laundryhub.ui.outcome.state.OutcomeUiItem
import com.raylabs.laundryhub.ui.outcome.state.OutcomeUiState
import com.raylabs.laundryhub.ui.theme.LaundryHubTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutcomeScreen(
    viewModel: OutcomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissSnackbar()
        }
    }

    val shouldShowBottomSheet =
        uiState.isAddSheetVisible || sheetState.currentValue != SheetValue.Hidden

    if (shouldShowBottomSheet) {
        LaunchedEffect(uiState.isAddSheetVisible) {
            if (uiState.isAddSheetVisible) {
                sheetState.show()
            } else if (sheetState.currentValue != SheetValue.Hidden) {
                sheetState.hide()
            }
        }

        ModalBottomSheet(
            onDismissRequest = { viewModel.hideAddSheet() },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface,
            scrimColor = Color.Transparent
        ) {
            AddOutcomeSheet(
                formState = uiState.form,
                onDateSelected = viewModel::onDateSelected,
                onPurposeChanged = viewModel::onPurposeChanged,
                onPriceChanged = viewModel::onPriceChanged,
                onPaymentSelected = viewModel::onPaymentSelected,
                onRemarkChanged = viewModel::onRemarkChanged,
                onSubmit = viewModel::submitOutcome
            )
        }
    }

    Scaffold(
        topBar = { DefaultTopAppBar(title = stringResource(id = R.string.outcome_screen_title)) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::showAddSheet,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.outcome_screen_add_button)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        OutcomeContent(
            state = uiState,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
fun OutcomeContent(
    state: OutcomeUiState,
    modifier: Modifier = Modifier
) {
    val displayState = state.history.toDisplayState()
    val emptyMessage = stringResource(id = R.string.outcome_history_empty)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        when (displayState) {
            OutcomeHistoryDisplayState.Loading -> loadingStateItem()
            is OutcomeHistoryDisplayState.Error -> errorStateItem(displayState.message)
            OutcomeHistoryDisplayState.Empty -> emptyStateItem(emptyMessage)
            is OutcomeHistoryDisplayState.Populated -> outcomeHistoryItems(displayState.items)
        }
    }
}

private fun LazyListScope.loadingStateItem() {
    item {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun LazyListScope.errorStateItem(message: String) {
    item {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

private fun LazyListScope.emptyStateItem(message: String) {
    item {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

private fun LazyListScope.outcomeHistoryItems(items: List<OutcomeUiItem>) {
    items(items) { item ->
        when (item) {
            is OutcomeUiItem.Header -> OutcomeDateHeader(dateLabel = item.dateLabel)
            is OutcomeUiItem.Entry -> OutcomeHistoryCard(
                item = item.item,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewOutcomeScreen() {
    LaundryHubTheme {
        OutcomeContent(state = dummyOutcomeUiState)
    }
}
