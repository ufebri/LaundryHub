package com.raylabs.laundryhub.core.domain.usecase.settings

import com.raylabs.laundryhub.core.domain.model.settings.SpreadsheetConfig
import com.raylabs.laundryhub.core.domain.repository.SpreadsheetConfigRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ObserveSpreadsheetConfigUseCaseTest {

    private val repository: SpreadsheetConfigRepository = mock()

    @Test
    fun `invoke returns repository flow`() = runTest {
        val expected = flowOf(
            SpreadsheetConfig(
                spreadsheetId = "sheet-123",
                spreadsheetName = "Laundry A",
                spreadsheetUrl = "https://docs.google.com/spreadsheets/d/sheet-123/edit"
            )
        )
        whenever(repository.spreadsheetConfig).thenReturn(expected)

        val useCase = ObserveSpreadsheetConfigUseCase(repository)

        assertEquals(expected, useCase())
    }
}
