package com.raylabs.laundryhub.ui.inventory

import com.raylabs.laundryhub.core.domain.model.settings.SpreadsheetConfig
import com.raylabs.laundryhub.core.domain.model.sheets.PackageData
import com.raylabs.laundryhub.core.domain.usecase.settings.ObserveSpreadsheetConfigUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.DeletePackageUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.GetOtherPackageUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadPackageUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.SubmitPackageUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.UpdatePackageUseCase
import com.raylabs.laundryhub.ui.common.dummy.inventory.dummyInventoryUiState
import com.raylabs.laundryhub.ui.common.util.Resource
import com.raylabs.laundryhub.ui.profile.inventory.InventoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mockReadPackageUseCase: ReadPackageUseCase = mock()
    private val mockGetOtherPackageUseCase: GetOtherPackageUseCase = mock()
    private val mockSubmitPackageUseCase: SubmitPackageUseCase = mock()
    private val mockUpdatePackageUseCase: UpdatePackageUseCase = mock()
    private val mockDeletePackageUseCase: DeletePackageUseCase = mock()
    private val mockObserveSpreadsheetConfigUseCase: ObserveSpreadsheetConfigUseCase = mock()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init fetches packages and otherPackages successfully`() = runTest {
        val dummyPackages = dummyInventoryUiState.packages.data!!.map {
            PackageData(name = it.name, price = it.price, duration = it.work, unit = "")
        }
        val dummyOtherPackages = dummyInventoryUiState.otherPackages.data!!
        stubSpreadsheetConfig()

        whenever(mockReadPackageUseCase.invoke()).thenReturn(Resource.Success(dummyPackages))
        whenever(mockGetOtherPackageUseCase.invoke()).thenReturn(Resource.Success(dummyOtherPackages))

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState
        assertNotNull(state.packages.data)
        assertEquals(dummyInventoryUiState.packages.data!!.size, state.packages.data!!.size)
        assertEquals(dummyInventoryUiState.packages.data!!.first().name, state.packages.data!!.first().name)
        assertNotNull(state.otherPackages.data)
        assertEquals(dummyInventoryUiState.otherPackages.data!!.size, state.otherPackages.data!!.size)
        assertEquals(dummyInventoryUiState.otherPackages.data!!.first(), state.otherPackages.data!!.first())
        assertFalse(state.packages.isLoading)
        assertFalse(state.otherPackages.isLoading)
        assertEquals("LaundryHub Spreadsheet", state.spreadsheetName)
        assertEquals("sheet-123", state.spreadsheetId)
    }

    @Test
    fun `fetchPackages handles error`() = runTest {
        stubSpreadsheetConfig(empty = true)
        whenever(mockReadPackageUseCase.invoke()).thenReturn(Resource.Error("Gagal"))
        whenever(mockGetOtherPackageUseCase.invoke()).thenReturn(Resource.Success(emptyList()))
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState
        assertEquals("Gagal", state.packages.errorMessage)
        assertNull(state.packages.data)
    }

    @Test
    fun `fetchPackages handles empty as empty list`() = runTest {
        stubSpreadsheetConfig(empty = true)
        whenever(mockReadPackageUseCase.invoke()).thenReturn(Resource.Empty)
        whenever(mockGetOtherPackageUseCase.invoke()).thenReturn(Resource.Success(emptyList()))

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(vm.uiState.packages.data)
        assertTrue(vm.uiState.packages.data!!.isEmpty())
        assertNull(vm.uiState.packages.errorMessage)
    }

    @Test
    fun `fetchOtherPackages handles error and empty`() = runTest {
        stubSpreadsheetConfig(empty = true)
        whenever(mockReadPackageUseCase.invoke()).thenReturn(Resource.Success(emptyList()))
        whenever(mockGetOtherPackageUseCase.invoke()).thenReturn(Resource.Error("API error"))
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState
        assertEquals("API error", state.otherPackages.errorMessage)
        assertNull(state.otherPackages.data)

        whenever(mockGetOtherPackageUseCase.invoke()).thenReturn(Resource.Empty)
        vm.refreshInventory()
        testDispatcher.scheduler.advanceUntilIdle()

        val stateAfterEmpty = vm.uiState
        assertNotNull(stateAfterEmpty.otherPackages.data)
        assertTrue(stateAfterEmpty.otherPackages.data!!.isEmpty())
        assertNull(stateAfterEmpty.otherPackages.errorMessage)
    }

    @Test
    fun `refreshInventory keeps existing packages when refresh fails`() = runTest {
        stubSpreadsheetConfig(empty = true)
        whenever(mockReadPackageUseCase.invoke()).thenReturn(
            Resource.Success(listOf(packageData(name = "Regular"))),
            Resource.Error("Refresh failed")
        )
        whenever(mockGetOtherPackageUseCase.invoke()).thenReturn(Resource.Success(emptyList()))

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.refreshInventory()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, vm.uiState.packages.data!!.size)
        assertEquals("Regular", vm.uiState.packages.data!!.first().name)
        assertEquals("Refresh failed", vm.uiState.packages.errorMessage)
        assertFalse(vm.uiState.packages.isLoading)
    }

    @Test
    fun `refreshInventory keeps existing other packages when refresh fails`() = runTest {
        stubSpreadsheetConfig(empty = true)
        whenever(mockReadPackageUseCase.invoke()).thenReturn(Resource.Success(emptyList()))
        whenever(mockGetOtherPackageUseCase.invoke()).thenReturn(
            Resource.Success(listOf("Express")),
            Resource.Error("Other packages failed")
        )

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.refreshInventory()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("Express"), vm.uiState.otherPackages.data)
        assertEquals("Other packages failed", vm.uiState.otherPackages.errorMessage)
        assertFalse(vm.uiState.otherPackages.isLoading)
    }

    @Test
    fun `refreshInventory re-fetches packages and otherPackages`() = runTest {
        stubSpreadsheetConfig(empty = true)
        whenever(mockReadPackageUseCase.invoke()).thenReturn(Resource.Success(emptyList()))
        whenever(mockGetOtherPackageUseCase.invoke()).thenReturn(Resource.Success(emptyList()))

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.refreshInventory()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(mockReadPackageUseCase, atLeast(2)).invoke()
        verify(mockGetOtherPackageUseCase, atLeast(2)).invoke()
    }

    @Test
    fun `submitPackage updates save state and refreshes inventory`() = runTest {
        stubSpreadsheetConfig(empty = true)
        whenever(mockReadPackageUseCase.invoke()).thenReturn(Resource.Success(emptyList()))
        whenever(mockGetOtherPackageUseCase.invoke()).thenReturn(Resource.Success(emptyList()))
        whenever(
            mockSubmitPackageUseCase.invoke(
                onRetry = org.mockito.kotlin.anyOrNull(),
                packageData = org.mockito.kotlin.any()
            )
        )
            .thenReturn(Resource.Success(true))

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.submitPackage(
            packageData = PackageData(price = "5000", name = "Regular", duration = "3d", unit = "kg"),
            onComplete = {},
            onError = {}
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, vm.uiState.savePackage.data)
        verify(mockReadPackageUseCase, atLeast(2)).invoke()
        verify(mockGetOtherPackageUseCase, atLeast(2)).invoke()
    }

    @Test
    fun `submitPackage handles error state`() = runTest {
        stubSpreadsheetConfig(empty = true)
        whenever(mockReadPackageUseCase.invoke()).thenReturn(Resource.Success(emptyList()))
        whenever(mockGetOtherPackageUseCase.invoke()).thenReturn(Resource.Success(emptyList()))
        whenever(
            mockSubmitPackageUseCase.invoke(
                onRetry = org.mockito.kotlin.anyOrNull(),
                packageData = org.mockito.kotlin.any()
            )
        ).thenReturn(Resource.Error("Duplicate package"))

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        var completed = false
        var receivedError: String? = null

        vm.submitPackage(
            packageData = packageData(),
            onComplete = { completed = true },
            onError = { receivedError = it }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(completed)
        assertEquals("Duplicate package", receivedError)
        assertEquals("Duplicate package", vm.uiState.savePackage.errorMessage)
    }

    @Test
    fun `submitPackage keeps loading state when use case returns loading`() = runTest {
        stubSpreadsheetConfig(empty = true)
        whenever(mockReadPackageUseCase.invoke()).thenReturn(Resource.Success(emptyList()))
        whenever(mockGetOtherPackageUseCase.invoke()).thenReturn(Resource.Success(emptyList()))
        whenever(
            mockSubmitPackageUseCase.invoke(
                onRetry = org.mockito.kotlin.anyOrNull(),
                packageData = org.mockito.kotlin.any()
            )
        ).thenReturn(Resource.Loading)

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.submitPackage(
            packageData = packageData(),
            onComplete = {},
            onError = {}
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.savePackage.isLoading)
    }

    @Test
    fun `updatePackage updates save state and refreshes inventory`() = runTest {
        stubSpreadsheetConfig(empty = true)
        whenever(mockReadPackageUseCase.invoke()).thenReturn(Resource.Success(emptyList()))
        whenever(mockGetOtherPackageUseCase.invoke()).thenReturn(Resource.Success(emptyList()))
        whenever(
            mockUpdatePackageUseCase.invoke(
                onRetry = org.mockito.kotlin.anyOrNull(),
                packageData = org.mockito.kotlin.any()
            )
        ).thenReturn(Resource.Success(true))

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.updatePackage(
            packageData = packageData(name = "Express", row = 4),
            onComplete = {},
            onError = {}
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, vm.uiState.savePackage.data)
        verify(mockReadPackageUseCase, atLeast(2)).invoke()
        verify(mockGetOtherPackageUseCase, atLeast(2)).invoke()
    }

    @Test
    fun `updatePackage handles error state`() = runTest {
        stubSpreadsheetConfig(empty = true)
        whenever(mockReadPackageUseCase.invoke()).thenReturn(Resource.Success(emptyList()))
        whenever(mockGetOtherPackageUseCase.invoke()).thenReturn(Resource.Success(emptyList()))
        whenever(
            mockUpdatePackageUseCase.invoke(
                onRetry = org.mockito.kotlin.anyOrNull(),
                packageData = org.mockito.kotlin.any()
            )
        ).thenReturn(Resource.Error("Package row not found."))

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        var completed = false
        var receivedError: String? = null

        vm.updatePackage(
            packageData = packageData(name = "Express", row = 4),
            onComplete = { completed = true },
            onError = { receivedError = it }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(completed)
        assertEquals("Package row not found.", receivedError)
        assertEquals("Package row not found.", vm.uiState.savePackage.errorMessage)
    }

    @Test
    fun `updatePackage keeps loading state when use case returns loading`() = runTest {
        stubSpreadsheetConfig(empty = true)
        whenever(mockReadPackageUseCase.invoke()).thenReturn(Resource.Success(emptyList()))
        whenever(mockGetOtherPackageUseCase.invoke()).thenReturn(Resource.Success(emptyList()))
        whenever(
            mockUpdatePackageUseCase.invoke(
                onRetry = org.mockito.kotlin.anyOrNull(),
                packageData = org.mockito.kotlin.any()
            )
        ).thenReturn(Resource.Loading)

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.updatePackage(
            packageData = packageData(name = "Express", row = 4),
            onComplete = {},
            onError = {}
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.savePackage.isLoading)
    }

    @Test
    fun `deletePackage updates delete state and refreshes inventory`() = runTest {
        stubSpreadsheetConfig(empty = true)
        whenever(mockReadPackageUseCase.invoke()).thenReturn(Resource.Success(emptyList()))
        whenever(mockGetOtherPackageUseCase.invoke()).thenReturn(Resource.Success(emptyList()))
        whenever(mockDeletePackageUseCase.invoke(onRetry = null, sheetRowIndex = 4))
            .thenReturn(Resource.Success(true))

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.deletePackage(
            sheetRowIndex = 4,
            onComplete = {},
            onError = {}
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, vm.uiState.deletePackage.data)
        verify(mockReadPackageUseCase, atLeast(2)).invoke()
        verify(mockGetOtherPackageUseCase, atLeast(2)).invoke()
    }

    @Test
    fun `deletePackage handles error state`() = runTest {
        stubSpreadsheetConfig(empty = true)
        whenever(mockReadPackageUseCase.invoke()).thenReturn(Resource.Success(emptyList()))
        whenever(mockGetOtherPackageUseCase.invoke()).thenReturn(Resource.Success(emptyList()))
        whenever(mockDeletePackageUseCase.invoke(onRetry = null, sheetRowIndex = 4))
            .thenReturn(Resource.Error("Failed to delete data."))

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        var completed = false
        var receivedError: String? = null

        vm.deletePackage(
            sheetRowIndex = 4,
            onComplete = { completed = true },
            onError = { receivedError = it }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(completed)
        assertEquals("Failed to delete data.", receivedError)
        assertEquals("Failed to delete data.", vm.uiState.deletePackage.errorMessage)
    }

    @Test
    fun `deletePackage keeps loading state when use case returns loading`() = runTest {
        stubSpreadsheetConfig(empty = true)
        whenever(mockReadPackageUseCase.invoke()).thenReturn(Resource.Success(emptyList()))
        whenever(mockGetOtherPackageUseCase.invoke()).thenReturn(Resource.Success(emptyList()))
        whenever(mockDeletePackageUseCase.invoke(onRetry = null, sheetRowIndex = 4))
            .thenReturn(Resource.Loading)

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.deletePackage(
            sheetRowIndex = 4,
            onComplete = {},
            onError = {}
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.deletePackage.isLoading)
    }

    @Test
    fun `init keeps sections loading when use cases return loading`() = runTest {
        stubSpreadsheetConfig(empty = true)
        whenever(mockReadPackageUseCase.invoke()).thenReturn(Resource.Loading)
        whenever(mockGetOtherPackageUseCase.invoke()).thenReturn(Resource.Loading)

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.packages.isLoading)
        assertTrue(vm.uiState.otherPackages.isLoading)
    }

    private fun stubSpreadsheetConfig(empty: Boolean = false) {
        whenever(mockObserveSpreadsheetConfigUseCase.invoke()).thenReturn(
            flowOf(
                if (empty) {
                    SpreadsheetConfig()
                } else {
                    SpreadsheetConfig(
                        spreadsheetId = "sheet-123",
                        spreadsheetName = "LaundryHub Spreadsheet",
                        spreadsheetUrl = "https://docs.google.com/spreadsheets/d/sheet-123/edit"
                    )
                }
            )
        )
    }

    private fun packageData(
        name: String = "Regular",
        price: String = "5000",
        duration: String = "3d",
        unit: String = "kg",
        row: Int = -1
    ): PackageData {
        return PackageData(
            price = price,
            name = name,
            duration = duration,
            unit = unit,
            sheetRowIndex = row
        )
    }

    private fun createViewModel(): InventoryViewModel {
        return InventoryViewModel(
            mockReadPackageUseCase,
            mockGetOtherPackageUseCase,
            mockSubmitPackageUseCase,
            mockUpdatePackageUseCase,
            mockDeletePackageUseCase,
            mockObserveSpreadsheetConfigUseCase
        )
    }
}
