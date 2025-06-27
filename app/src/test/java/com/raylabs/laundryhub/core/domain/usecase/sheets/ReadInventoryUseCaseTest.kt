package com.raylabs.laundryhub.core.domain.usecase.sheets

import com.raylabs.laundryhub.core.domain.model.sheets.InventoryData
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
class ReadInventoryUseCaseTest {
    private lateinit var repository: GoogleSheetRepository
    private lateinit var useCase: ReadInventoryUseCase

    @Before
    fun setUp() {
        repository = mock()
        useCase = ReadInventoryUseCase(repository)
    }

    @Test
    fun `returns success when repository returns success`() = runTest {
        val data = listOf(InventoryData("1", "Station1", "", "", true))
        whenever(repository.readInventoryData()).thenReturn(Resource.Success(data))
        val result = useCase.invoke()
        assertTrue(result is Resource.Success)
        assertEquals(data, (result as Resource.Success).data)
    }

    @Test
    fun `returns error when repository returns null`() = runTest {
        whenever(repository.readInventoryData()).thenReturn(null)
        val result = useCase.invoke()
        assertTrue(result is Resource.Error)
        assertEquals("Failed after 3 attempts.", (result as Resource.Error).message)
    }

    @Test
    fun `returns error when repository returns error`() = runTest {
        whenever(repository.readInventoryData()).thenReturn(Resource.Error("fail"))
        val result = useCase.invoke()
        assertTrue(result is Resource.Error)
        assertEquals("fail", (result as Resource.Error).message)
    }
}

