package com.raylabs.laundryhub.ui.profile.inventory

import com.raylabs.laundryhub.core.domain.model.sheets.PackageData
import com.raylabs.laundryhub.core.domain.usecase.sheets.DeletePackageUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.GetOtherPackageUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadPackageUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.SubmitPackageUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.UpdatePackageUseCase
import com.raylabs.laundryhub.shared.util.Resource
import com.raylabs.laundryhub.ui.profile.inventory.state.PackageItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val readPackageUseCase: ReadPackageUseCase = mock()
    private val getOtherPackageUseCase: GetOtherPackageUseCase = mock()
    private val submitPackageUseCase: SubmitPackageUseCase = mock()
    private val updatePackageUseCase: UpdatePackageUseCase = mock()
    private val deletePackageUseCase: DeletePackageUseCase = mock()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `other packages empty resolves to success empty state`() = runTest {
        whenever(readPackageUseCase.invoke(onRetry = anyOrNull()))
            .thenReturn(Resource.Success(emptyList()))
        whenever(getOtherPackageUseCase.invoke())
            .thenReturn(Resource.Empty)

        val viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.otherPackages.isLoading)
        assertEquals(emptyList<String>(), viewModel.uiState.otherPackages.data)
        assertEquals(null, viewModel.uiState.otherPackages.errorMessage)
    }

    @Test
    fun `deletePackage uses delete state and removes package locally`() = runTest {
        whenever(readPackageUseCase.invoke(onRetry = anyOrNull()))
            .thenReturn(Resource.Success(listOf(samplePackage())))
            .thenReturn(Resource.Success(emptyList()))
        whenever(getOtherPackageUseCase.invoke())
            .thenReturn(Resource.Empty)
        whenever(deletePackageUseCase.invoke(onRetry = anyOrNull(), packageName = eq("Regular")))
            .thenReturn(Resource.Success(true))

        val viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        var completed = false
        viewModel.deletePackage(packageName = "Regular", onComplete = { completed = true })
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(completed)
        assertEquals(true, viewModel.uiState.deletePackage.data)
        assertFalse(viewModel.uiState.deletePackage.isLoading)
        assertFalse(viewModel.uiState.savePackage.isLoading)
        assertTrue(viewModel.uiState.packages.data.orEmpty().none { it.name == "Regular" })
    }

    @Test
    fun `backend package without sheet row still opens as edit mode`() {
        val packageItem = PackageItem(
            id = 7,
            name = "Regular",
            price = "5000",
            work = "3d",
            unit = "kg",
            sheetRowIndex = -1
        )

        val editorState = packageItem.toEditorState()

        assertTrue(editorState.isEditMode)
        assertEquals("Regular", editorState.originalName)
        assertEquals(-1, editorState.toPackageData().sheetRowIndex)
    }

    private fun createViewModel(): InventoryViewModel {
        return InventoryViewModel(
            readPackageUseCase = readPackageUseCase,
            getOtherPackageUseCase = getOtherPackageUseCase,
            submitPackageUseCase = submitPackageUseCase,
            updatePackageUseCase = updatePackageUseCase,
            deletePackageUseCase = deletePackageUseCase
        )
    }

    private fun samplePackage() = PackageData(
        id = 7,
        name = "Regular",
        price = "5000",
        duration = "3d",
        unit = "kg"
    )
}
