package com.raylabs.laundryhub.core.domain.usecase.sheets

import com.raylabs.laundryhub.core.domain.model.sheets.HistoryData
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
class SubmitHistoryUseCaseTest {
    private lateinit var repository: GoogleSheetRepository
    private lateinit var useCase: SubmitHistoryUseCase

    @Before
    fun setUp() {
        repository = mock()
        useCase = SubmitHistoryUseCase(repository)
    }

    @Test
    fun `returns success when repository returns success`() = runTest {
        val history = HistoryData("1", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "")
        whenever(repository.addHistoryOrder(history)).thenReturn(Resource.Success(true))
        val result = useCase.invoke(history = history)
        assertTrue(result is Resource.Success)
        assertEquals(true, (result as Resource.Success).data)
    }

    @Test
    fun `returns error when repository returns null`() = runTest {
        val history = HistoryData("1", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "")
        whenever(repository.addHistoryOrder(history)).thenReturn(null)
        val result = useCase.invoke(history = history)
        assertTrue(result is Resource.Error)
        assertEquals("Failed to submit data", (result as Resource.Error).message)
    }

    @Test
    fun `returns error when repository returns error`() = runTest {
        val history = HistoryData("1", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "")
        whenever(repository.addHistoryOrder(history)).thenReturn(Resource.Error("fail"))
        val result = useCase.invoke(history = history)
        assertTrue(result is Resource.Error)
        assertEquals("fail", (result as Resource.Error).message)
    }
}

