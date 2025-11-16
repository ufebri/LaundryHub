package com.raylabs.laundryhub.core.data.repository

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.sheets.v4.Sheets
import com.google.api.client.http.HttpHeaders
import com.google.api.client.http.HttpResponseException
import com.google.api.services.sheets.v4.model.ValueRange
import com.raylabs.laundryhub.core.data.service.GoogleSheetService
import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.model.sheets.OrderData
import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.ui.common.util.Resource
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
        val sheets = mock<Sheets>()
        val spreadsheets = mock<Sheets.Spreadsheets>()
        val values = mock<Sheets.Spreadsheets.Values>()
        val get = mock<Sheets.Spreadsheets.Values.Get>()
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
        val sheets = mock<Sheets>()
        val spreadsheets = mock<Sheets.Spreadsheets>()
        val values = mock<Sheets.Spreadsheets.Values>()
        val get = mock<Sheets.Spreadsheets.Values.Get>()
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
        val sheets = mock<Sheets>()
        val spreadsheets = mock<Sheets.Spreadsheets>()
        val values = mock<Sheets.Spreadsheets.Values>()
        val get = mock<Sheets.Spreadsheets.Values.Get>()
        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.values()).thenReturn(values)
        whenever(values.get(any(), any())).thenReturn(get)
        val exception = GoogleJsonResponseException(
            HttpResponseException.Builder(404, "Not Found", HttpHeaders()),
            null
        )
        whenever(get.execute()).thenThrow(exception)

        val result = repo.readIncomeTransaction(FILTER.SHOW_ALL_DATA, null)
        assertTrue(result is Resource.Error)
        assertTrue((result as Resource.Error).message.contains("Error 404"))
    }

    @Test
    fun `readIncomeTransaction handles Exception`() = runTest {
        val sheets = mock<Sheets>()
        val spreadsheets = mock<Sheets.Spreadsheets>()
        val values = mock<Sheets.Spreadsheets.Values>()
        val get = mock<Sheets.Spreadsheets.Values.Get>()
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
        val sheets = mock<Sheets>()
        val spreadsheets = mock<Sheets.Spreadsheets>()
        val valuesApi = mock<Sheets.Spreadsheets.Values>()
        val get = mock<Sheets.Spreadsheets.Values.Get>()
        val update = mock<Sheets.Spreadsheets.Values.Update>()

        // Chain .getSheetsService()... etc
        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.values()).thenReturn(valuesApi)
        whenever(valuesApi.get(any(), any())).thenReturn(get)

        // Setup data: header + 2 data row
        val header = listOf(
            "orderID",
            "Date",
            "Name",
            "Weight",
            "Price/Kg",
            "Total Price",
            "Paid Status",
            "Package",
            "Remark",
            "Payment",
            "Phone",
            "Due Date"
        )
        val row1 = listOf(
            "ORD1",
            "15/07/2025",
            "Alice",
            "2",
            "5000",
            "10000",
            "Paid",
            "Reguler",
            "-",
            "Cash",
            "0812",
            "21/06/2025"
        )
        val row2 = listOf(
            "ORD2",
            "16/07/2025",
            "Bob",
            "1",
            "8000",
            "8000",
            "Unpaid",
            "Express",
            "-",
            "Cash",
            "0822",
            "22/06/2025"
        )
        val valueRange = mock<ValueRange>()
        whenever(valueRange.getValues()).thenReturn(listOf(header, row1, row2))
        whenever(get.execute()).thenReturn(valueRange)

        // Mock update
        whenever(valuesApi.update(any(), any(), any())).thenReturn(update)
        whenever(update.setValueInputOption(any())).thenReturn(update)
        whenever(update.execute()).thenReturn(mock())

        // Create repo & test data
        val repo = GoogleSheetRepositoryImpl(googleSheetService)
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
    fun `readOutcomeTransactions returns mapped data`() = runTest {
        val sheets = mock<Sheets>()
        val spreadsheets = mock<Sheets.Spreadsheets>()
        val values = mock<Sheets.Spreadsheets.Values>()
        val get = mock<Sheets.Spreadsheets.Values.Get>()
        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.values()).thenReturn(values)
        whenever(values.get(any(), any())).thenReturn(get)
        whenever(get.execute()).thenReturn(valueRange)

        whenever(valueRange.getValues()).thenReturn(
            listOf(
                listOf("id", "date", "keperluan", "price", "remark", "payment"),
                listOf("12", "01/04/2025", "gas 3kg", "Rp23.000", "toko depan", "cash")
            )
        )

        val result = repo.readOutcomeTransactions()
        assertTrue(result is Resource.Success)
        val data = (result as Resource.Success).data
        assertEquals(1, data.size)
        assertEquals("12", data[0].id)
        assertEquals("gas 3kg", data[0].purpose)
    }

    @Test
    fun `addOutcome appends new row`() = runTest {
        val sheets = mock<Sheets>()
        val spreadsheets = mock<Sheets.Spreadsheets>()
        val valuesApi = mock<Sheets.Spreadsheets.Values>()
        val append = mock<Sheets.Spreadsheets.Values.Append>()
        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.values()).thenReturn(valuesApi)
        whenever(valuesApi.append(any(), any(), any())).thenReturn(append)
        whenever(append.setValueInputOption(any())).thenReturn(append)
        whenever(append.execute()).thenReturn(mock())

        val outcome = OutcomeData(
            id = "15",
            date = "05/04/2025",
            purpose = "hanger",
            price = "Rp28.000",
            remark = "toko depan",
            payment = "cash"
        )

        val result = repo.addOutcome(outcome)
        assertTrue(result is Resource.Success)

        verify(valuesApi).append(
            any(),
            eq("outcome!A1:F"),
            argThat { body ->
                val valuesList = body.getValues().first()
                assertEquals(
                    listOf("15", "05/04/2025", "hanger", "Rp28.000", "toko depan", "cash"),
                    valuesList
                )
                true
            }
        )
    }

    @Test
    fun `getOrderById handles exception`() = runTest {
        val sheets = mock<Sheets>()
        val spreadsheets = mock<Sheets.Spreadsheets>()
        val valuesApi = mock<Sheets.Spreadsheets.Values>()
        val get = mock<Sheets.Spreadsheets.Values.Get>()
        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.values()).thenReturn(valuesApi)
        whenever(valuesApi.get(any(), any())).thenReturn(get)
        whenever(get.execute()).thenThrow(RuntimeException("boom order"))

        val result = repo.getOrderById("ORD100")
        assertTrue(result is Resource.Error)
        assertTrue((result as Resource.Error).message.contains("boom order"))
    }

    @Test
    fun `updateOrder returns error when order id not found`() = runTest {
        val sheets = mock<Sheets>()
        val spreadsheets = mock<Sheets.Spreadsheets>()
        val valuesApi = mock<Sheets.Spreadsheets.Values>()
        val get = mock<Sheets.Spreadsheets.Values.Get>()
        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.values()).thenReturn(valuesApi)
        whenever(valuesApi.get(any(), any())).thenReturn(get)
        whenever(get.execute()).thenReturn(
            valueRangeOf(
                listOf("orderID", "Date"),
                listOf("ORD1", "01/01/2025")
            )
        )

        val order = OrderData(
            orderId = "ORD99",
            name = "",
            phoneNumber = "",
            packageName = "",
            priceKg = "",
            totalPrice = "",
            paidStatus = "",
            paymentMethod = "",
            remark = "",
            weight = "",
            orderDate = "",
            dueDate = ""
        )
        val result = repo.updateOrder(order)
        assertTrue(result is Resource.Error)
        assertTrue((result as Resource.Error).message.contains("Order ID not found"))
    }

    @Test
    fun `updateOrder uses fallback existing date when order date blank`() = runTest {
        val sheets = mock<Sheets>()
        val spreadsheets = mock<Sheets.Spreadsheets>()
        val valuesApi = mock<Sheets.Spreadsheets.Values>()
        val get = mock<Sheets.Spreadsheets.Values.Get>()
        val update = mock<Sheets.Spreadsheets.Values.Update>()
        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.values()).thenReturn(valuesApi)
        whenever(valuesApi.get(any(), any())).thenReturn(get)
        whenever(valuesApi.update(any(), any(), any())).thenReturn(update)
        whenever(update.setValueInputOption(any())).thenReturn(update)
        whenever(update.execute()).thenReturn(mock())
        whenever(get.execute()).thenReturn(
            valueRangeOf(
                listOf("orderID", "Date"),
                listOf("ORD1", "12/05/2025")
            )
        )

        val order = OrderData(
            orderId = "ORD1",
            name = "",
            phoneNumber = "",
            packageName = "",
            priceKg = "",
            totalPrice = "",
            paidStatus = "",
            paymentMethod = "",
            remark = "",
            weight = "",
            orderDate = "",
            dueDate = ""
        )
        repo.updateOrder(order)

        verify(valuesApi).update(
            any(),
            eq("income!A2:L"),
            argThat { body ->
                val updatedRow = body.getValues().first()
                assertEquals("12/05/2025", updatedRow[1])
                true
            }
        )
    }

    @Test
    fun `updateOrder handles exception`() = runTest {
        val sheets = mock<Sheets>()
        val spreadsheets = mock<Sheets.Spreadsheets>()
        val valuesApi = mock<Sheets.Spreadsheets.Values>()
        val get = mock<Sheets.Spreadsheets.Values.Get>()
        val update = mock<Sheets.Spreadsheets.Values.Update>()
        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.values()).thenReturn(valuesApi)
        whenever(valuesApi.get(any(), any())).thenReturn(get)
        whenever(get.execute()).thenReturn(
            valueRangeOf(
                listOf("orderID", "Date"),
                listOf("ORD1", "01/01/2025")
            )
        )
        whenever(valuesApi.update(any(), any(), any())).thenReturn(update)
        whenever(update.setValueInputOption(any())).thenReturn(update)
        whenever(update.execute()).thenThrow(RuntimeException("boom"))

        val order = OrderData(
            orderId = "ORD1",
            name = "",
            phoneNumber = "",
            packageName = "",
            priceKg = "",
            totalPrice = "",
            paidStatus = "",
            paymentMethod = "",
            remark = "",
            weight = "",
            orderDate = "",
            dueDate = ""
        )

        val result = repo.updateOrder(order)
        assertTrue(result is Resource.Error)
        assertTrue((result as Resource.Error).message.contains("boom"))
    }

    @Test
    fun `readOutcomeTransactions handles GoogleJsonResponseException`() = runTest {
        val sheets = mock<Sheets>()
        val spreadsheets = mock<Sheets.Spreadsheets>()
        val valuesApi = mock<Sheets.Spreadsheets.Values>()
        val get = mock<Sheets.Spreadsheets.Values.Get>()
        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.values()).thenReturn(valuesApi)
        whenever(valuesApi.get(any(), any())).thenReturn(get)

        val exception = GoogleJsonResponseException(
            HttpResponseException.Builder(400, "Bad Request", HttpHeaders()),
            null
        )
        whenever(get.execute()).thenThrow(exception)

        val result = repo.readOutcomeTransactions()
        assertTrue(result is Resource.Error)
        assertTrue((result as Resource.Error).message.contains("Error 400"))
    }

    @Test
    fun `readOutcomeTransactions handles exception`() = runTest {
        val sheets = mock<Sheets>()
        val spreadsheets = mock<Sheets.Spreadsheets>()
        val valuesApi = mock<Sheets.Spreadsheets.Values>()
        val get = mock<Sheets.Spreadsheets.Values.Get>()
        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.values()).thenReturn(valuesApi)
        whenever(valuesApi.get(any(), any())).thenReturn(get)
        whenever(get.execute()).thenThrow(RuntimeException("fail outcome"))

        val result = repo.readOutcomeTransactions()
        assertTrue(result is Resource.Error)
        assertTrue((result as Resource.Error).message.contains("fail outcome"))
    }

    @Test
fun `addOutcome handles exception`() = runTest {
        val sheets = mock<Sheets>()
        val spreadsheets = mock<Sheets.Spreadsheets>()
        val valuesApi = mock<Sheets.Spreadsheets.Values>()
        val append = mock<Sheets.Spreadsheets.Values.Append>()
        whenever(googleSheetService.getSheetsService()).thenReturn(sheets)
        whenever(sheets.spreadsheets()).thenReturn(spreadsheets)
        whenever(spreadsheets.values()).thenReturn(valuesApi)
        whenever(valuesApi.append(any(), any(), any())).thenReturn(append)
        whenever(append.setValueInputOption(any())).thenReturn(append)
        whenever(append.execute()).thenThrow(RuntimeException("append fail"))

        val outcome = OutcomeData("1", "", "", "", "", "")
        val result = repo.addOutcome(outcome)
        assertTrue(result is Resource.Error)
        assertTrue((result as Resource.Error).message.contains("append fail"))
    }
}

private fun valueRangeOf(vararg rows: List<String>): ValueRange {
    val data = rows.map { row ->
        row.map { it as Any }.toMutableList()
    }.toMutableList()
    return ValueRange().setValues(data)
}
