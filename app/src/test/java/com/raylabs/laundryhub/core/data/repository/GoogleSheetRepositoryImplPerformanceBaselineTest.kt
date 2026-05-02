package com.raylabs.laundryhub.core.data.repository

import com.raylabs.laundryhub.core.data.service.GoogleSheetsAuthorizationManager
import com.raylabs.laundryhub.core.domain.model.sheets.*
import com.raylabs.laundryhub.core.domain.repository.SpreadsheetIdProvider
import com.raylabs.laundryhub.shared.network.api.GoogleSheetsApiClient
import com.raylabs.laundryhub.shared.network.model.sheets.ValueRange
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
import kotlin.system.measureTimeMillis

@OptIn(ExperimentalCoroutinesApi::class)
class GoogleSheetRepositoryImplPerformanceBaselineTest {

    private val apiClient: GoogleSheetsApiClient = mock()
    private val authManager: GoogleSheetsAuthorizationManager = mock()
    private val spreadsheetIdProvider: SpreadsheetIdProvider = mock()
    private lateinit var repo: GoogleSheetRepositoryImpl

    @Before
    fun setup() {
        runBlocking {
            whenever(spreadsheetIdProvider.getSpreadsheetId()).thenReturn(TEST_SPREADSHEET_ID)
            whenever(authManager.getAccessToken()).thenReturn("token")
        }
        repo = GoogleSheetRepositoryImpl(apiClient, authManager, spreadsheetIdProvider)
    }

    @Test
    fun `readIncomeTransaction unpaid baseline logs elapsed time for large dataset`() = runTest {
        val rowCount = 500
        val unpaidEvery = 3
        val expectedUnpaidCount = rowCount / unpaidEvery

        whenever(apiClient.getValues(any(), any(), any())).thenReturn(
            ValueRange(values = buildIncomeSheetRows(rowCount, unpaidEvery))
        )

        lateinit var result: Resource<List<TransactionData>>
        val elapsedMs = measureTimeMillis {
            result = repo.readIncomeTransaction(FILTER.SHOW_UNPAID_DATA, null)
        }

        assertTrue(result is Resource.Success)
        val data = (result as Resource.Success).data
        assertEquals(expectedUnpaidCount, data.size)
        assertTrue(elapsedMs >= 0)
    }

    private fun buildIncomeSheetRows(rowCount: Int, unpaidEvery: Int): List<List<String>> {
        val header = listOf("orderID", "Date", "Name", "Weight", "Price/kg", "Total Price", "(lunas/belum)", "Package", "remark", "payment", "phoneNumber", "due date")
        val rows = (1..rowCount).map { index ->
            val paymentStatus = if (index % unpaidEvery == 0) UNPAID_ID else PAID
            listOf("ORD-$index", "01/01/2026", "Customer $index", "1", "7000", "7000", paymentStatus, "Regular", "", "Cash", "081", "02/01/2026")
        }
        return listOf(header) + rows
    }

    companion object {
        private const val TEST_SPREADSHEET_ID = "spreadsheet-id"
    }
}
