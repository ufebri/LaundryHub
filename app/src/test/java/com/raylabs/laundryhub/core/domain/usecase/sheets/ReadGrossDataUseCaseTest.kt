package com.raylabs.laundryhub.core.domain.usecase.sheets

import com.raylabs.laundryhub.core.domain.model.sheets.GrossData
import com.raylabs.laundryhub.core.domain.repository.LaundryRepository
import com.raylabs.laundryhub.shared.util.Resource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ReadGrossDataUseCaseTest {

    private lateinit var repository: LaundryRepository
    private lateinit var readGrossDataUseCase: ReadGrossDataUseCase

    @Before
    fun setUp() {
        repository = mock()
        readGrossDataUseCase = ReadGrossDataUseCase(repository)
    }

    @Test
    fun `invoke returns success when repository returns data`() = runTest {
        val data = listOf(GrossData(month = "Mei 2026", totalNominal = "Rp1.000.000", orderCount = "10", tax = "0"))
        whenever(repository.readGrossData()).thenReturn(Resource.Success(data))

        val result = readGrossDataUseCase()

        assertEquals(Resource.Success(data), result)
    }

    @Test
    fun `invoke returns error when repository fails after all attempts`() = runTest {
        whenever(repository.readGrossData()).thenThrow(RuntimeException("Persistent error"))

        val result = readGrossDataUseCase()

        assertEquals(Resource.Error("Failed after 3 attempts."), result)
    }

    @Test
    fun `getPagingData returns paging flow successfully`() = runTest {
        val flow = readGrossDataUseCase.getPagingData()
        assertNotNull(flow)
    }
}
