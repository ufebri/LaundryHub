package com.raylabs.laundryhub.ui.outcome

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarHost
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.component.DateHeader
import com.raylabs.laundryhub.ui.component.DefaultTopAppBar
import com.raylabs.laundryhub.ui.component.EntryItemCard
import com.raylabs.laundryhub.ui.component.OutcomeBottomSheet
import com.raylabs.laundryhub.ui.component.SectionOrLoading
import com.raylabs.laundryhub.ui.outcome.state.DateListItemUI
import com.raylabs.laundryhub.ui.outcome.state.EntryItem
import com.raylabs.laundryhub.ui.outcome.state.OutcomeUiState
import com.raylabs.laundryhub.ui.outcome.state.TypeCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun OutcomeScreenView(viewModel: OutcomeViewModel = hiltViewModel()) {
    val state = viewModel.uiState
    val scaffoldState = rememberScaffoldState()
    val bottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )
    val coroutineScope = rememberCoroutineScope()

    fun hideSheet() {
        coroutineScope.launch {
            bottomSheetState.hide()
            viewModel.resetForm()
        }
    }

    ModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetBackgroundColor = MaterialTheme.colors.surface,
        sheetContent = {
            OutcomeBottomSheet(
                state = state,
                onPurposeChanged = { viewModel.onPurposeChanged(it) },
                onPriceChanged = { viewModel.onPriceChanged(it) },
                onPaymentMethodSelected = { viewModel.onPaymentMethodSelected(it) },
                onDateSelected = { viewModel.onDateChanged(it) },
                onRemarkChanged = { viewModel.onRemarkChanged(it) },
                onSubmit = {
                    val outcomeData = viewModel.buildOutcomeDataForSubmit()
                    if (outcomeData == null) {
                        coroutineScope.launch {
                            scaffoldState.snackbarHostState.showSnackbar("Outcome ID unavailable. Try again.")
                        }
                        return@OutcomeBottomSheet
                    }

                    coroutineScope.launch {
                        viewModel.submitOutcome(outcomeData) {
                            hideSheet()
                            scaffoldState.snackbarHostState.showSnackbar("Outcome #${outcomeData.id} submitted")
                        }
                    }
                },
                onUpdate = {
                    val outcomeData = viewModel.buildOutcomeDataForUpdate()
                    if (outcomeData == null) {
                        coroutineScope.launch {
                            scaffoldState.snackbarHostState.showSnackbar("Outcome ID unavailable. Try again.")
                        }
                        return@OutcomeBottomSheet
                    }

                    coroutineScope.launch {
                        viewModel.updateOutcome(outcomeData) {
                            hideSheet()
                            scaffoldState.snackbarHostState.showSnackbar("Outcome #${outcomeData.id} updated")
                        }
                    }
                }
            )
        }
    ) {
        Scaffold(
            scaffoldState = scaffoldState,
            topBar = { DefaultTopAppBar(title = stringResource(R.string.outcome)) },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        viewModel.prepareNewOutcome()
                        coroutineScope.launch { bottomSheetState.show() }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = stringResource(R.string.add_outcome)
                    )
                }
            },
            snackbarHost = { SnackbarHost(hostState = scaffoldState.snackbarHostState) }
        ) { paddingValues ->
            OutcomeContent(
                state = state,
                scaffoldState = scaffoldState,
                modifier = Modifier.padding(paddingValues),
                onEntryClick = { entry ->
                    coroutineScope.launch {
                        val success = viewModel.onOutcomeEditClick(entry.id)
                        if (success) {
                            bottomSheetState.show()
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun OutcomeContent(
    state: OutcomeUiState,
    scaffoldState: ScaffoldState,
    modifier: Modifier = Modifier,
    onEntryClick: (EntryItem) -> Unit = {}
) {
    LaunchedEffect(state.outcome.errorMessage) {
        state.outcome.errorMessage?.let { msg ->
            scaffoldState.snackbarHostState.showSnackbar(msg)
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
                            EntryItemCard(
                                item = it.item,
                                onClick = { onEntryClick(it.item) }
                            )
                        }
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Preview
@Composable
fun PreviewOutcomeScreen() {
    val previewState = OutcomeUiState(
        outcome = SectionState(
            data = listOf(
                DateListItemUI.Header("01/07/2025"),
                DateListItemUI.Entry(
                    EntryItem(
                        id = "O1",
                        name = "Groceries",
                        date = "01/07/2025",
                        price = "50.000",
                        remark = "Soap & detergen",
                        paymentStatus = "Paid",
                        typeCard = TypeCard.OUTCOME
                    )
                )
            )
        )
    )

    val scaffoldState = rememberScaffoldState()
    val bottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )
    val scope = rememberCoroutineScope()

    ModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetBackgroundColor = MaterialTheme.colors.surface,
        sheetContent = {
            OutcomeBottomSheet(state = previewState)
        }
    ) {
        Scaffold(
            scaffoldState = scaffoldState,
            topBar = { DefaultTopAppBar(title = "Outcome Preview") },
            floatingActionButton = {
                FloatingActionButton(onClick = { scope.launch { bottomSheetState.show() } }) {
                    Icon(imageVector = Icons.Default.AddCircle, contentDescription = null)
                }
            },
            snackbarHost = { SnackbarHost(hostState = scaffoldState.snackbarHostState) }
        ) { paddingValues ->
            OutcomeContent(
                state = previewState,
                scaffoldState = scaffoldState,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}
