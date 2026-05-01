package com.raylabs.laundryhub.core.data.repository

import com.google.api.services.sheets.v4.model.ValueRange
import com.raylabs.laundryhub.core.data.service.GoogleSheetService
import com.raylabs.laundryhub.core.domain.model.sheets.CASH
import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.model.sheets.PAID
import com.raylabs.laundryhub.core.domain.model.sheets.UNPAID_ID
import com.raylabs.laundryhub.core.domain.repository.SpreadsheetIdProvider
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

    private lateinit var googleSheetService: GoogleSheetService
    private lateinit var spreadsheetIdProvider: SpreadsheetIdProvider
    private lateinit var repo: GoogleSheetRepositoryImpl

    @Before
    fun setup() {
        googleSheetService = mock()
        spreadsheetIdProvider = mock()
        runBlocking {
            whenever(spreadsheetIdProvider.getSpreadsheetId()).thenReturn(TEST_SPREADSHEET_ID)
        }
        repo = GoogleSheetRepositoryImpl(googleSheetService, spreadsheetIdProvider)
    }

    @Test
    fun `readIncomeTransaction unpaid baseline logs elapsed time for large dataset`() = runTest {
        val rowCount = 5_000
        val unpaidEvery = 3
        val expectedUnpaidCount = rowCount / unpaidEvery

        val sheets = mock<com.google.api.services.sheets.v4.Sheets>()
        val spreadsheets = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets>()
        val values = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values>()
        val get = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Get>()
        val valueRange = mock<ValueRange>()

        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.values()).thenReturn(values)
        whenever(values.get(any(), any())).thenReturn(get)
        whenever(get.execute()).thenReturn(valueRange)
        whenever(valueRange.getValues()).thenReturn(buildIncomeSheetRows(rowCount, unpaidEvery))

        lateinit var result: Resource<List<com.raylabs.laundryhub.core.domain.model.sheets.TransactionData>>
        val elapsedMs = measureTimeMillis {
            result = repo.readIncomeTransaction(FILTER.SHOW_UNPAID_DATA, null)
        }

        assertTrue(result is Resource.Success)
        val data = (result as Resource.Success).data
        assertEquals(expectedUnpaidCount, data.size)
        assertTrue(data.first().orderID.isNotBlank())
        assertTrue(elapsedMs >= 0)

        println(
            "PERF_BASELINE repository=GoogleSheetRepositoryImpl " +
                "method=readIncomeTransaction filter=SHOW_UNPAID_DATA rows=$rowCount " +
                "expected_unpaid=$expectedUnpaidCount elapsed_ms=$elapsedMs"
        )
    }

    private fun buildIncomeSheetRows(rowCount: Int, unpaidEvery: Int): List<List<Any>> {
        val header = listOf(
            "orderID",
            "Date",
            "Name",
            "Weight",
            "Price/kg",
            "Total Price",
            "(lunas/belum)",
            "Package",
            "remark",
            "payment",
            "phoneNumber",
            "due date"
        )

        val rows = (1..rowCount).map { index ->
            val day = ((index - 1) % 28) + 1
            val paymentStatus = if (index % unpaidEvery == 0) UNPAID_ID else PAID
            val paymentMethod = if (paymentStatus == PAID) CASH else ""
            listOf(
                "ORD-$index",
                "%02d/03/2026".format(day),
                "Customer $index",
                "1",
                "7000",
                "7000",
                paymentStatus,
                "Regular",
                "",
                paymentMethod,
                "08$index",
                "%02d/03/2026".format(day + 1)
            )
        }

        return listOf(header) + rows
    }

    companion object {
        private const val TEST_SPREADSHEET_ID = "spreadsheet-id"
    }
}
