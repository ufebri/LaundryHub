package com.raylabs.laundryhub.core.domain.usecase.sheets

import com.raylabs.laundryhub.core.domain.model.sheets.PackageData
import com.raylabs.laundryhub.core.domain.repository.LaundryRepository
import com.raylabs.laundryhub.shared.util.Resource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ReadPackageUseCaseTest {

    private lateinit var repository: LaundryRepository
    private lateinit var readPackageUseCase: ReadPackageUseCase

    @Before
    fun setUp() {
        repository = mock()
        readPackageUseCase = ReadPackageUseCase(repository)
    }

    @Test
    fun `invoke returns success when repository read succeeds`() = runTest {
        val packages = listOf(PackageData(price = "15", name = "Express", duration = "1 hour", unit = "kg"))
        whenever(repository.readPackageData()).thenReturn(Resource.Success(packages))

        val result = readPackageUseCase()

        assertEquals(Resource.Success(packages), result)
        verify(repository).readPackageData()
    }

    @Test
    fun `invoke returns error when repository read returns error`() = runTest {
        whenever(repository.readPackageData()).thenReturn(Resource.Error("Failed to read"))

        val result = readPackageUseCase()

        assertEquals(Resource.Error("Failed to read"), result)
        verify(repository).readPackageData()
    }

    @Test
    fun `invoke retries and succeeds after exceptions are thrown`() = runTest {
        val packages = listOf(PackageData(price = "15", name = "Express", duration = "1 hour", unit = "kg"))
        var callCount = 0
        whenever(repository.readPackageData()).thenAnswer {
            callCount++
            if (callCount < 2) {
                throw RuntimeException("Network glitch")
            }
            Resource.Success(packages)
        }

        val retriesInvoked = mutableListOf<Int>()
        val result = readPackageUseCase(onRetry = { retriesInvoked.add(it) })

        assertEquals(Resource.Success(packages), result)
        assertEquals(2, callCount)
        assertEquals(listOf(1), retriesInvoked)
    }

    @Test
    fun `invoke returns error after exhausting all retries on exception`() = runTest {
        whenever(repository.readPackageData()).thenThrow(RuntimeException("Fatal error"))

        val retriesInvoked = mutableListOf<Int>()
        val result = readPackageUseCase(onRetry = { retriesInvoked.add(it) })

        assertEquals(Resource.Error("Failed after 3 attempts."), result)
        assertEquals(listOf(1, 2), retriesInvoked)
        verify(repository, times(3)).readPackageData()
    }
}
