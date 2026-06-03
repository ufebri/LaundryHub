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

    @Test
    fun `submitPackage updates state and adds package locally`() = runTest {
        val newPkg = PackageData(name = "New", price = "1000", duration = "1h", unit = "pcs")
        whenever(readPackageUseCase.invoke(onRetry = anyOrNull()))
            .thenReturn(Resource.Success(emptyList()))
        whenever(getOtherPackageUseCase.invoke())
            .thenReturn(Resource.Empty)
        
        whenever(submitPackageUseCase.invoke(onRetry = anyOrNull(), packageData = eq(newPkg)))
            .thenReturn(Resource.Success(true))

        val viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        var completed = false
        viewModel.submitPackage(packageData = newPkg, onComplete = { completed = true })
        
        assertTrue(completed)
        assertTrue(viewModel.uiState.savePackage.data == true)
        assertTrue(viewModel.uiState.packages.data.orEmpty().any { it.name == "New" })
    }

    @Test
    fun `updatePackage updates state and updates package locally`() = runTest {
        val existing = samplePackage()
        val updated = existing.copy(price = "6000")
        whenever(readPackageUseCase.invoke(onRetry = anyOrNull()))
            .thenReturn(Resource.Success(listOf(existing)))
        whenever(getOtherPackageUseCase.invoke())
            .thenReturn(Resource.Empty)
        
        whenever(updatePackageUseCase.invoke(onRetry = anyOrNull(), packageData = eq(updated), originalPackageName = eq(existing.name)))
            .thenReturn(Resource.Success(true))

        val viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        var completed = false
        viewModel.updatePackage(originalPackageName = existing.name, packageData = updated, onComplete = { completed = true })

        assertTrue(completed)
        assertTrue(viewModel.uiState.savePackage.data == true)
        assertEquals("6000", viewModel.uiState.packages.data.orEmpty().find { it.name == existing.name }?.price)
    }

    @Test
    fun `submitPackage rolls back or sets error on backend failure`() = runTest {
        val newPkg = PackageData(name = "New", price = "1000", duration = "1h", unit = "pcs")
        whenever(readPackageUseCase.invoke(onRetry = anyOrNull())).thenReturn(Resource.Success(emptyList()))
        whenever(getOtherPackageUseCase.invoke()).thenReturn(Resource.Empty)
        whenever(submitPackageUseCase.invoke(onRetry = anyOrNull(), packageData = eq(newPkg)))
            .thenReturn(Resource.Error("Submit failed"))

        val viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        var errorMsg = ""
        viewModel.submitPackage(packageData = newPkg, onComplete = {}, onError = { errorMsg = it })
        
        assertEquals("Submit failed", errorMsg)
        assertEquals("Submit failed", viewModel.uiState.savePackage.errorMessage)
    }

    @Test
    fun `updatePackage rolls back or sets error on backend failure`() = runTest {
        val existing = samplePackage()
        val updated = existing.copy(price = "6000")
        whenever(readPackageUseCase.invoke(onRetry = anyOrNull())).thenReturn(Resource.Success(listOf(existing)))
        whenever(getOtherPackageUseCase.invoke()).thenReturn(Resource.Empty)
        whenever(updatePackageUseCase.invoke(onRetry = anyOrNull(), packageData = eq(updated), originalPackageName = eq(existing.name)))
            .thenReturn(Resource.Error("Update failed"))

        val viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        var errorMsg = ""
        viewModel.updatePackage(originalPackageName = existing.name, packageData = updated, onComplete = {}, onError = { errorMsg = it })

        assertEquals("Update failed", errorMsg)
        assertEquals("Update failed", viewModel.uiState.savePackage.errorMessage)
    }

    @Test
    fun `deletePackage rolls back or sets error on backend failure`() = runTest {
        val existing = samplePackage()
        whenever(readPackageUseCase.invoke(onRetry = anyOrNull())).thenReturn(Resource.Success(listOf(existing)))
        whenever(getOtherPackageUseCase.invoke()).thenReturn(Resource.Empty)
        whenever(deletePackageUseCase.invoke(onRetry = anyOrNull(), packageName = eq(existing.name)))
            .thenReturn(Resource.Error("Delete failed"))

        val viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        var errorMsg = ""
        viewModel.deletePackage(packageName = existing.name, onError = { errorMsg = it })
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Delete failed", errorMsg)
        assertEquals("Delete failed", viewModel.uiState.deletePackage.errorMessage)
    }

    @Test
    fun `fetchPackages handles error and empty responses`() = runTest {
        whenever(readPackageUseCase.invoke(onRetry = anyOrNull())).thenReturn(Resource.Error("Read failed"))
        whenever(getOtherPackageUseCase.invoke()).thenReturn(Resource.Empty)

        var viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("Read failed", viewModel.uiState.packages.errorMessage)

        whenever(readPackageUseCase.invoke(onRetry = anyOrNull())).thenReturn(Resource.Empty)
        viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.packages.data.orEmpty().isEmpty())
    }

    @Test
    fun `fetchOtherPackages handles error response`() = runTest {
        whenever(readPackageUseCase.invoke(onRetry = anyOrNull())).thenReturn(Resource.Success(emptyList()))
        whenever(getOtherPackageUseCase.invoke()).thenReturn(Resource.Error("Read other failed"))

        val viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("Read other failed", viewModel.uiState.otherPackages.errorMessage)
    }

    @Test
    fun `matchesPackage branch coverage`() = runTest {
        val pkg1 = PackageData(id = 0, name = "A", price = "1000", duration = "1h", unit = "pcs", sheetRowIndex = 5)
        val pkgUpdated1 = PackageData(id = 0, name = "A-updated", price = "2000", duration = "1h", unit = "pcs", sheetRowIndex = 5)
        
        whenever(readPackageUseCase.invoke(onRetry = anyOrNull())).thenReturn(Resource.Success(listOf(pkg1)))
        whenever(getOtherPackageUseCase.invoke()).thenReturn(Resource.Empty)
        whenever(updatePackageUseCase.invoke(onRetry = anyOrNull(), packageData = eq(pkgUpdated1), originalPackageName = anyOrNull()))
            .thenReturn(Resource.Success(true))

        val viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.updatePackage(packageData = pkgUpdated1, originalPackageName = "A", onComplete = {})
        assertEquals("A-updated", viewModel.uiState.packages.data.orEmpty().first().name)

        val pkg2 = PackageData(id = 0, name = "B", price = "1000", duration = "1h", unit = "pcs", sheetRowIndex = -1)
        val pkgUpdated2 = PackageData(id = 0, name = "B", price = "2000", duration = "1h", unit = "pcs", sheetRowIndex = -1)
        
        whenever(readPackageUseCase.invoke(onRetry = anyOrNull())).thenReturn(Resource.Success(listOf(pkg2)))
        whenever(updatePackageUseCase.invoke(onRetry = anyOrNull(), packageData = eq(pkgUpdated2), originalPackageName = anyOrNull()))
            .thenReturn(Resource.Success(true))

        viewModel.refreshInventory(isSilent = false)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.updatePackage(packageData = pkgUpdated2, originalPackageName = "B", onComplete = {})
        assertEquals("2000", viewModel.uiState.packages.data.orEmpty().first().price)
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
