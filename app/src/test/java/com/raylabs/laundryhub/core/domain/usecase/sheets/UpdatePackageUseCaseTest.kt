package com.raylabs.laundryhub.core.domain.usecase.sheets

import com.raylabs.laundryhub.core.domain.model.sheets.PackageData
import com.raylabs.laundryhub.core.domain.repository.LaundryRepository
import com.raylabs.laundryhub.shared.util.Resource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class UpdatePackageUseCaseTest {

    private lateinit var repository: LaundryRepository
    private lateinit var updatePackageUseCase: UpdatePackageUseCase

    @Before
    fun setUp() {
        repository = mock()
        updatePackageUseCase = UpdatePackageUseCase(repository)
    }

    @Test
    fun `invoke returns error when package validation fails`() = runTest {
        val invalidPackage = PackageData(id = 1, name = "", price = "1000", duration = "2", unit = "Hari")
        val result = updatePackageUseCase(packageData = invalidPackage)
        assertEquals(Resource.Error("Package name is required."), result)
    }

    @Test
    fun `invoke returns error when duplicate name is found in database`() = runTest {
        val existing = listOf(
            PackageData(id = 1, name = "Express", price = "1000", duration = "2", unit = "Hari"),
            PackageData(id = 2, name = "Regular", price = "1000", duration = "2", unit = "Hari")
        )
        whenever(repository.readPackageData()).thenReturn(Resource.Success(existing))

        // Updating a package to name "Regular" (which duplicate of id=2)
        val toUpdate = PackageData(id = 1, name = "Regular", price = "1000", duration = "2", unit = "Hari", sheetRowIndex = 2)
        val result = updatePackageUseCase(packageData = toUpdate)

        assertEquals(Resource.Error("Package name already exists in the master list."), result)
    }

    @Test
    fun `invoke returns error when reading existing packages fails`() = runTest {
        whenever(repository.readPackageData()).thenReturn(Resource.Error("Read error"))

        val newPackage = PackageData(id = 2, name = "Express", price = "1000", duration = "2", unit = "Hari")
        val result = updatePackageUseCase(packageData = newPackage)

        assertEquals(Resource.Error("Read error"), result)
    }

    @Test
    fun `invoke returns success when repository update succeeds`() = runTest {
        whenever(repository.readPackageData()).thenReturn(Resource.Success(emptyList()))
        whenever(repository.updatePackage(any(), any())).thenReturn(Resource.Success(true))

        val newPackage = PackageData(id = 2, name = "Express", price = "1000", duration = "2", unit = "Hari")
        val result = updatePackageUseCase(packageData = newPackage, originalPackageName = "Old Express")

        assertEquals(Resource.Success(true), result)
        verify(repository).updatePackage("Old Express", newPackage)
    }

    @Test
    fun `invoke uses package name when originalPackageName is blank`() = runTest {
        whenever(repository.readPackageData()).thenReturn(Resource.Success(emptyList()))
        whenever(repository.updatePackage(any(), any())).thenReturn(Resource.Success(true))

        val newPackage = PackageData(id = 2, name = "Express", price = "1000", duration = "2", unit = "Hari")
        val result = updatePackageUseCase(packageData = newPackage, originalPackageName = "")

        assertEquals(Resource.Success(true), result)
        verify(repository).updatePackage("Express", newPackage)
    }
}
