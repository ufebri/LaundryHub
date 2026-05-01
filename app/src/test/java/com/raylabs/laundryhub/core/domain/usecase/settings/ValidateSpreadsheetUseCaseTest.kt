package com.raylabs.laundryhub.core.domain.usecase.settings

import com.raylabs.laundryhub.core.domain.model.settings.SpreadsheetValidationResult
import com.raylabs.laundryhub.core.domain.repository.SpreadsheetValidationRepository
import com.raylabs.laundryhub.shared.util.Resource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ValidateSpreadsheetUseCaseTest {

    private val repository: SpreadsheetValidationRepository = mock()

    @Test
    fun `invoke delegates validation input to repository`() = runTest {
        val expected = Resource.Success(
            SpreadsheetValidationResult(
                spreadsheetId = "sheet-123",
                spreadsheetTitle = "Laundry A"
            )
        )
        whenever(repository.validateSpreadsheet("sheet-123")).thenReturn(expected)

        val useCase = ValidateSpreadsheetUseCase(repository)

        assertEquals(expected, useCase("sheet-123"))
    }
}
