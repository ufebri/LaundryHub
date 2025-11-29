package com.raylabs.laundryhub.ui.inventory

import com.raylabs.laundryhub.core.domain.model.sheets.PackageData
import com.raylabs.laundryhub.core.domain.usecase.sheets.GetOtherPackageUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadPackageUseCase
import com.raylabs.laundryhub.ui.common.dummy.inventory.dummyInventoryUiState
import com.raylabs.laundryhub.ui.common.util.Resource
import com.raylabs.laundryhub.ui.profile.inventory.InventoryViewModel
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mockReadPackageUseCase: ReadPackageUseCase = mock()
    private val mockGetOtherPackageUseCase: GetOtherPackageUseCase = mock()

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

        whenever(mockReadPackageUseCase.invoke()).thenReturn(Resource.Success(dummyPackages))
        whenever(mockGetOtherPackageUseCase.invoke()).thenReturn(Resource.Success(dummyOtherPackages))

        val vm = InventoryViewModel(mockReadPackageUseCase, mockGetOtherPackageUseCase)
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
    }

    @Test
    fun `fetchPackages handles error`() = runTest {
        whenever(mockReadPackageUseCase.invoke()).thenReturn(Resource.Error("Gagal"))
        whenever(mockGetOtherPackageUseCase.invoke()).thenReturn(Resource.Success(emptyList()))
        val vm = InventoryViewModel(mockReadPackageUseCase, mockGetOtherPackageUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState
        assertEquals("Gagal", state.packages.errorMessage)
        assertNull(state.packages.data)
    }

    @Test
    fun `fetchOtherPackages handles error and empty`() = runTest {
        whenever(mockReadPackageUseCase.invoke()).thenReturn(Resource.Success(emptyList()))
        whenever(mockGetOtherPackageUseCase.invoke()).thenReturn(Resource.Error("API error"))
        val vm = InventoryViewModel(mockReadPackageUseCase, mockGetOtherPackageUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState
        assertEquals("API error", state.otherPackages.errorMessage)
        assertNull(state.otherPackages.data)

        // Test Empty resource
        whenever(mockGetOtherPackageUseCase.invoke()).thenReturn(Resource.Empty)
        vm.apply {
            // jalankan lagi fetchOtherPackages untuk trigger state
            val method = this::class.java.getDeclaredMethod("fetchOtherPackages")
            method.isAccessible = true
            method.invoke(this)
        }
        testDispatcher.scheduler.advanceUntilIdle()
        val stateAfterEmpty = vm.uiState
        assertEquals("Tidak ada paket lain.", stateAfterEmpty.otherPackages.errorMessage)
    }
}
