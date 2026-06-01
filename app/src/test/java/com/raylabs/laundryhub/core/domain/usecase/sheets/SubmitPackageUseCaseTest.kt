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

class SubmitPackageUseCaseTest {

    private lateinit var repository: LaundryRepository
    private lateinit var submitPackageUseCase: SubmitPackageUseCase

    @Before
    fun setUp() {
        repository = mock()
        submitPackageUseCase = SubmitPackageUseCase(repository)
    }

    @Test
    fun `invoke returns error when package validation fails`() = runTest {
        val invalidPackage = PackageData(id = 1, name = "", price = "1000", duration = "2", unit = "Hari")
        val result = submitPackageUseCase(packageData = invalidPackage)
        assertEquals(Resource.Error("Package name is required."), result)
    }

    @Test
    fun `invoke returns error when duplicate name is found`() = runTest {
        val existing = listOf(PackageData(id = 1, name = "Express", price = "1000", duration = "2", unit = "Hari"))
        whenever(repository.readPackageData()).thenReturn(Resource.Success(existing))

        val newPackage = PackageData(id = 2, name = "Express", price = "1000", duration = "2", unit = "Hari")
        val result = submitPackageUseCase(packageData = newPackage)

        assertEquals(Resource.Error("Package name already exists in the master list."), result)
    }

    @Test
    fun `invoke returns error when reading existing packages fails`() = runTest {
        whenever(repository.readPackageData()).thenReturn(Resource.Error("Read error"))

        val newPackage = PackageData(id = 2, name = "Express", price = "1000", duration = "2", unit = "Hari")
        val result = submitPackageUseCase(packageData = newPackage)

        assertEquals(Resource.Error("Read error"), result)
    }

    @Test
    fun `invoke returns success when repository add succeeds`() = runTest {
        whenever(repository.readPackageData()).thenReturn(Resource.Success(emptyList()))
        whenever(repository.addPackage(any())).thenReturn(Resource.Success(true))

        val newPackage = PackageData(id = 2, name = "Express", price = "1000", duration = "2", unit = "Hari")
        val result = submitPackageUseCase(packageData = newPackage)

        assertEquals(Resource.Success(true), result)
        verify(repository).addPackage(newPackage)
    }

    @Test
    fun `invoke retries and succeeds after temporary exception`() = runTest {
        whenever(repository.readPackageData()).thenReturn(Resource.Success(emptyList()))
        var callCount = 0
        val newPackage = PackageData(id = 2, name = "Express", price = "1000", duration = "2", unit = "Hari")
        whenever(repository.addPackage(newPackage)).thenAnswer {
            callCount++
            if (callCount < 2) {
                throw RuntimeException("Network error")
            }
            Resource.Success(true)
        }

        val retriesInvoked = mutableListOf<Int>()
        val result = submitPackageUseCase(
            onRetry = { retriesInvoked.add(it) },
            packageData = newPackage
        )

        assertEquals(Resource.Success(true), result)
        assertEquals(2, callCount)
        assertEquals(listOf(1), retriesInvoked)
    }
}
