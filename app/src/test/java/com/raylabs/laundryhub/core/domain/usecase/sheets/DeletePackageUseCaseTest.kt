package com.raylabs.laundryhub.core.domain.usecase.sheets

import com.raylabs.laundryhub.core.domain.repository.LaundryRepository
import com.raylabs.laundryhub.core.domain.usecase.UseCaseErrorHandling
import com.raylabs.laundryhub.shared.util.Resource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class DeletePackageUseCaseTest {

    private lateinit var repository: LaundryRepository
    private lateinit var deletePackageUseCase: DeletePackageUseCase

    @Before
    fun setUp() {
        repository = mock()
        deletePackageUseCase = DeletePackageUseCase(repository)
    }

    @Test
    fun `invoke returns error when package name is blank`() = runTest {
        val result = deletePackageUseCase(packageName = "")

        assertEquals(Resource.Error("Package name is required."), result)
    }

    @Test
    fun `invoke returns success when repository delete succeeds`() = runTest {
        whenever(repository.deletePackage("Normal Package")).thenReturn(Resource.Success(true))

        val result = deletePackageUseCase(packageName = "Normal Package")

        assertEquals(Resource.Success(true), result)
        verify(repository).deletePackage("Normal Package")
    }

    @Test
    fun `invoke returns error when repository delete returns error`() = runTest {
        whenever(repository.deletePackage("Normal Package")).thenReturn(Resource.Error("Failed to delete"))

        val result = deletePackageUseCase(packageName = "Normal Package")

        assertEquals(Resource.Error("Failed to delete"), result)
        verify(repository).deletePackage("Normal Package")
    }

    @Test
    fun `invoke retries and succeeds after exceptions are thrown`() = runTest {
        var callCount = 0
        whenever(repository.deletePackage("Normal Package")).thenAnswer {
            callCount++
            if (callCount < 2) {
                throw RuntimeException("Temporary network issue")
            }
            Resource.Success(true)
        }

        val retriesInvoked = mutableListOf<Int>()
        val result = deletePackageUseCase(
            onRetry = { retriesInvoked.add(it) },
            packageName = "Normal Package"
        )

        assertEquals(Resource.Success(true), result)
        assertEquals(2, callCount)
        assertEquals(listOf(1), retriesInvoked)
    }

    @Test
    fun `invoke returns handleFailedSubmit after exhausting all retries on exception`() = runTest {
        whenever(repository.deletePackage("Normal Package")).thenThrow(RuntimeException("Persistent error"))

        val retriesInvoked = mutableListOf<Int>()
        val result = deletePackageUseCase(
            onRetry = { retriesInvoked.add(it) },
            packageName = "Normal Package"
        )

        assertEquals(UseCaseErrorHandling.handleFailedSubmit, result)
        assertEquals(listOf(1, 2), retriesInvoked)
        verify(repository, times(3)).deletePackage("Normal Package")
    }
}
