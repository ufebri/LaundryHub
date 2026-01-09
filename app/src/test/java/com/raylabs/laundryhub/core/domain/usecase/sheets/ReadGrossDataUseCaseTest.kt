package com.raylabs.laundryhub.core.domain.usecase.sheets

import com.raylabs.laundryhub.core.domain.model.sheets.GrossData
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
class ReadGrossDataUseCaseTest {
    private lateinit var repository: GoogleSheetRepository
    private lateinit var useCase: ReadGrossDataUseCase

    @Before
    fun setUp() {
        repository = mock()
        useCase = ReadGrossDataUseCase(repository)
    }

    @Test
    fun `returns success when repository returns success`() = runTest {
        val data = listOf(GrossData("januari", "Rp1.000", "2", "Rp10"))
        whenever(repository.readGrossData()).thenReturn(Resource.Success(data))
        val result = useCase.invoke()
        assertTrue(result is Resource.Success)
        assertEquals(data, (result as Resource.Success).data)
    }

    @Test
    fun `returns error when repository returns null`() = runTest {
        whenever(repository.readGrossData()).thenReturn(null)
        val result = useCase.invoke()
        assertTrue(result is Resource.Error)
        assertEquals("Failed after 3 attempts.", (result as Resource.Error).message)
    }

    @Test
    fun `returns error when repository returns error`() = runTest {
        whenever(repository.readGrossData()).thenReturn(Resource.Error("fail"))
        val result = useCase.invoke()
        assertTrue(result is Resource.Error)
        assertEquals("fail", (result as Resource.Error).message)
    }
}
