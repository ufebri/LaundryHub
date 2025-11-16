package com.raylabs.laundryhub.ui.outcome

import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadOutcomeTransactionUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.SubmitOutcomeUseCase
import com.raylabs.laundryhub.ui.common.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class OutcomeViewModelTest {

    private val readUseCase: ReadOutcomeTransactionUseCase = mock()
    private val submitUseCase: SubmitOutcomeUseCase = mock()
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `refreshHistory success updates state`() = runTest {
        whenever(readUseCase()).thenReturn(
            Resource.Success(
                listOf(OutcomeData("1", "01/01/2025", "Gas", "Rp10.000", "", "cash"))
            )
        )
        val viewModel = OutcomeViewModel(readUseCase, submitUseCase)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.history.data?.isNotEmpty() == true)
        assertEquals("2", state.form.id)
    }

    @Test
    fun `refreshHistory error sets error message`() = runTest {
        whenever(readUseCase()).thenReturn(Resource.Error("failed"))
        val viewModel = OutcomeViewModel(readUseCase, submitUseCase)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("failed", state.history.errorMessage)
    }

    @Test
    fun `submitOutcome success resets form and hides sheet`() = runTest {
        whenever(readUseCase()).thenReturn(Resource.Success(emptyList()), Resource.Success(emptyList()))
        whenever(submitUseCase(any())).thenReturn(Resource.Success(true))
        val viewModel = OutcomeViewModel(readUseCase, submitUseCase)
        advanceUntilIdle()

        viewModel.onPurposeChanged("Gas")
        viewModel.onPriceChanged("20000")
        viewModel.onRemarkChanged("note")
        viewModel.showAddSheet()

        viewModel.submitOutcome()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.form.isSubmitting)
        assertFalse(state.isAddSheetVisible)
        assertEquals("Outcome #1 added", state.snackbarMessage)
        verify(submitUseCase).invoke(any())
    }

    @Test
    fun `submitOutcome error shows snackbar and keeps sheet`() = runTest {
        whenever(readUseCase()).thenReturn(Resource.Success(emptyList()))
        whenever(submitUseCase(any())).thenReturn(Resource.Error("failed submit"))
        val viewModel = OutcomeViewModel(readUseCase, submitUseCase)
        advanceUntilIdle()

        viewModel.onPurposeChanged("Gas")
        viewModel.onPriceChanged("20000")
        viewModel.submitOutcome()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.form.isSubmitting)
        assertEquals("failed submit", state.snackbarMessage)
    }
}
