package com.raylabs.laundryhub.core.data.repository

import com.raylabs.laundryhub.core.data.service.GoogleSheetsAuthorizationManager
import com.raylabs.laundryhub.core.domain.model.sheets.*
import com.raylabs.laundryhub.core.domain.repository.SpreadsheetIdProvider
import com.raylabs.laundryhub.shared.network.api.GoogleSheetsApiClient
import com.raylabs.laundryhub.shared.network.model.sheets.*
import com.raylabs.laundryhub.shared.util.Resource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GoogleSheetRepositoryImplTest {
    private val apiClient: GoogleSheetsApiClient = mock()
    private val authManager: GoogleSheetsAuthorizationManager = mock()
    private val spreadsheetIdProvider: SpreadsheetIdProvider = mock()
    private lateinit var repo: GoogleSheetRepositoryImpl

    @Before
    fun setup() {
        runBlocking {
            whenever(spreadsheetIdProvider.getSpreadsheetId()).thenReturn(TEST_SPREADSHEET_ID)
            whenever(authManager.getAccessToken()).thenReturn(TEST_TOKEN)
        }
        repo = GoogleSheetRepositoryImpl(apiClient, authManager, spreadsheetIdProvider)
    }

    @Test
    fun `readIncomeTransaction returns empty when no data`() = runTest {
        whenever(apiClient.getValues(any(), any(), any())).thenReturn(ValueRange(values = listOf()))

        val result = repo.readIncomeTransaction(FILTER.SHOW_ALL_DATA, null)
        assertTrue(result is Resource.Empty)
    }

    @Test
    fun `readIncomeTransaction returns success with valid data`() = runTest {
        // Header + 1 row
        whenever(apiClient.getValues(any(), any(), any())).thenReturn(
            ValueRange(
                values = listOf(
                    listOf("orderID", "Date", "Name", "Weight", "Price/kg", "Total Price", "(lunas/belum)", "Package", "remark", "payment", "phoneNumber", "due date"),
                    listOf("ORD1", "21/06/2025", "Alice", "5", "1000", "5000", "Lunas", "Regular", "", "Cash", "081", "22/06/2025")
                )
            )
        )
        val result = repo.readIncomeTransaction(FILTER.SHOW_ALL_DATA, null)
        assertTrue(result is Resource.Success)
        val data = (result as Resource.Success).data
        assertEquals(1, data.size)
        assertEquals("ORD1", data[0].orderID)
        assertEquals("Alice", data[0].name)
    }

    @Test
    fun `readGrossData returns success with valid data`() = runTest {
        whenever(apiClient.getValues(any(), any(), any())).thenReturn(
            ValueRange(
                values = listOf(
                    listOf("bulan", "total nominal", "# nota laundry", "pajak"),
                    listOf("Januari", "100000", "10", "1000")
                )
            )
        )
        val result = repo.readGrossData()
        assertTrue(result is Resource.Success)
        val data = (result as Resource.Success).data
        assertEquals(1, data.size)
        assertEquals("Januari", data[0].month)
    }

    @Test
    fun `readSummaryTransaction returns success with valid data`() = runTest {
        whenever(apiClient.getValues(any(), any(), any())).thenReturn(
            ValueRange(
                values = listOf(
                    listOf("Key1", "Value1"),
                    listOf("Key2", "Value2")
                )
            )
        )
        val result = repo.readSummaryTransaction()
        assertTrue(result is Resource.Success)
        val data = (result as Resource.Success).data
        assertEquals(2, data.size)
        assertEquals("Key1", data[0].key)
    }

    @Test
    fun `submitOrder returns success when API call succeeds`() = runTest {
        val order = OrderData("ORD1", "Alice", "081", "Regular", "1000", "5000", "Lunas", "Cash", "", "5", "21/06/2025", "22/06/2025")
        whenever(apiClient.appendValues(any(), any(), any(), any())).thenReturn(AppendValuesResponse(updates = UpdateValuesResponse(updatedRows = 1)))

        val result = repo.addOrder(order)
        assertTrue(result is Resource.Success)
    }

    @Test
    fun `deleteOrder returns success when API call succeeds`() = runTest {
        // Delete order calls getValues to find the row index first
        whenever(apiClient.getValues(any(), any(), any())).thenReturn(
            ValueRange(values = listOf(listOf("orderID"), listOf("ORD1")))
        )
        // Also needs sheetId
        val metadata = """{ "sheets": [ { "properties": { "title": "income", "sheetId": 123 } } ] }"""
        whenever(apiClient.getSpreadsheet(any(), any())).thenReturn(metadata)

        whenever(apiClient.batchUpdate(any(), any(), any())).thenReturn(BatchUpdateSpreadsheetResponse())

        val result = repo.deleteOrder("ORD1")
        assertTrue(result is Resource.Success)
    }

    companion object {
        private const val TEST_SPREADSHEET_ID = "test-id"
        private const val TEST_TOKEN = "test-token"
    }
}
