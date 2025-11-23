package com.raylabs.laundryhub.core.data.repository

import com.google.api.services.sheets.v4.model.ValueRange
import com.raylabs.laundryhub.core.data.service.GoogleSheetService
import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.ui.common.util.Resource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GoogleSheetRepositoryImplFullTest {

    private lateinit var googleSheetService: GoogleSheetService
    private lateinit var repo: GoogleSheetRepositoryImpl

    @Before
    fun setUp() {
        googleSheetService = mock()
        repo = GoogleSheetRepositoryImpl(googleSheetService)
    }

    @Test
    fun `getOrderById returns transaction when found`() = runTest {
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
        val targetRow = listOf(
            "ORD123",
            "01/07/2025",
            "Alice",
            "2",
            "5000",
            "10000",
            "lunas",
            "Reguler",
            "note",
            "cash",
            "0812",
            "02/07/2025"
        )
        val valueRange = ValueRange().setValues(listOf(header, targetRow))
        val sheets = mock<com.google.api.services.sheets.v4.Sheets>()
        val spreadsheets = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets>()
        val values = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values>()
        val get = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Get>()

        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.values()).thenReturn(values)
        whenever(values.get(any(), any())).thenReturn(get)
        whenever(get.execute()).thenReturn(valueRange)

        when (val result = repo.getOrderById("ORD123")) {
            is Resource.Success -> {
                val data = result.data
                assertEquals("ORD123", data.orderID)
                assertEquals("Alice", data.name)
                assertEquals("cash", data.paymentMethod)
                assertEquals("lunas", data.paymentStatus)
            }

            else -> fail("Expected Success but was $result")
        }
    }

    @Test
    fun `getOrderById returns empty when id not found`() = runTest {
        val header = listOf("orderID")
        val valueRange = ValueRange().setValues(listOf(header))
        val sheets = mock<com.google.api.services.sheets.v4.Sheets>()
        val spreadsheets = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets>()
        val values = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values>()
        val get = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Get>()

        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.values()).thenReturn(values)
        whenever(values.get(any(), any())).thenReturn(get)
        whenever(get.execute()).thenReturn(valueRange)

        val result = repo.getOrderById("ORD123")

        assertTrue(result is Resource.Empty)
    }

    @Test
    fun `readOutcomeTransaction returns sorted success`() = runTest {
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

        val header = listOf("id", "date", "keperluan", "price", "remark", "payment")
        val older = listOf("O1", "01/07/2025", "Snacks", "15000", "old remark", "cash")
        val newer = listOf("O2", "05/07/2025", "Supplies", "20000", "new remark", "qris")
        whenever(valueRange.getValues()).thenReturn(listOf(header, older, newer))

        val result = repo.readOutcomeTransaction()

        assertTrue(result is Resource.Success)
        val data = (result as Resource.Success).data
        assertEquals(2, data.size)
        assertEquals("O2", data.first().id) // sorted by date desc
        assertEquals("Supplies", data.first().purpose)
    }

    @Test
    fun `addOutcome appends values and returns success`() = runTest {
        val sheets = mock<com.google.api.services.sheets.v4.Sheets>()
        val spreadsheets = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets>()
        val values = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values>()
        val append = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Append>()

        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.values()).thenReturn(values)
        whenever(values.append(any(), any(), any())).thenReturn(append)
        whenever(append.setValueInputOption(any())).thenReturn(append)
        whenever(append.execute()).thenReturn(mock())

        val outcome = OutcomeData(
            id = "O10",
            date = "07/07/2025",
            purpose = "Refill",
            price = "50000",
            remark = "Soap",
            payment = "cash"
        )

        val result = repo.addOutcome(outcome)

        assertTrue(result is Resource.Success)
        verify(values).append(
            any(),
            eq("outcome!A1:F"),
            argThat { vr ->
                val updated = vr.getValues().first()
                updated[0] == "O10" && updated[2] == "Refill" && updated[5] == "cash"
            }
        )
        verify(append).setValueInputOption("USER_ENTERED")
        verify(append).execute()
    }

    @Test
    fun `getLastOutcomeId increments from last row`() = runTest {
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

        val header = listOf("id", "date")
        val rows = listOf(
            listOf("1", "01/07/2025"),
            listOf("3", "02/07/2025"),
            listOf("9", "03/07/2025")
        )
        whenever(valueRange.getValues()).thenReturn(listOf(header) + rows)

        val result = repo.getLastOutcomeId()

        assertTrue(result is Resource.Success)
        assertEquals("10", (result as Resource.Success).data)
    }

    @Test
    fun `updateOutcome updates row and preserves existing date`() = runTest {
        val sheets = mock<com.google.api.services.sheets.v4.Sheets>()
        val spreadsheets = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets>()
        val values = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values>()
        val get = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Get>()
        val update = mock<com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Update>()
        val valueRange = mock<ValueRange>()

        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.values()).thenReturn(values)
        whenever(values.get(any(), any())).thenReturn(get)
        whenever(get.execute()).thenReturn(valueRange)
        whenever(values.update(any(), any(), any())).thenReturn(update)
        whenever(update.setValueInputOption(any())).thenReturn(update)
        whenever(update.execute()).thenReturn(mock())

        val header = listOf("id", "date", "keperluan", "price", "remark", "payment")
        val targetRow = listOf("O5", "10/07/2025", "Old", "1000", "remark", "cash")
        whenever(valueRange.getValues()).thenReturn(listOf(header, targetRow))

        val outcome = OutcomeData(
            id = "O5",
            date = "",
            purpose = "Updated Purpose",
            price = "2000",
            remark = "new remark",
            payment = "qris"
        )

        val result = repo.updateOutcome(outcome)

        assertTrue(result is Resource.Success)
        verify(values).update(
            any(),
            eq("outcome!A2:F"),
            argThat { vr ->
                val updated = vr.getValues().first()
                updated[0] == "O5" &&
                        updated[1] == "10/07/2025" && // preserved date
                        updated[2] == "Updated Purpose" &&
                        updated[5] == "qris"
            }
        )
        verify(update).setValueInputOption("USER_ENTERED")
        verify(update).execute()
    }

    @Test
    fun `getOutcomeById returns data when found`() = runTest {
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

        val header = listOf("id", "date", "keperluan", "price", "remark", "payment")
        val row = listOf("O7", "11/07/2025", "Groceries", "30000", "note", "cash")
        whenever(valueRange.getValues()).thenReturn(listOf(header, row))

        val result = repo.getOutcomeById("O7")

        assertTrue(result is Resource.Success)
        val data = (result as Resource.Success).data
        assertEquals("O7", data.id)
        assertEquals("Groceries", data.purpose)
        assertEquals("30000", data.price)
    }

    @Test
    fun `getOutcomeById returns empty when not found`() = runTest {
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

        val header = listOf("id")
        val row = listOf("O9")
        whenever(valueRange.getValues()).thenReturn(listOf(header, row))

        val result = repo.getOutcomeById("O7")

        assertTrue(result is Resource.Empty)
    }
}
