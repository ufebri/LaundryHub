package com.raylabs.laundryhub.core.data.repository

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.sheets.v4.model.ValueRange
import com.raylabs.laundryhub.core.data.service.GoogleSheetService
import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.ui.common.util.Resource
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private lateinit var googleSheetService: GoogleSheetService
    private lateinit var repo: GoogleSheetRepositoryImpl
    private lateinit var valueRange: ValueRange

    @Before
    fun setup() {
        googleSheetService = mock()
        valueRange = mock()
        repo = GoogleSheetRepositoryImpl(googleSheetService)
    }

    @Test
    fun `readIncomeTransaction returns empty when no data`() = runTest {
        val sheets = mock<com.google.api.services.sheets.v4.Sheets>()
        val spreadsheets = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets>()
        val values = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values>()
        val get = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Get>()
        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.values()).thenReturn(values)
        whenever(values.get(any(), any())).thenReturn(get)
        whenever(get.execute()).thenReturn(valueRange)
        whenever(valueRange.getValues()).thenReturn(listOf())

        val result = repo.readIncomeTransaction(FILTER.SHOW_ALL_DATA, null)
        assertTrue(result is Resource.Empty)
    }

    @Test
    fun `readIncomeTransaction returns success with valid data`() = runTest {
        val sheets = mock<com.google.api.services.sheets.v4.Sheets>()
        val spreadsheets = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets>()
        val values = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values>()
        val get = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Get>()
        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.values()).thenReturn(values)
        whenever(values.get(any(), any())).thenReturn(get)
        whenever(get.execute()).thenReturn(valueRange)
        // Header + 1 row
        whenever(valueRange.getValues()).thenReturn(
            listOf(
                listOf("orderID", "Date", "Name", "Total Price"),
                listOf("ORD1", "21/06/2025", "Alice", "10000")
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
    fun `readIncomeTransaction handles GoogleJsonResponseException`() = runTest {
        val sheets = mock<com.google.api.services.sheets.v4.Sheets>()
        val spreadsheets = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets>()
        val values = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values>()
        val get = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Get>()
        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.values()).thenReturn(values)
        whenever(values.get(any(), any())).thenReturn(get)
        val exception = mock<GoogleJsonResponseException>()
        whenever(get.execute()).thenThrow(exception)
        whenever(exception.statusCode).thenReturn(404)
        whenever(exception.statusMessage).thenReturn("Not Found")
        whenever(exception.details).thenReturn(null)

        val result = repo.readIncomeTransaction(FILTER.SHOW_ALL_DATA, null)
        assertTrue(result is Resource.Error)
        assertTrue((result as Resource.Error).message.contains("Error 404"))
    }

    @Test
    fun `readIncomeTransaction handles Exception`() = runTest {
        val sheets = mock<com.google.api.services.sheets.v4.Sheets>()
        val spreadsheets = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets>()
        val values = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values>()
        val get = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Get>()
        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.values()).thenReturn(values)
        whenever(values.get(any(), any())).thenReturn(get)
        whenever(get.execute()).thenThrow(RuntimeException("fail"))

        val result = repo.readIncomeTransaction(FILTER.SHOW_ALL_DATA, null)
        assertTrue(result is Resource.Error)
        assertTrue((result as Resource.Error).message.contains("fail"))
    }
}

