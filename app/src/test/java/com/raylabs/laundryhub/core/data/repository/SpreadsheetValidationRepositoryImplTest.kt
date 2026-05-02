package com.raylabs.laundryhub.core.data.repository

import com.raylabs.laundryhub.core.data.service.GoogleSheetsAuthorizationManager
import com.raylabs.laundryhub.core.domain.model.settings.SpreadsheetValidationResult
import com.raylabs.laundryhub.shared.network.api.GoogleSheetsApiClient
import com.raylabs.laundryhub.shared.network.model.sheets.ValueRange
import com.raylabs.laundryhub.shared.util.Resource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SpreadsheetValidationRepositoryImplTest {

    private val apiClient: GoogleSheetsApiClient = mock()
    private val authManager: GoogleSheetsAuthorizationManager = mock()

    @Test
    fun `validateSpreadsheet returns invalid input error when spreadsheet id cannot be parsed`() = runTest {
        val repository = SpreadsheetValidationRepositoryImpl(apiClient, authManager)

        val result = repository.validateSpreadsheet("not-a-sheet")

        assertEquals(
            Resource.Error("Invalid spreadsheet URL or ID."),
            result
        )
    }

    @Test
    fun `validateSpreadsheet succeeds when spreadsheet is readable and has required sheets`() = runTest {
        whenever(authManager.getAccessToken()).thenReturn("token")
        
        // Mock valid metadata JSON
        val metadata = """
            {
              "properties": { "title": "Laundry A" },
              "sheets": [
                { "properties": { "title": "summary" } },
                { "properties": { "title": "gross" } },
                { "properties": { "title": "income" } },
                { "properties": { "title": "notes" } },
                { "properties": { "title": "outcome" } }
              ]
            }
        """.trimIndent()
        whenever(apiClient.getSpreadsheet(any(), any())).thenReturn(metadata)
        
        // Mock valid headers
        whenever(apiClient.getValues(any(), any(), any())).thenReturn(
            ValueRange(values = listOf(listOf("header1", "header2"))) // Simplified for mock
        )
        
        // Mock specific header calls for validation
        whenever(apiClient.getValues(any(), eq("gross!A1:D1"), any())).thenReturn(
            ValueRange(values = listOf(listOf("bulan", "total nominal", "# nota laundry", "pajak")))
        )
        whenever(apiClient.getValues(any(), eq("income!A1:L1"), any())).thenReturn(
            ValueRange(values = listOf(listOf("orderID", "Date", "Name", "Weight", "Price/kg", "Total Price", "(lunas/belum)", "Package", "remark", "payment", "phoneNumber", "due date")))
        )
        whenever(apiClient.getValues(any(), eq("notes!A1:D1"), any())).thenReturn(
            ValueRange(values = listOf(listOf("harga", "packages", "work", "unit")))
        )
        whenever(apiClient.getValues(any(), eq("outcome!A1:F1"), any())).thenReturn(
            ValueRange(values = listOf(listOf("id", "date", "keperluan", "price", "remark", "payment")))
        )

        val repository = SpreadsheetValidationRepositoryImpl(apiClient, authManager)
        val result = repository.validateSpreadsheet(INPUT_URL)

        assertTrue(result is Resource.Success)
        assertEquals(SHEET_ID, (result as Resource.Success).data.spreadsheetId)
        assertEquals("Laundry A", result.data.spreadsheetTitle)
    }

    @Test
    fun `validateSpreadsheet returns missing sheets when template is incomplete`() = runTest {
        whenever(authManager.getAccessToken()).thenReturn("token")
        val incompleteMetadata = """
            {
              "properties": { "title": "Laundry A" },
              "sheets": [
                { "properties": { "title": "summary" } }
              ]
            }
        """.trimIndent()
        whenever(apiClient.getSpreadsheet(any(), any())).thenReturn(incompleteMetadata)

        val repository = SpreadsheetValidationRepositoryImpl(apiClient, authManager)
        val result = repository.validateSpreadsheet(INPUT_URL)

        assertTrue(result is Resource.Error)
        assertTrue((result as Resource.Error).message.contains("Missing sheets"))
    }

    private companion object {
        private const val SHEET_ID = "sheet-123"
        private const val INPUT_URL = "https://docs.google.com/spreadsheets/d/sheet-123/edit"
    }
}
