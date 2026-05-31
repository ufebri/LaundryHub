package com.raylabs.laundryhub.ui.outcome

import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.core.domain.model.sheets.getPaymentValueFromDescription
import com.raylabs.laundryhub.core.domain.usecase.sheets.outcome.DeleteOutcomeUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.outcome.GetLastOutcomeIdUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.outcome.GetOutcomeUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.outcome.ReadOutcomeTransactionUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.outcome.SubmitOutcomeUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.outcome.UpdateOutcomeUseCase
import com.raylabs.laundryhub.shared.util.Resource
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
import org.mockito.kotlin.eq
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
    private val mockDeleteOutcome: DeleteOutcomeUseCase = mock()

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
            whenever(mockReadOutcome.invoke(onRetry = anyOrNull()))
                .thenReturn(Resource.Success(emptyList()))
            whenever(mockGetLastOutcomeId.invoke(onRetry = anyOrNull()))
                .thenReturn(Resource.Success("1"))
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init fetches outcome list and last id`() = runTest {
        val vm = createViewModel()

        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState
        assertEquals("1", state.lastOutcomeId)
        assertFalse(state.outcome.isLoading)
        assertNotNull(state.outcome.data)
    }

    @Test
    fun `refreshOutcomeList triggers fetch outcome and last id`() = runTest {
        whenever(mockGetLastOutcomeId.invoke(onRetry = anyOrNull()))
            .thenReturn(Resource.Success("5"))
        val vm = createViewModel()
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
        val vm = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        val success = vm.onOutcomeEditClick("1")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
        assertTrue(vm.uiState.isEditMode)
        assertEquals("Test", vm.uiState.name)
        assertEquals("Paid by Cash", vm.uiState.paymentStatus)
    }

    @Test
    fun `buildOutcomeDataForSubmit leaves id blank for backend allocation`() = runTest {
        whenever(mockGetLastOutcomeId.invoke(onRetry = anyOrNull()))
            .thenReturn(Resource.Success("abc"))
        val vm = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        val data = vm.buildOutcomeDataForSubmit()

        assertNotNull(data)
        assertEquals("", data.id)
    }

    @Test
    fun `submitOutcome resets submitting flag on success`() = runTest {
        whenever(mockSubmitOutcome.invoke(onRetry = anyOrNull(), order = any()))
            .thenReturn(Resource.Success("42"))
        val vm = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onPurposeChanged("Test")
        vm.onPriceChanged("1000")
        vm.onPaymentMethodSelected("Paid by Cash")
        vm.onRemarkChanged("-")
        vm.prepareNewOutcome()

        val data = vm.buildOutcomeDataForSubmit()
        assertNotNull(data)

        var completedId: String? = null
        vm.submitOutcome(data) { completedId = it }
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.isSubmitting)
        assertEquals("42", completedId)
        assertEquals("42", vm.uiState.submitNewOutcome.data)
    }

    @Test
    fun `submitOutcome sets error state on failure`() = runTest {
        whenever(mockSubmitOutcome.invoke(onRetry = anyOrNull(), order = any()))
            .thenReturn(Resource.Error("fail"))

        val vm = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onPurposeChanged("Test")
        vm.onPriceChanged("1,000")
        vm.onPaymentMethodSelected("Paid by Cash")
        vm.onRemarkChanged("-")

        val data = vm.buildOutcomeDataForSubmit()
        assertNotNull(data)

        vm.submitOutcome(data) {}
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.isSubmitting)
        assertEquals("fail", vm.uiState.submitNewOutcome.errorMessage)
    }

    @Test
    fun `updateOutcome clears submitting flag on success`() = runTest {
        whenever(mockUpdateOutcome.invoke(onRetry = anyOrNull(), order = any()))
            .thenReturn(Resource.Success(true))
        whenever(mockGetOutcome.invoke(onRetry = anyOrNull(), outcomeID = any()))
            .thenReturn(Resource.Success(sampleOutcome))

        val vm = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onOutcomeEditClick("1")
        var completed = false
        vm.updateOutcome(sampleOutcome) { completed = true }
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.isSubmitting)
        assertTrue(completed)
        assertTrue(vm.uiState.updateOutcome.data == true)
    }

    @Test
    fun `onOutcomeEditClick returns false and sets error when use case fails`() = runTest {
        whenever(mockGetOutcome.invoke(onRetry = anyOrNull(), outcomeID = any()))
            .thenReturn(Resource.Error("boom"))

        val vm = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        val result = vm.onOutcomeEditClick("1")
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(result)
        assertFalse(vm.uiState.isEditMode)
        assertEquals("boom", vm.uiState.editOutcome.errorMessage)
    }

    @Test
    fun `onOutcomeEditClick returns false and sets empty message when no data`() = runTest {
        whenever(mockGetOutcome.invoke(onRetry = anyOrNull(), outcomeID = any()))
            .thenReturn(Resource.Empty)

        val vm = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        val result = vm.onOutcomeEditClick("1")
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(result)
        assertFalse(vm.uiState.isEditMode)
        assertEquals("No data found for outcome ID: 1", vm.uiState.editOutcome.errorMessage)
    }

    @Test
    fun `buildOutcomeDataForSubmit returns data when last id valid`() = runTest {
        whenever(mockGetLastOutcomeId.invoke(onRetry = anyOrNull()))
            .thenReturn(Resource.Success("123"))
        val vm = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onPurposeChanged("Laundry")
        vm.onPriceChanged("1,000")
        vm.onPaymentMethodSelected("Paid by Cash")
        vm.onRemarkChanged("-")
        vm.onDateChanged("01/01/2025")

        val data = vm.buildOutcomeDataForSubmit()
        assertNotNull(data)
        assertEquals("", data.id)
        assertEquals("1000", data.price)
        assertEquals(getPaymentValueFromDescription("Paid by Cash"), data.payment)
    }

    @Test
    fun `buildOutcomeDataForUpdate returns data when outcomeID set`() = runTest {
        whenever(mockGetOutcome.invoke(onRetry = anyOrNull(), outcomeID = any()))
            .thenReturn(Resource.Success(sampleOutcome))
        val vm = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onOutcomeEditClick("1")
        vm.onDateChanged("02/02/2025")
        vm.onPurposeChanged("Update")
        vm.onPriceChanged("2000")
        vm.onPaymentMethodSelected("Paid by Cash")

        val data = vm.buildOutcomeDataForUpdate()
        assertNotNull(data)
        assertEquals(vm.uiState.outcomeID, data!!.id)
    }

    @Test
    fun `resetForm clears fields and edit mode`() = runTest {
        val vm = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onOutcomeEditClick("1")
        vm.onPurposeChanged("Change")
        vm.onPaymentMethodSelected("Paid by Cash")

        vm.resetForm()
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.isEditMode)
        assertEquals("", vm.uiState.name)
        assertEquals("", vm.uiState.paymentStatus)
    }

    @Test
    fun `fetchOutcomeList sets error when use case returns Error`() = runTest {
        whenever(mockReadOutcome.invoke(onRetry = anyOrNull()))
            .thenReturn(Resource.Error("fail"))

        val vm = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("fail", vm.uiState.outcome.errorMessage)
        assertFalse(vm.uiState.outcome.isLoading)
    }

    @Test
    fun `fetchOutcomeList sets error message when use case returns Empty`() = runTest {
        whenever(mockReadOutcome.invoke(onRetry = anyOrNull()))
            .thenReturn(Resource.Empty)

        val vm = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Data Kosong", vm.uiState.outcome.errorMessage)
        assertFalse(vm.uiState.outcome.isLoading)
    }

    @Test
    fun `fetchLastOutcomeId sets fallback text on error`() = runTest {
        whenever(mockGetLastOutcomeId.invoke(onRetry = anyOrNull()))
            .thenReturn(Resource.Error("boom"))

        val vm = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Error, try again", vm.uiState.lastOutcomeId)
    }

    @Test
    fun `buildOutcomeDataForUpdate returns null when outcomeID blank`() = runTest {
        val vm = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.resetForm()
        assertTrue(vm.buildOutcomeDataForUpdate() == null)
    }

    @Test
    fun `updateOutcome stops submitting flag on error`() = runTest {
        whenever(mockUpdateOutcome.invoke(onRetry = anyOrNull(), order = any()))
            .thenReturn(Resource.Error("fail"))

        val vm = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onOutcomeEditClick("1")
        vm.updateOutcome(sampleOutcome) {}
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.isSubmitting)
        assertEquals("fail", vm.uiState.updateOutcome.errorMessage)
    }

    @Test
    fun `deleteOutcome refreshes list and last id on success`() = runTest {
        whenever(mockDeleteOutcome.invoke(onRetry = anyOrNull(), outcomeId = eq("1")))
            .thenReturn(Resource.Success(true))

        val vm = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        var completed = false
        vm.deleteOutcome(outcomeId = "1", onComplete = { completed = true })
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(completed)
        assertEquals(true, vm.uiState.deleteOutcome.data)
        verify(mockReadOutcome, atLeastOnce()).invoke(onRetry = anyOrNull())
        verify(mockGetLastOutcomeId, atLeastOnce()).invoke(onRetry = anyOrNull())
    }

    @Test
    fun `deleteOutcome stores error and forwards callback when delete fails`() = runTest {
        whenever(mockDeleteOutcome.invoke(onRetry = anyOrNull(), outcomeId = eq("1")))
            .thenReturn(Resource.Error("delete fail"))

        val vm = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        var receivedError: String? = null
        vm.deleteOutcome(
            outcomeId = "1",
            onComplete = {},
            onError = { receivedError = it }
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("delete fail", receivedError)
        assertEquals("delete fail", vm.uiState.deleteOutcome.errorMessage)
    }

    @Test
    fun `submitOutcome adds optimistic item instantly and updates status on success`() = runTest {
        whenever(mockSubmitOutcome.invoke(onRetry = anyOrNull(), order = any()))
            .thenReturn(Resource.Success("real-id-99"))

        val vm = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onPurposeChanged("Beli Sabun")
        vm.onPriceChanged("15000")
        vm.onPaymentMethodSelected("Paid by Cash")
        vm.onRemarkChanged("Keperluan Toko")

        val data = vm.buildOutcomeDataForSubmit()
        
        vm.submitOutcome(data)
        // Verify optimistic item added immediately before advancing scheduler
        val optimisticListBefore = vm.uiState.optimisticOutcomes
        assertEquals(1, optimisticListBefore.size)
        val tempItem = optimisticListBefore.first()
        assertEquals(com.raylabs.laundryhub.ui.home.state.SyncStatus.PENDING, tempItem.syncStatus)
        assertEquals("Beli Sabun", tempItem.name)

        dispatcher.scheduler.advanceUntilIdle()

        // After success, it should be SYNCED and hold the real id
        val optimisticListAfter = vm.uiState.optimisticOutcomes
        assertEquals(1, optimisticListAfter.size)
        assertEquals(com.raylabs.laundryhub.ui.home.state.SyncStatus.SYNCED, optimisticListAfter.first().syncStatus)
        assertEquals("real-id-99", optimisticListAfter.first().id)
    }

    @Test
    fun `submitOutcome updates status to FAILED on error`() = runTest {
        whenever(mockSubmitOutcome.invoke(onRetry = anyOrNull(), order = any()))
            .thenReturn(Resource.Error("network timeout"))

        val vm = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        val data = vm.buildOutcomeDataForSubmit()
        vm.submitOutcome(data)
        
        dispatcher.scheduler.advanceUntilIdle()

        val optimisticList = vm.uiState.optimisticOutcomes
        assertEquals(1, optimisticList.size)
        assertEquals(com.raylabs.laundryhub.ui.home.state.SyncStatus.FAILED, optimisticList.first().syncStatus)
    }

    @Test
    fun `retryOptimisticOutcome triggers submit again`() = runTest {
        whenever(mockSubmitOutcome.invoke(onRetry = anyOrNull(), order = any()))
            .thenReturn(Resource.Error("failed first time"))

        val vm = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        val data = vm.buildOutcomeDataForSubmit()
        vm.submitOutcome(data)
        dispatcher.scheduler.advanceUntilIdle()

        // Setup mock to succeed on retry
        whenever(mockSubmitOutcome.invoke(onRetry = anyOrNull(), order = any()))
            .thenReturn(Resource.Success("retry-success-id"))

        val failedFakeId = vm.uiState.optimisticOutcomes.first().id
        vm.retryOptimisticOutcome(failedFakeId)

        // Instantly goes to PENDING on retry
        assertEquals(com.raylabs.laundryhub.ui.home.state.SyncStatus.PENDING, vm.uiState.optimisticOutcomes.first().syncStatus)

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(com.raylabs.laundryhub.ui.home.state.SyncStatus.SYNCED, vm.uiState.optimisticOutcomes.first().syncStatus)
        assertEquals("retry-success-id", vm.uiState.optimisticOutcomes.first().id)
    }

    @Test
    fun `removeOptimisticOutcome clears item from list`() = runTest {
        val vm = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        val data = vm.buildOutcomeDataForSubmit()
        vm.submitOutcome(data)
        val fakeId = vm.uiState.optimisticOutcomes.first().id

        vm.removeOptimisticOutcome(fakeId)
        assertTrue(vm.uiState.optimisticOutcomes.isEmpty())
    }

    @Test
    fun `updateOutcome adds to optimisticUpdates instantly and clears on success`() = runTest {
        whenever(mockUpdateOutcome.invoke(onRetry = anyOrNull(), order = any()))
            .thenReturn(Resource.Success(true))

        val vm = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.updateOutcome(sampleOutcome)
        
        // Instantly added with PENDING status
        val updateMapBefore = vm.uiState.optimisticUpdates
        assertEquals(1, updateMapBefore.size)
        assertEquals(com.raylabs.laundryhub.ui.home.state.SyncStatus.PENDING, updateMapBefore[sampleOutcome.id]?.syncStatus)

        dispatcher.scheduler.advanceUntilIdle()

        // Cleared on success
        assertTrue(vm.uiState.optimisticUpdates.isEmpty())
    }

    @Test
    fun `updateOutcome updates status to FAILED on error`() = runTest {
        whenever(mockUpdateOutcome.invoke(onRetry = anyOrNull(), order = any()))
            .thenReturn(Resource.Error("update error"))

        val vm = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.updateOutcome(sampleOutcome)
        dispatcher.scheduler.advanceUntilIdle()

        val updateMap = vm.uiState.optimisticUpdates
        assertEquals(1, updateMap.size)
        assertEquals(com.raylabs.laundryhub.ui.home.state.SyncStatus.FAILED, updateMap[sampleOutcome.id]?.syncStatus)
    }

    @Test
    fun `deleteOutcome hides item instantly and rolls back on failure`() = runTest {
        whenever(mockDeleteOutcome.invoke(onRetry = anyOrNull(), outcomeId = any()))
            .thenReturn(Resource.Error("delete error"))

        val vm = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.deleteOutcome(outcomeId = "test-delete-id")

        // Instantly hidden
        assertTrue("test-delete-id" in vm.uiState.hiddenOutcomeIds)

        dispatcher.scheduler.advanceUntilIdle()

        // Visual rollback on failure
        assertFalse("test-delete-id" in vm.uiState.hiddenOutcomeIds)
    }

    @Test
    fun `submitOutcome handles empty or unknown resource state`() = runTest {
        whenever(mockSubmitOutcome.invoke(onRetry = anyOrNull(), order = any()))
            .thenReturn(Resource.Empty)

        val vm = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        val data = vm.buildOutcomeDataForSubmit()
        var errorMsg: String? = null
        vm.submitOutcome(data, onError = { errorMsg = it })
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Unknown error", errorMsg)
        assertFalse(vm.uiState.isSubmitting)
        assertEquals(com.raylabs.laundryhub.ui.home.state.SyncStatus.FAILED, vm.uiState.optimisticOutcomes.first().syncStatus)
    }

    @Test
    fun `retryOptimisticOutcome handles empty or unknown resource state`() = runTest {
        whenever(mockSubmitOutcome.invoke(onRetry = anyOrNull(), order = any()))
            .thenReturn(Resource.Error("first-fail"))

        val vm = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        val data = vm.buildOutcomeDataForSubmit()
        vm.submitOutcome(data)
        dispatcher.scheduler.advanceUntilIdle()

        whenever(mockSubmitOutcome.invoke(onRetry = anyOrNull(), order = any()))
            .thenReturn(Resource.Empty)

        val fakeId = vm.uiState.optimisticOutcomes.first().id
        var errorMsg: String? = null
        vm.retryOptimisticOutcome(fakeId, onError = { errorMsg = it })
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Unknown error", errorMsg)
        assertEquals(com.raylabs.laundryhub.ui.home.state.SyncStatus.FAILED, vm.uiState.optimisticOutcomes.first().syncStatus)
    }

    @Test
    fun `updateOutcome handles empty or unknown resource state`() = runTest {
        whenever(mockUpdateOutcome.invoke(onRetry = anyOrNull(), order = any()))
            .thenReturn(Resource.Empty)

        val vm = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        var errorMsg: String? = null
        vm.updateOutcome(sampleOutcome, onError = { errorMsg = it })
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Unknown error", errorMsg)
        assertFalse(vm.uiState.isSubmitting)
        assertEquals(com.raylabs.laundryhub.ui.home.state.SyncStatus.FAILED, vm.uiState.optimisticUpdates[sampleOutcome.id]?.syncStatus)
    }

    @Test
    fun `retryOptimisticUpdate handles empty or unknown resource state`() = runTest {
        whenever(mockUpdateOutcome.invoke(onRetry = anyOrNull(), order = any()))
            .thenReturn(Resource.Error("first-fail"))

        val vm = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.updateOutcome(sampleOutcome)
        dispatcher.scheduler.advanceUntilIdle()

        whenever(mockUpdateOutcome.invoke(onRetry = anyOrNull(), order = any()))
            .thenReturn(Resource.Empty)

        var errorMsg: String? = null
        vm.retryOptimisticUpdate(sampleOutcome.id, onError = { errorMsg = it })
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Unknown error", errorMsg)
        assertEquals(com.raylabs.laundryhub.ui.home.state.SyncStatus.FAILED, vm.uiState.optimisticUpdates[sampleOutcome.id]?.syncStatus)
    }

    @Test
    fun `deleteOutcome handles empty or unknown resource state`() = runTest {
        whenever(mockDeleteOutcome.invoke(onRetry = anyOrNull(), outcomeId = any()))
            .thenReturn(Resource.Empty)

        val vm = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        var errorMsg: String? = null
        vm.deleteOutcome(outcomeId = "test-id", onError = { errorMsg = it })
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Unknown error", errorMsg)
        assertFalse("test-id" in vm.uiState.hiddenOutcomeIds)
    }

    private fun createViewModel(): OutcomeViewModel {
        return OutcomeViewModel(
            readOutcomeUseCase = mockReadOutcome,
            submitOutcomeUseCase = mockSubmitOutcome,
            getLastOutcomeIdUseCase = mockGetLastOutcomeId,
            updateOutcomeUseCase = mockUpdateOutcome,
            getOutcomeUseCase = mockGetOutcome,
            deleteOutcomeUseCase = mockDeleteOutcome
        )
    }
}
