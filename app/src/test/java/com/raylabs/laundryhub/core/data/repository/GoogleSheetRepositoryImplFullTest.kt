package com.raylabs.laundryhub.core.data.repository

import com.google.api.services.sheets.v4.model.ValueRange
import com.raylabs.laundryhub.core.data.service.GoogleSheetService
import com.raylabs.laundryhub.core.domain.model.sheets.HistoryFilter
import com.raylabs.laundryhub.core.domain.model.sheets.OrderData
import com.raylabs.laundryhub.ui.common.util.Resource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GoogleSheetRepositoryImplFullTest {
    private lateinit var googleSheetService: GoogleSheetService
    private lateinit var repo: GoogleSheetRepositoryImpl
    private lateinit var valueRange: ValueRange

    @Before
    fun setup() {
        googleSheetService = mock()
        valueRange = mock()
        repo = GoogleSheetRepositoryImpl(googleSheetService)
    }

    private fun mockSheets(range: String, values: List<List<Any?>>) {
        val sheets = mock<com.google.api.services.sheets.v4.Sheets>()
        val spreadsheets = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets>()
        val valuesApi = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values>()
        val get = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Get>()
        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.values()).thenReturn(valuesApi)
        whenever(valuesApi.get(any(), eq(range))).thenReturn(get)
        whenever(get.execute()).thenReturn(valueRange)
        whenever(valueRange.getValues()).thenReturn(values)
    }

    @Test
    fun `readSummaryTransaction returns success`() = runTest {
        mockSheets("summary!A2:B", listOf(listOf("key1", "val1"), listOf("key2", "val2")))
        val result = repo.readSummaryTransaction()
        assertTrue(result is Resource.Success)
        val data = (result as Resource.Success).data
        assertEquals(2, data.size)
        assertEquals("key1", data[0].key)
        assertEquals("val1", data[0].value)
    }

    @Test
    fun `readSummaryTransaction returns empty`() = runTest {
        mockSheets("summary!A2:B14", listOf())
        val result = repo.readSummaryTransaction()
        assertTrue(result is Resource.Error || result is Resource.Empty)
    }

    @Test
    fun `readHistoryData returns success`() = runTest {
        mockSheets(
            "history!A1:V", listOf(
                listOf("orderId", "status", "dueDate"),
                listOf("ORD1", "Ready for Pickup", "21/06/2025"),
                listOf("ORD2", "On Progress", "22/06/2025")
            )
        )
        val result = repo.readHistoryData(HistoryFilter.SHOW_ALL_DATA)
        assertTrue(result is Resource.Success)
        val data = (result as Resource.Success).data
        assertEquals(2, data.size)
        assertEquals("", data[1].orderId)
    }

    @Test
    fun `readHistoryData returns empty`() = runTest {
        mockSheets("history!A1:V", listOf())
        val result = repo.readHistoryData(HistoryFilter.SHOW_ALL_DATA)
        assertTrue(result is Resource.Error || result is Resource.Empty)
    }

    @Test
    fun `readPackageData returns success`() = runTest {
        mockSheets(
            "notes!A1:D", listOf(
                listOf("id", "name"), listOf("1", "Reguler"), listOf("2", "Express")
            )
        )
        val result = repo.readPackageData()
        assertTrue(result is Resource.Success)
        val data = (result as Resource.Success).data
        assertEquals(2, data.size)
    }

    @Test
    fun `readPackageData returns empty`() = runTest {
        mockSheets("notes!A1:D", listOf())
        val result = repo.readPackageData()
        assertTrue(result is Resource.Error || result is Resource.Empty)
    }

    @Test
    fun `readOtherPackage returns success`() = runTest {
        mockSheets("income!I2:I", listOf(listOf("Remark1"), listOf("Remark2")))
        val result = repo.readOtherPackage()
        assertTrue(result is Resource.Success)
        val data = (result as Resource.Success).data
        assertEquals(2, data.size)
        assertEquals("Remark1", data[0])
    }

    @Test
    fun `readOtherPackage returns empty`() = runTest {
        mockSheets("income!I2:I", listOf())
        val result = repo.readOtherPackage()
        assertTrue(result is Resource.Error || result is Resource.Empty)
    }

    @Test
    fun `getLastOrderId returns next id`() = runTest {
        mockSheets("income!A2:A", listOf(listOf("orderID"), listOf("1"), listOf("2")))
        val result = repo.getLastOrderId()
        assertTrue(result is Resource.Success)
        val data = (result as Resource.Success).data
        assertEquals("3", data)
    }

    @Test
    fun `getLastOrderId returns zero if no data`() = runTest {
        mockSheets("income!A2:A", listOf(listOf("orderID")))
        val result = repo.getLastOrderId()
        assertTrue(result is Resource.Success)
        val data = (result as Resource.Success).data
        assertEquals("0", data)
    }

    @Test
    fun `addOrder returns success`() = runTest {
        val sheets = mock<com.google.api.services.sheets.v4.Sheets>()
        val spreadsheets = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets>()
        val valuesApi = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values>()
        val append = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Append>()
        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.values()).thenReturn(valuesApi)
        whenever(valuesApi.append(any(), any(), any())).thenReturn(append)
        whenever(append.setValueInputOption(any())).thenReturn(append)
        whenever(append.execute()).thenReturn(mock())
        val order = OrderData(
            "3", "Bob", "0812", "Reguler", "5000", "Cash", "-", "5000", "Paid", "1", "21/06/2025"
        )
        val result = repo.addOrder(order)
        assertTrue(result is Resource.Success)
    }

    @Test
    fun `addOrder handles exception`() = runTest {
        val sheets = mock<com.google.api.services.sheets.v4.Sheets>()
        val spreadsheets = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets>()
        val valuesApi = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values>()
        val append = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Append>()
        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.values()).thenReturn(valuesApi)
        whenever(valuesApi.append(any(), any(), any())).thenReturn(append)
        whenever(append.setValueInputOption(any())).thenReturn(append)
        whenever(append.execute()).thenThrow(RuntimeException("fail"))
        val order = OrderData(
            "3", "Bob", "0812", "Reguler", "5000", "Cash", "-", "5000", "Paid", "1", "21/06/2025"
        )
        val result = repo.addOrder(order)
        assertTrue(result is Resource.Error)
        assertTrue((result as Resource.Error).message.contains("fail"))
    }
}

