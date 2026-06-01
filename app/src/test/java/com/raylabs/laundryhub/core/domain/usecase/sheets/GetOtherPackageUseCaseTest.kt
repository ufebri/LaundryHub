package com.raylabs.laundryhub.core.domain.usecase.sheets

import com.raylabs.laundryhub.core.domain.model.sheets.PackageData
import com.raylabs.laundryhub.core.domain.repository.LaundryRepository
import com.raylabs.laundryhub.shared.util.Resource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class GetOtherPackageUseCaseTest {

    private lateinit var repository: LaundryRepository
    private lateinit var useCase: GetOtherPackageUseCase

    @Before
    fun setUp() {
        repository = mock()
        useCase = GetOtherPackageUseCase(repository)
    }

    @Test
    fun `invoke returns success when other packages exist`() = runTest {
        val remarks = listOf("Regular", "Dry Clean", "Special Care", "   Dry   Clean  ")
        val packages = listOf(
            PackageData(price = "10", name = "Regular", duration = "1 hour", unit = "kg")
        )

        whenever(repository.readOtherPackage()).thenReturn(Resource.Success(remarks))
        whenever(repository.readPackageData()).thenReturn(Resource.Success(packages))

        val result = useCase()

        // "Regular" exists in package data. "Dry Clean" and "Special Care" are other packages.
        // Whitespace normalized "Dry Clean" should not be duplicated.
        assertEquals(Resource.Success(listOf("Dry Clean", "Special Care")), result)
        verify(repository).readOtherPackage()
        verify(repository).readPackageData()
    }

    @Test
    fun `invoke returns empty when no other packages exist`() = runTest {
        val remarks = listOf("Regular")
        val packages = listOf(
            PackageData(price = "10", name = "Regular", duration = "1 hour", unit = "kg")
        )

        whenever(repository.readOtherPackage()).thenReturn(Resource.Success(remarks))
        whenever(repository.readPackageData()).thenReturn(Resource.Success(packages))

        val result = useCase()

        assertEquals(Resource.Empty, result)
    }

    @Test
    fun `invoke returns error when readOtherPackage returns error`() = runTest {
        whenever(repository.readOtherPackage()).thenReturn(Resource.Error("Error remarks"))
        whenever(repository.readPackageData()).thenReturn(Resource.Success(emptyList()))

        val result = useCase()

        assertEquals(Resource.Error("Error remarks"), result)
    }

    @Test
    fun `invoke returns error when readPackageData returns error`() = runTest {
        whenever(repository.readOtherPackage()).thenReturn(Resource.Success(listOf("Regular")))
        whenever(repository.readPackageData()).thenReturn(Resource.Error("Error packages"))

        val result = useCase()

        assertEquals(Resource.Error("Error packages"), result)
    }
}
