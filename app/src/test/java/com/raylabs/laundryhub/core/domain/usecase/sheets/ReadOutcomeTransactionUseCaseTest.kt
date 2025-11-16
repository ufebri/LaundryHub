package com.raylabs.laundryhub.core.domain.usecase.sheets

import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.core.domain.repository.GoogleSheetRepository
import com.raylabs.laundryhub.ui.common.util.Resource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ReadOutcomeTransactionUseCaseTest {
    private val repository: GoogleSheetRepository = mock()
    private val useCase = ReadOutcomeTransactionUseCase(repository)

    @Test
    fun `invoke returns repository data when success`() = runTest {
        val expectedData = listOf(
            OutcomeData("1", "01/01/2025", "Gas", "Rp20.000", "", "cash")
        )
        whenever(repository.readOutcomeTransactions()).thenReturn(Resource.Success(expectedData))

        val result = useCase()

        assertTrue(result is Resource.Success)
        assertEquals(expectedData, (result as Resource.Success).data)
    }

    @Test
    fun `invoke returns error when repository keeps failing`() = runTest {
        whenever(repository.readOutcomeTransactions()).thenThrow(RuntimeException("boom"))

        val result = useCase()

        assertTrue(result is Resource.Error)
        assertEquals("Failed after 3 attempts.", (result as Resource.Error).message)
    }
}
