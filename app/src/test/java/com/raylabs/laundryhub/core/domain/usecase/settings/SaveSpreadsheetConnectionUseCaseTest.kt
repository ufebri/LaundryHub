package com.raylabs.laundryhub.core.domain.usecase.settings

import com.raylabs.laundryhub.core.domain.repository.SpreadsheetConfigRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class SaveSpreadsheetConnectionUseCaseTest {

    private val repository: SpreadsheetConfigRepository = mock()

    @Test
    fun `invoke delegates full spreadsheet payload to repository`() = runTest {
        val useCase = SaveSpreadsheetConnectionUseCase(repository)

        useCase(
            spreadsheetId = "sheet-123",
            spreadsheetName = "Laundry A",
            spreadsheetUrl = "https://docs.google.com/spreadsheets/d/sheet-123/edit"
        )

        verify(repository).saveSpreadsheetConnection(
            spreadsheetId = "sheet-123",
            spreadsheetName = "Laundry A",
            spreadsheetUrl = "https://docs.google.com/spreadsheets/d/sheet-123/edit"
        )
    }
}
