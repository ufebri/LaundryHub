package com.raylabs.laundryhub.core.domain.usecase.sheets

import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.core.domain.repository.GoogleSheetRepository
import com.raylabs.laundryhub.ui.common.util.Resource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SubmitOutcomeUseCaseTest {

    private val repository: GoogleSheetRepository = mock()
    private val useCase = SubmitOutcomeUseCase(repository)

    @Test
    fun `invoke returns success when repository succeeds`() = runTest {
        whenever(repository.addOutcome(any())).thenReturn(Resource.Success(true))
        val data = OutcomeData("1", "01/01/2025", "Gas", "Rp10.000", "", "cash")

        val result = useCase(data)

        assertTrue(result is Resource.Success)
        assertTrue((result as Resource.Success).data)
    }

    @Test
    fun `invoke returns error after retries on failure`() = runTest {
        whenever(repository.addOutcome(any())).thenThrow(RuntimeException("boom"))
        val data = OutcomeData("1", "01/01/2025", "Gas", "Rp10.000", "", "cash")

        val result = useCase(data)

        assertTrue(result is Resource.Error)
        assertEquals("Failed after 3 attempts.", (result as Resource.Error).message)
    }
}
