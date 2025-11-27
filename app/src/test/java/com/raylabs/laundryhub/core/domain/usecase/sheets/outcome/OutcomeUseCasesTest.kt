package com.raylabs.laundryhub.core.domain.usecase.sheets.outcome

import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
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
class OutcomeUseCasesTest {
    private lateinit var repository: GoogleSheetRepository
    private lateinit var readOutcomeTransactionUseCase: ReadOutcomeTransactionUseCase
    private lateinit var submitOutcomeUseCase: SubmitOutcomeUseCase
    private lateinit var updateOutcomeUseCase: UpdateOutcomeUseCase
    private lateinit var getOutcomeUseCase: GetOutcomeUseCase
    private lateinit var getLastOutcomeIdUseCase: GetLastOutcomeIdUseCase

    private val sampleOutcome = OutcomeData(
        id = "1",
        date = "01/01/2025",
        purpose = "Test",
        price = "1000",
        remark = "-",
        payment = "cash"
    )

    @Before
    fun setUp() {
        repository = mock()
        readOutcomeTransactionUseCase = ReadOutcomeTransactionUseCase(repository)
        submitOutcomeUseCase = SubmitOutcomeUseCase(repository)
        updateOutcomeUseCase = UpdateOutcomeUseCase(repository)
        getOutcomeUseCase = GetOutcomeUseCase(repository)
        getLastOutcomeIdUseCase = GetLastOutcomeIdUseCase(repository)
    }

    @Test
    fun `readOutcomeTransaction returns success`() = runTest {
        val data = listOf(sampleOutcome)
        whenever(repository.readOutcomeTransaction()).thenReturn(Resource.Success(data))

        val result = readOutcomeTransactionUseCase.invoke()

        assertTrue(result is Resource.Success)
        assertEquals(data, (result as Resource.Success).data)
    }

    @Test
    fun `readOutcomeTransaction returns error when null`() = runTest {
        whenever(repository.readOutcomeTransaction()).thenReturn(null)

        val result = readOutcomeTransactionUseCase.invoke()

        assertTrue(result is Resource.Error)
        assertEquals("Failed after 3 attempts.", (result as Resource.Error).message)
    }

    @Test
    fun `readOutcomeTransaction returns error`() = runTest {
        whenever(repository.readOutcomeTransaction()).thenReturn(Resource.Error("fail"))

        val result = readOutcomeTransactionUseCase.invoke()

        assertTrue(result is Resource.Error)
        assertEquals("fail", (result as Resource.Error).message)
    }

    @Test
    fun `submitOutcome returns success`() = runTest {
        whenever(repository.addOutcome(sampleOutcome)).thenReturn(Resource.Success(true))

        val result = submitOutcomeUseCase.invoke(order = sampleOutcome)

        assertTrue(result is Resource.Success)
        assertTrue((result as Resource.Success).data)
    }

    @Test
    fun `submitOutcome returns error when null`() = runTest {
        whenever(repository.addOutcome(sampleOutcome)).thenReturn(null)

        val result = submitOutcomeUseCase.invoke(order = sampleOutcome)

        assertTrue(result is Resource.Error)
        assertEquals("Failed to submit data", (result as Resource.Error).message)
    }

    @Test
    fun `submitOutcome returns error`() = runTest {
        whenever(repository.addOutcome(sampleOutcome)).thenReturn(Resource.Error("submit fail"))

        val result = submitOutcomeUseCase.invoke(order = sampleOutcome)

        assertTrue(result is Resource.Error)
        assertEquals("submit fail", (result as Resource.Error).message)
    }

    @Test
    fun `updateOutcome returns success`() = runTest {
        whenever(repository.updateOutcome(sampleOutcome)).thenReturn(Resource.Success(true))

        val result = updateOutcomeUseCase.invoke(order = sampleOutcome)

        assertTrue(result is Resource.Success)
        assertTrue((result as Resource.Success).data)
    }

    @Test
    fun `updateOutcome returns error when null`() = runTest {
        whenever(repository.updateOutcome(sampleOutcome)).thenReturn(null)

        val result = updateOutcomeUseCase.invoke(order = sampleOutcome)

        assertTrue(result is Resource.Error)
        assertEquals("Failed to submit data", (result as Resource.Error).message)
    }

    @Test
    fun `updateOutcome returns error`() = runTest {
        whenever(repository.updateOutcome(sampleOutcome)).thenReturn(Resource.Error("update fail"))

        val result = updateOutcomeUseCase.invoke(order = sampleOutcome)

        assertTrue(result is Resource.Error)
        assertEquals("update fail", (result as Resource.Error).message)
    }

    @Test
    fun `getOutcome returns success`() = runTest {
        whenever(repository.getOutcomeById("1")).thenReturn(Resource.Success(sampleOutcome))

        val result = getOutcomeUseCase.invoke(outcomeID = "1")

        assertTrue(result is Resource.Success)
        assertEquals(sampleOutcome, (result as Resource.Success).data)
    }

    @Test
    fun `getOutcome returns error when null`() = runTest {
        whenever(repository.getOutcomeById("1")).thenReturn(null)

        val result = getOutcomeUseCase.invoke(outcomeID = "1")

        assertTrue(result is Resource.Error)
        assertEquals("Failed after 3 attempts.", (result as Resource.Error).message)
    }

    @Test
    fun `getOutcome returns error`() = runTest {
        whenever(repository.getOutcomeById("1")).thenReturn(Resource.Error("not found"))

        val result = getOutcomeUseCase.invoke(outcomeID = "1")

        assertTrue(result is Resource.Error)
        assertEquals("not found", (result as Resource.Error).message)
    }

    @Test
    fun `getLastOutcomeId returns success`() = runTest {
        whenever(repository.getLastOutcomeId()).thenReturn(Resource.Success("5"))

        val result = getLastOutcomeIdUseCase.invoke()

        assertTrue(result is Resource.Success)
        assertEquals("5", (result as Resource.Success).data)
    }

    @Test
    fun `getLastOutcomeId returns error when null`() = runTest {
        whenever(repository.getLastOutcomeId()).thenReturn(null)

        val result = getLastOutcomeIdUseCase.invoke()

        assertTrue(result is Resource.Error)
        assertEquals("Failed to retrieve ID.", (result as Resource.Error).message)
    }

    @Test
    fun `getLastOutcomeId returns error`() = runTest {
        whenever(repository.getLastOutcomeId()).thenReturn(Resource.Error("id fail"))

        val result = getLastOutcomeIdUseCase.invoke()

        assertTrue(result is Resource.Error)
        assertEquals("id fail", (result as Resource.Error).message)
    }
}
