package com.raylabs.laundryhub.core.data.repository

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.sheets.v4.model.Sheet
import com.google.api.services.sheets.v4.model.SheetProperties
import com.google.api.services.sheets.v4.model.Spreadsheet
import com.google.api.services.sheets.v4.model.ValueRange
import com.raylabs.laundryhub.core.data.service.GoogleSheetService
import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.model.sheets.OrderData
import com.raylabs.laundryhub.core.domain.model.sheets.PackageData
import com.raylabs.laundryhub.core.domain.repository.SpreadsheetIdProvider
import com.raylabs.laundryhub.ui.common.util.Resource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GoogleSheetRepositoryImplTest {
    private lateinit var googleSheetService: GoogleSheetService
    private lateinit var spreadsheetIdProvider: SpreadsheetIdProvider
    private lateinit var repo: GoogleSheetRepositoryImpl
    private lateinit var valueRange: ValueRange

    @Before
    fun setup() {
        googleSheetService = mock()
        spreadsheetIdProvider = mock()
        valueRange = mock()
        runBlocking {
            whenever(spreadsheetIdProvider.getSpreadsheetId()).thenReturn(TEST_SPREADSHEET_ID)
        }
        repo = GoogleSheetRepositoryImpl(googleSheetService, spreadsheetIdProvider)
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
    fun `readGrossData returns success with valid data`() = runTest {
        val sheets = mock<com.google.api.services.sheets.v4.Sheets>()
        val spreadsheets = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets>()
        val values = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values>()
        val get = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Get>()
        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.values()).thenReturn(values)
        whenever(values.get(any(), any())).thenReturn(get)
        whenever(get.execute()).thenReturn(valueRange)
        whenever(valueRange.getValues()).thenReturn(
            listOf(
                listOf("bulan", "total nominal", "# nota laundry", "pajak"),
                listOf("maret", "Rp1.038.150", "35", "Rp5.191")
            )
        )

        val result = repo.readGrossData()
        assertTrue(result is Resource.Success)
        val data = (result as Resource.Success).data
        assertEquals(1, data.size)
        assertEquals("maret", data[0].month)
        assertEquals("Rp1.038.150", data[0].totalNominal)
        assertEquals("35", data[0].orderCount)
        assertEquals("Rp5.191", data[0].tax)
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

    @Test
    fun `updateOrder updates row and preserves date column`() = runTest {
        // Mock GoogleSheet service and all its chained calls
        val sheets = mock<com.google.api.services.sheets.v4.Sheets>()
        val spreadsheets = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets>()
        val valuesApi = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values>()
        val get = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Get>()
        val update = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Update>()

        // Chain .getSheetsService()... etc
        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.values()).thenReturn(valuesApi)
        whenever(valuesApi.get(any(), any())).thenReturn(get)

        // Setup data: header + 2 data row
        val header = listOf("orderID", "Date", "Name", "Weight", "Price/Kg", "Total Price", "Paid Status", "Package", "Remark", "Payment", "Phone", "Due Date")
        val row1 = listOf("ORD1", "15/07/2025", "Alice", "2", "5000", "10000", "Paid", "Reguler", "-", "Cash", "0812", "21/06/2025")
        val row2 = listOf("ORD2", "16/07/2025", "Bob", "1", "8000", "8000", "Unpaid", "Express", "-", "Cash", "0822", "22/06/2025")
        val valueRange = mock<ValueRange>()
        whenever(valueRange.getValues()).thenReturn(listOf(header, row1, row2))
        whenever(get.execute()).thenReturn(valueRange)

        // Mock update
        whenever(valuesApi.update(any(), any(), any())).thenReturn(update)
        whenever(update.setValueInputOption(any())).thenReturn(update)
        whenever(update.execute()).thenReturn(mock())

        // Create repo & test data
        val repo = GoogleSheetRepositoryImpl(googleSheetService, spreadsheetIdProvider)
        val orderData = OrderData(
            orderId = "ORD1",
            name = "Alicia", // changed name!
            phoneNumber = "0813",
            packageName = "Reguler",
            priceKg = "5500",
            totalPrice = "11000",
            paidStatus = "Paid",
            paymentMethod = "Cash",
            remark = "Edited",
            weight = "3",
            orderDate = "15/07/2025",
            dueDate = "25/07/2025"
        )

        val result = repo.updateOrder(orderData)
        assertTrue(result is Resource.Success)

        // Verify updated row dikirim dengan existingDate dari row1[1]
        verify(valuesApi).update(
            any(), // sheet id
            eq("income!A2:L"), // ORD1 is in row 1, +1 header, so index = 0 + 2 = 2
            argThat { valueRangeArg ->
                val updated = valueRangeArg.getValues().first()
                // Check kolom 1 (A) = orderId
                assertEquals("ORD1", updated[0])
                // Check kolom 2 (B) = existing date (preserved!)
                assertEquals("15/07/2025", updated[1])
                // Check nama sudah update
                assertEquals("Alicia", updated[2])
                // Cek kolom lain juga boleh sesuai kebutuhan
                true
            }
        )
    }

    @Test
    fun `deleteOrder removes matching income row via batch update`() = runTest {
        val sheets = mock<com.google.api.services.sheets.v4.Sheets>()
        val spreadsheets = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets>()
        val valuesApi = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values>()
        val getValues = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Get>()
        val getSpreadsheet = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Get>()
        val batchUpdate = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.BatchUpdate>()
        val valueRange = mock<ValueRange>()

        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.values()).thenReturn(valuesApi)
        whenever(valuesApi.get(any(), any())).thenReturn(getValues)
        whenever(getValues.execute()).thenReturn(valueRange)
        whenever(spreadsheets.get(any())).thenReturn(getSpreadsheet)
        whenever(spreadsheets.batchUpdate(any(), any())).thenReturn(batchUpdate)
        whenever(batchUpdate.execute()).thenReturn(mock())

        val header = listOf("orderID", "Date", "Name")
        val row1 = listOf("ORD1", "15/07/2025", "Alice")
        val row2 = listOf("ORD2", "16/07/2025", "Bob")
        whenever(valueRange.getValues()).thenReturn(listOf(header, row1, row2))
        whenever(getSpreadsheet.execute()).thenReturn(
            Spreadsheet().setSheets(
                listOf(
                    Sheet().setProperties(
                        SheetProperties()
                            .setTitle("income")
                            .setSheetId(11)
                    )
                )
            )
        )

        val result = repo.deleteOrder("ORD2")

        assertTrue(result is Resource.Success)
        verify(spreadsheets).batchUpdate(
            eq(TEST_SPREADSHEET_ID),
            argThat { request ->
                val range = request.requests.single().deleteDimension.range
                range.sheetId == 11 &&
                    range.dimension == "ROWS" &&
                    range.startIndex == 2 &&
                    range.endIndex == 3
            }
        )
    }

    @Test
    fun `readSummaryTransaction returns success when data exists`() = runTest {
        val sheets = mock<com.google.api.services.sheets.v4.Sheets>()
        val spreadsheets = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets>()
        val values = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values>()
        val get = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Get>()
        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.values()).thenReturn(values)
        whenever(values.get(any(), any())).thenReturn(get)
        whenever(get.execute()).thenReturn(valueRange)
        whenever(valueRange.getValues()).thenReturn(
            listOf(
                listOf("today_income", "20000"),
                listOf("weekly_income", "50000")
            )
        )

        val result = repo.readSummaryTransaction()
        assertTrue(result is Resource.Success)
        val data = (result as Resource.Success).data
        assertEquals(2, data.size)
        assertEquals("today_income", data[0].key)
        assertEquals("20000", data[0].value)
    }

    @Test
    fun `readSummaryTransaction returns empty when no rows`() = runTest {
        val sheets = mock<com.google.api.services.sheets.v4.Sheets>()
        val spreadsheets = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets>()
        val values = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values>()
        val get = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Get>()
        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.values()).thenReturn(values)
        whenever(values.get(any(), any())).thenReturn(get)
        whenever(get.execute()).thenReturn(valueRange)
        whenever(valueRange.getValues()).thenReturn(emptyList())

        val result = repo.readSummaryTransaction()
        assertTrue(result is Resource.Empty)
    }

    @Test
    fun `readSummaryTransaction returns error on exception`() = runTest {
        val sheets = mock<com.google.api.services.sheets.v4.Sheets>()
        val spreadsheets = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets>()
        val values = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values>()
        val get = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Get>()
        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.values()).thenReturn(values)
        whenever(values.get(any(), any())).thenReturn(get)
        whenever(get.execute()).thenThrow(RuntimeException("boom"))

        val result = repo.readSummaryTransaction()
        assertTrue(result is Resource.Error)
        assertTrue((result as Resource.Error).message.contains("boom"))
    }

    @Test
    fun `readPackageData returns success when rows exist`() = runTest {
        val sheets = mock<com.google.api.services.sheets.v4.Sheets>()
        val spreadsheets = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets>()
        val values = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values>()
        val get = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Get>()
        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.values()).thenReturn(values)
        whenever(values.get(any(), any())).thenReturn(get)

        val header = listOf("packages", "harga", "work", "unit")
        val row1 = listOf("Regular", "5000", "3d", "Kg")
        val row2 = listOf("Express", "8000", "1d", "Kg")
        whenever(get.execute()).thenReturn(valueRange)
        whenever(valueRange.getValues()).thenReturn(listOf(header, row1, row2))

        val result = repo.readPackageData()
        assertTrue(result is Resource.Success)
        val data = (result as Resource.Success).data
        assertEquals(2, data.size)
        assertEquals("Regular", data[0].name)
        assertEquals("5000", data[0].price)
        assertEquals("Express", data[1].name)
        assertEquals(2, data[0].sheetRowIndex)
        assertEquals(3, data[1].sheetRowIndex)
    }

    @Test
    fun `readPackageData returns empty when no rows`() = runTest {
        val sheets = mock<com.google.api.services.sheets.v4.Sheets>()
        val spreadsheets = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets>()
        val values = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values>()
        val get = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Get>()
        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.values()).thenReturn(values)
        whenever(values.get(any(), any())).thenReturn(get)
        whenever(get.execute()).thenReturn(valueRange)
        whenever(valueRange.getValues()).thenReturn(emptyList())

        val result = repo.readPackageData()
        assertTrue(result is Resource.Empty)
    }

    @Test
    fun `readPackageData returns error when sheet read throws`() = runTest {
        val sheets = mock<com.google.api.services.sheets.v4.Sheets>()
        val spreadsheets = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets>()
        val values = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values>()
        val get = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Get>()
        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.values()).thenReturn(values)
        whenever(values.get(any(), any())).thenReturn(get)
        whenever(get.execute()).thenThrow(RuntimeException("package read failed"))

        val result = repo.readPackageData()

        assertTrue(result is Resource.Error)
        assertTrue((result as Resource.Error).message.contains("package read failed"))
    }

    @Test
    fun `readOtherPackage returns success when data exists`() = runTest {
        val sheets = mock<com.google.api.services.sheets.v4.Sheets>()
        val spreadsheets = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets>()
        val values = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values>()
        val get = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Get>()
        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.values()).thenReturn(values)
        whenever(values.get(any(), any())).thenReturn(get)
        whenever(get.execute()).thenReturn(valueRange)
        whenever(valueRange.getValues()).thenReturn(
            listOf(listOf("Note A"), listOf("Note B"))
        )

        val result = repo.readOtherPackage()
        assertTrue(result is Resource.Success)
        val data = (result as Resource.Success).data
        assertEquals(listOf("Note A", "Note B"), data)
    }

    @Test
    fun `addPackage appends package row to notes sheet`() = runTest {
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

        val result = repo.addPackage(
            PackageData(price = "5000", name = "Regular", duration = "3d", unit = "kg")
        )

        assertTrue(result is Resource.Success)
        verify(valuesApi).append(
            eq(TEST_SPREADSHEET_ID),
            eq("notes!A:D"),
            argThat { range ->
                range.getValues() == listOf(listOf("5000", "Regular", "3d", "kg"))
            }
        )
    }

    @Test
    fun `addPackage returns error when append fails`() = runTest {
        val sheets = mock<com.google.api.services.sheets.v4.Sheets>()
        val spreadsheets = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets>()
        val valuesApi = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values>()
        val append = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Append>()

        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.values()).thenReturn(valuesApi)
        whenever(valuesApi.append(any(), any(), any())).thenReturn(append)
        whenever(append.setValueInputOption(any())).thenReturn(append)
        whenever(append.execute()).thenThrow(RuntimeException("append failed"))

        val result = repo.addPackage(
            PackageData(price = "5000", name = "Regular", duration = "3d", unit = "kg")
        )

        assertTrue(result is Resource.Error)
        assertTrue((result as Resource.Error).message.contains("append failed"))
    }

    @Test
    fun `updatePackage updates targeted notes row`() = runTest {
        val sheets = mock<com.google.api.services.sheets.v4.Sheets>()
        val spreadsheets = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets>()
        val valuesApi = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values>()
        val update = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Update>()

        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.values()).thenReturn(valuesApi)
        whenever(valuesApi.update(any(), any(), any())).thenReturn(update)
        whenever(update.setValueInputOption(any())).thenReturn(update)
        whenever(update.execute()).thenReturn(mock())

        val result = repo.updatePackage(
            PackageData(
                price = "8000",
                name = "Express",
                duration = "1d",
                unit = "kg",
                sheetRowIndex = 4
            )
        )

        assertTrue(result is Resource.Success)
        verify(valuesApi).update(
            eq(TEST_SPREADSHEET_ID),
            eq("notes!A4:D"),
            argThat { range ->
                range.getValues() == listOf(listOf("8000", "Express", "1d", "kg"))
            }
        )
    }

    @Test
    fun `updatePackage rejects invalid row index`() = runTest {
        val result = repo.updatePackage(
            PackageData(
                price = "8000",
                name = "Express",
                duration = "1d",
                unit = "kg",
                sheetRowIndex = 1
            )
        )

        assertTrue(result is Resource.Error)
        assertEquals("Package row not found.", (result as Resource.Error).message)
    }

    @Test
    fun `deletePackage removes targeted notes row via batch update`() = runTest {
        val sheets = mock<com.google.api.services.sheets.v4.Sheets>()
        val spreadsheets = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets>()
        val getSpreadsheet = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Get>()
        val batchUpdate = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.BatchUpdate>()

        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.get(any())).thenReturn(getSpreadsheet)
        whenever(spreadsheets.batchUpdate(any(), any())).thenReturn(batchUpdate)
        whenever(batchUpdate.execute()).thenReturn(mock())
        whenever(getSpreadsheet.execute()).thenReturn(
            Spreadsheet().setSheets(
                listOf(
                    Sheet().setProperties(
                        SheetProperties()
                            .setTitle("notes")
                            .setSheetId(13)
                    )
                )
            )
        )

        val result = repo.deletePackage(sheetRowIndex = 4)

        assertTrue(result is Resource.Success)
        verify(spreadsheets).batchUpdate(
            eq(TEST_SPREADSHEET_ID),
            argThat { request ->
                val range = request.requests.single().deleteDimension.range
                range.sheetId == 13 &&
                    range.dimension == "ROWS" &&
                    range.startIndex == 3 &&
                    range.endIndex == 4
            }
        )
    }

    @Test
    fun `deletePackage rejects invalid row index`() = runTest {
        val result = repo.deletePackage(sheetRowIndex = 1)

        assertTrue(result is Resource.Error)
        assertEquals("Package row not found.", (result as Resource.Error).message)
    }

    @Test
    fun `readOtherPackage returns empty when no data`() = runTest {
        val sheets = mock<com.google.api.services.sheets.v4.Sheets>()
        val spreadsheets = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets>()
        val values = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values>()
        val get = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Get>()
        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.values()).thenReturn(values)
        whenever(values.get(any(), any())).thenReturn(get)
        whenever(get.execute()).thenReturn(valueRange)
        whenever(valueRange.getValues()).thenReturn(emptyList())

        val result = repo.readOtherPackage()
        assertTrue(result is Resource.Empty)
    }

    @Test
    fun `getLastOrderId returns success when rows exist`() = runTest {
        val sheets = mock<com.google.api.services.sheets.v4.Sheets>()
        val spreadsheets = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets>()
        val values = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values>()
        val get = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Get>()
        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.values()).thenReturn(values)
        whenever(values.get(any(), any())).thenReturn(get)
        whenever(get.execute()).thenReturn(valueRange)
        whenever(valueRange.getValues()).thenReturn(
            listOf(listOf("10"), listOf("11"))
        )

        val result = repo.getLastOrderId()
        assertTrue(result is Resource.Success)
        assertEquals("12", (result as Resource.Success).data)
    }

    @Test
    fun `getLastOrderId returns error on exception`() = runTest {
        val sheets = mock<com.google.api.services.sheets.v4.Sheets>()
        val spreadsheets = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets>()
        val values = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values>()
        val get = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Get>()
        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.values()).thenReturn(values)
        whenever(values.get(any(), any())).thenReturn(get)
        whenever(get.execute()).thenThrow(RuntimeException("fail get last id"))

        val result = repo.getLastOrderId()
        assertTrue(result is Resource.Error)
        assertTrue((result as Resource.Error).message.contains("fail get last id"))
    }

    @Test
    fun `readSummaryTransaction returns config error when spreadsheet is not connected`() = runTest {
        runBlocking {
            whenever(spreadsheetIdProvider.getSpreadsheetId()).thenReturn(null)
        }

        val result = repo.readSummaryTransaction()

        assertTrue(result is Resource.Error)
        assertEquals(
            "Spreadsheet not connected yet. Please complete setup first.",
            (result as Resource.Error).message
        )
    }

    private companion object {
        const val TEST_SPREADSHEET_ID = "sheet-test-id"
    }
}
