package com.raylabs.laundryhub.core.domain.usecase.sheets

import com.raylabs.laundryhub.core.domain.model.sheets.PackageData
import com.raylabs.laundryhub.core.domain.repository.GoogleSheetRepository
import com.raylabs.laundryhub.ui.common.util.Resource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class PackageCrudUseCasesTest {

    private lateinit var repository: GoogleSheetRepository
    private lateinit var submitPackageUseCase: SubmitPackageUseCase
    private lateinit var updatePackageUseCase: UpdatePackageUseCase
    private lateinit var deletePackageUseCase: DeletePackageUseCase

    @Before
    fun setUp() {
        repository = mock()
        submitPackageUseCase = SubmitPackageUseCase(repository)
        updatePackageUseCase = UpdatePackageUseCase(repository)
        deletePackageUseCase = DeletePackageUseCase(repository)
    }

    @Test
    fun `submitPackage returns success when repository addPackage succeeds`() = runTest {
        val packageData = PackageData(price = "5000", name = "Regular", duration = "3d", unit = "kg")
        whenever(repository.readPackageData()).thenReturn(Resource.Success(emptyList()))
        whenever(repository.addPackage(packageData)).thenReturn(Resource.Success(true))

        val result = submitPackageUseCase(packageData = packageData)

        assertTrue(result is Resource.Success)
        assertEquals(true, (result as Resource.Success).data)
    }

    @Test
    fun `submitPackage rejects duplicate package name`() = runTest {
        val packageData = PackageData(price = "5000", name = "Regular", duration = "3d", unit = "kg")
        whenever(repository.readPackageData()).thenReturn(
            Resource.Success(
                listOf(PackageData(price = "8000", name = " regular ", duration = "1d", unit = "kg", sheetRowIndex = 2))
            )
        )

        val result = submitPackageUseCase(packageData = packageData)

        assertTrue(result is Resource.Error)
        assertEquals("Package name already exists in the master list.", (result as Resource.Error).message)
    }

    @Test
    fun `updatePackage returns success when repository updatePackage succeeds`() = runTest {
        val packageData = PackageData(
            price = "8000",
            name = "Express",
            duration = "1d",
            unit = "kg",
            sheetRowIndex = 4
        )
        whenever(repository.readPackageData()).thenReturn(Resource.Success(listOf(packageData)))
        whenever(repository.updatePackage(packageData)).thenReturn(Resource.Success(true))

        val result = updatePackageUseCase(packageData = packageData)

        assertTrue(result is Resource.Success)
        assertEquals(true, (result as Resource.Success).data)
    }

    @Test
    fun `updatePackage rejects missing row index`() = runTest {
        val packageData = PackageData(
            price = "8000",
            name = "Express",
            duration = "1d",
            unit = "kg"
        )

        val result = updatePackageUseCase(packageData = packageData)

        assertTrue(result is Resource.Error)
        assertEquals("Package row not found.", (result as Resource.Error).message)
    }

    @Test
    fun `deletePackage returns success when repository deletePackage succeeds`() = runTest {
        whenever(repository.deletePackage(4)).thenReturn(Resource.Success(true))

        val result = deletePackageUseCase(sheetRowIndex = 4)

        assertTrue(result is Resource.Success)
        assertEquals(true, (result as Resource.Success).data)
    }

    @Test
    fun `deletePackage rejects invalid row index`() = runTest {
        val result = deletePackageUseCase(sheetRowIndex = -1)

        assertTrue(result is Resource.Error)
        assertEquals("Package row not found.", (result as Resource.Error).message)
    }
}
