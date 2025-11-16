package com.raylabs.laundryhub.ui.outcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.raylabs.laundryhub.ui.common.dummy.outcome.dummyOutcomeUiState
import com.raylabs.laundryhub.ui.component.DefaultTopAppBar
import com.raylabs.laundryhub.ui.outcome.component.AddOutcomeSheet
import com.raylabs.laundryhub.ui.outcome.component.OutcomeDateHeader
import com.raylabs.laundryhub.ui.outcome.component.OutcomeHistoryCard
import com.raylabs.laundryhub.ui.outcome.state.OutcomeUiItem
import com.raylabs.laundryhub.ui.outcome.state.OutcomeUiState

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun OutcomeScreen(
    viewModel: OutcomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissSnackbar()
        }
    }

    LaunchedEffect(uiState.isAddSheetVisible) {
        if (uiState.isAddSheetVisible) {
            sheetState.show()
        } else {
            sheetState.hide()
        }
    }

    LaunchedEffect(sheetState.currentValue) {
        if (sheetState.currentValue == ModalBottomSheetValue.Hidden && uiState.isAddSheetVisible) {
            viewModel.hideAddSheet()
        }
    }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        scrimColor = Color.Transparent,
        sheetContent = {
            if (uiState.isAddSheetVisible) {
                AddOutcomeSheet(
                    formState = uiState.form,
                    onDateSelected = { viewModel.onDateSelected(it) },
                    onPurposeChanged = viewModel::onPurposeChanged,
                    onPriceChanged = viewModel::onPriceChanged,
                    onPaymentSelected = viewModel::onPaymentSelected,
                    onRemarkChanged = viewModel::onRemarkChanged,
                    onSubmit = { viewModel.submitOutcome() }
                )
            } else {
                Spacer(modifier = Modifier.height(1.dp))
            }
        }
    ) {
        Scaffold(
            topBar = { DefaultTopAppBar(title = "Outcome") },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        viewModel.showAddSheet()
                    },
                    backgroundColor = Color(0xFF5B3E9E),
                    contentColor = Color.White
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Outcome")
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
}

@Composable
fun OutcomeContent(
    state: OutcomeUiState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7F5FA)),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        when {
            state.history.isLoading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            state.history.errorMessage != null -> {
                item {
                    Text(
                        text = state.history.errorMessage,
                        color = Color.Red,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }

            state.history.data.isNullOrEmpty() -> {
                item {
                    Text(
                        text = "Outcome history is empty.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        color = Color.Gray
                    )
                }
            }

            else -> {
                items(state.history.data) { item ->
                    when (item) {
                        is OutcomeUiItem.Header -> OutcomeDateHeader(
                            dateLabel = item.dateLabel
                        )

                        is OutcomeUiItem.Entry -> OutcomeHistoryCard(
                            item = item.item,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewOutcomeScreen() {
    OutcomeContent(state = dummyOutcomeUiState)
}
