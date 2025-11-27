package com.raylabs.laundryhub.ui.outcome

import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.core.domain.usecase.sheets.outcome.GetLastOutcomeIdUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.outcome.GetOutcomeUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.outcome.ReadOutcomeTransactionUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.outcome.SubmitOutcomeUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.outcome.UpdateOutcomeUseCase
import com.raylabs.laundryhub.ui.common.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class OutcomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val mockReadOutcome: ReadOutcomeTransactionUseCase = mock()
    private val mockSubmitOutcome: SubmitOutcomeUseCase = mock()
    private val mockGetLastOutcomeId: GetLastOutcomeIdUseCase = mock()
    private val mockUpdateOutcome: UpdateOutcomeUseCase = mock()
    private val mockGetOutcome: GetOutcomeUseCase = mock()

    private val sampleOutcome = OutcomeData(
        id = "1",
        date = "01/01/2025",
        purpose = "Test",
        price = "1000",
        remark = "-",
        payment = "cash"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        runTest {
            whenever(mockReadOutcome.invoke(onRetry = anyOrNull())).thenReturn(Resource.Success(emptyList()))
            whenever(mockGetLastOutcomeId.invoke(onRetry = anyOrNull())).thenReturn(Resource.Success("1"))
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init fetches outcome list and last id`() = runTest {
        val vm = OutcomeViewModel(
            readOutcomeUseCase = mockReadOutcome,
            submitOutcomeUseCase = mockSubmitOutcome,
            getLastOutcomeIdUseCase = mockGetLastOutcomeId,
            updateOutcomeUseCase = mockUpdateOutcome,
            getOutcomeUseCase = mockGetOutcome
        )

        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState
        assertEquals("1", state.lastOutcomeId)
        assertFalse(state.outcome.isLoading)
        assertNotNull(state.outcome.data)
    }

    @Test
    fun `refreshOutcomeList triggers fetch outcome and last id`() = runTest {
        whenever(mockGetLastOutcomeId.invoke(onRetry = anyOrNull())).thenReturn(Resource.Success("5"))
        val vm = OutcomeViewModel(
            readOutcomeUseCase = mockReadOutcome,
            submitOutcomeUseCase = mockSubmitOutcome,
            getLastOutcomeIdUseCase = mockGetLastOutcomeId,
            updateOutcomeUseCase = mockUpdateOutcome,
            getOutcomeUseCase = mockGetOutcome
        )
        dispatcher.scheduler.advanceUntilIdle()

        vm.refreshOutcomeList()
        dispatcher.scheduler.advanceUntilIdle()

        verify(mockReadOutcome, atLeastOnce()).invoke(onRetry = anyOrNull())
        verify(mockGetLastOutcomeId, atLeastOnce()).invoke(onRetry = anyOrNull())
        assertEquals("5", vm.uiState.lastOutcomeId)
    }

    @Test
    fun `onOutcomeEditClick loads outcome and updates state`() = runTest {
        whenever(mockGetOutcome.invoke(onRetry = anyOrNull(), outcomeID = any()))
            .thenReturn(Resource.Success(sampleOutcome))
        val vm = OutcomeViewModel(
            readOutcomeUseCase = mockReadOutcome,
            submitOutcomeUseCase = mockSubmitOutcome,
            getLastOutcomeIdUseCase = mockGetLastOutcomeId,
            updateOutcomeUseCase = mockUpdateOutcome,
            getOutcomeUseCase = mockGetOutcome
        )
        dispatcher.scheduler.advanceUntilIdle()

        val success = vm.onOutcomeEditClick("1")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
        assertTrue(vm.uiState.isEditMode)
        assertEquals("Test", vm.uiState.name)
        assertEquals("Paid by Cash", vm.uiState.paymentStatus)
    }

    @Test
    fun `buildOutcomeDataForSubmit returns null when last id invalid`() = runTest {
        whenever(mockGetLastOutcomeId.invoke(onRetry = anyOrNull())).thenReturn(Resource.Success("abc"))
        val vm = OutcomeViewModel(
            readOutcomeUseCase = mockReadOutcome,
            submitOutcomeUseCase = mockSubmitOutcome,
            getLastOutcomeIdUseCase = mockGetLastOutcomeId,
            updateOutcomeUseCase = mockUpdateOutcome,
            getOutcomeUseCase = mockGetOutcome
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.buildOutcomeDataForSubmit() == null)
    }

    @Test
    fun `submitOutcome resets submitting flag on success`() = runTest {
        whenever(mockSubmitOutcome.invoke(onRetry = anyOrNull(), order = any()))
            .thenReturn(Resource.Success(true))
        val vm = OutcomeViewModel(
            readOutcomeUseCase = mockReadOutcome,
            submitOutcomeUseCase = mockSubmitOutcome,
            getLastOutcomeIdUseCase = mockGetLastOutcomeId,
            updateOutcomeUseCase = mockUpdateOutcome,
            getOutcomeUseCase = mockGetOutcome
        )
        dispatcher.scheduler.advanceUntilIdle()

        vm.onPurposeChanged("Test")
        vm.onPriceChanged("1000")
        vm.onPaymentMethodSelected("Paid by Cash")
        vm.onRemarkChanged("-")
        vm.prepareNewOutcome()

        val data = vm.buildOutcomeDataForSubmit()
        assertNotNull(data)

        var completed = false
        vm.submitOutcome(data!!) { completed = true }
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.isSubmitting)
        assertTrue(completed)
    }
}
