package com.raylabs.laundryhub.backend.service

import com.raylabs.laundryhub.core.domain.model.sheets.OrderData
import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.core.domain.model.sheets.PackageData
import com.raylabs.laundryhub.core.domain.model.sheets.GrossData
import com.raylabs.laundryhub.core.domain.model.sheets.SpreadsheetData
import com.raylabs.laundryhub.core.domain.model.sheets.toSheetValues
import com.raylabs.laundryhub.backend.db.repository.SyncDeleteEvent
import com.raylabs.laundryhub.backend.db.repository.SyncEntityType
import com.raylabs.laundryhub.shared.network.api.GoogleSheetsApiClient
import com.raylabs.laundryhub.shared.network.model.sheets.AppendValuesResponse
import com.raylabs.laundryhub.shared.network.model.sheets.BatchClearValuesResponse
import com.raylabs.laundryhub.shared.network.model.sheets.BatchClearValuesRequest
import com.raylabs.laundryhub.shared.network.model.sheets.BatchUpdateValuesRequest
import com.raylabs.laundryhub.shared.network.model.sheets.BatchUpdateValuesResponse
import com.raylabs.laundryhub.shared.network.model.sheets.UpdateValuesResponse
import com.raylabs.laundryhub.shared.network.model.sheets.ValueRange
import com.raylabs.laundryhub.backend.util.syncVerificationSignature
import kotlinx.coroutines.runBlocking
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.mockito.kotlin.doReturn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SheetsSyncServiceTest {

    @Test
    fun `acknowledgedUpdateKeys returns keys with updated rows or cells`() {
        val acknowledged = acknowledgedUpdateKeys(
            keys = listOf("1674", "1675", "1676"),
            response = BatchUpdateValuesResponse(
                responses = listOf(
                    UpdateValuesResponse(updatedRows = 1, updatedCells = 12),
                    UpdateValuesResponse(updatedRows = 0, updatedCells = 0),
                    UpdateValuesResponse(updatedRows = null, updatedCells = 12)
                )
            )
        )

        assertEquals(listOf("1674", "1676"), acknowledged)
    }

    @Test
    fun `acknowledgedUpdateKeys returns empty when Sheets reports no updated rows or cells`() {
        val acknowledged = acknowledgedUpdateKeys(
            keys = listOf("1674"),
            response = BatchUpdateValuesResponse(
                responses = listOf(UpdateValuesResponse(updatedRows = 0, updatedCells = 0))
            )
        )

        assertEquals(emptyList(), acknowledged)
    }

    @Test
    fun `acknowledgedAppendKeys returns all keys when append covers every row`() {
        val acknowledged = acknowledgedAppendKeys(
            keys = listOf("1674", "1675"),
            response = AppendValuesResponse(
                updates = UpdateValuesResponse(updatedRows = 2, updatedCells = 24)
            )
        )

        assertEquals(listOf("1674", "1675"), acknowledged)
    }

    @Test
    fun `acknowledgedAppendKeys returns empty when append response does not cover every row`() {
        val acknowledged = acknowledgedAppendKeys(
            keys = listOf("1674", "1675"),
            response = AppendValuesResponse(
                updates = UpdateValuesResponse(updatedRows = 1, updatedCells = 12)
            )
        )

        assertEquals(emptyList(), acknowledged)
    }

    @Test
    fun `syncOrder appends when order is not found`() = runBlocking {
        val mockClient: GoogleSheetsApiClient = mock()
        val service = spy(SheetsSyncService())
        doReturn("fake-token").whenever(service).getServiceAccountToken()

        val field = SheetsSyncService::class.java.getDeclaredField("sheetsApiClient")
        field.isAccessible = true
        field.set(service, mockClient)

        whenever(mockClient.getValues(eq("sheet-id"), eq("income!A:A"), eq("fake-token")))
            .thenReturn(ValueRange(values = emptyList()))

        val order = OrderData(
            orderId = "ORD-1",
            orderDate = "10/05/2026",
            name = "John Doe",
            weight = "5",
            priceKg = "10000",
            totalPrice = "50000",
            paidStatus = "Lunas",
            packageName = "Cuci Setrika",
            remark = "",
            paymentMethod = "Cash",
            phoneNumber = "",
            dueDate = ""
        )

        val success = service.syncOrder("sheet-id", order)
        assertTrue(success)
    }

    @Test
    fun `syncOrder updates when order exists`() = runBlocking {
        val mockClient: GoogleSheetsApiClient = mock()
        val service = spy(SheetsSyncService())
        doReturn("fake-token").whenever(service).getServiceAccountToken()

        val field = SheetsSyncService::class.java.getDeclaredField("sheetsApiClient")
        field.isAccessible = true
        field.set(service, mockClient)

        whenever(mockClient.getValues(eq("sheet-id"), eq("income!A:A"), eq("fake-token")))
            .thenReturn(ValueRange(values = listOf(listOf("header"), listOf("ORD-1"))))

        val order = OrderData(
            orderId = "ORD-1",
            orderDate = "10/05/2026",
            name = "John Doe",
            weight = "5",
            priceKg = "10000",
            totalPrice = "50000",
            paidStatus = "Lunas",
            packageName = "Cuci Setrika",
            remark = "",
            paymentMethod = "Cash",
            phoneNumber = "",
            dueDate = ""
        )

        val success = service.syncOrder("sheet-id", order)
        assertTrue(success)
    }

    @Test
    fun `deleteOrderFromSheet clears row when order exists`() = runBlocking {
        val mockClient: GoogleSheetsApiClient = mock()
        val service = spy(SheetsSyncService())
        doReturn("fake-token").whenever(service).getServiceAccountToken()

        val field = SheetsSyncService::class.java.getDeclaredField("sheetsApiClient")
        field.isAccessible = true
        field.set(service, mockClient)

        whenever(mockClient.getValues(eq("sheet-id"), eq("income!A:A"), eq("fake-token")))
            .thenReturn(ValueRange(values = listOf(listOf("header"), listOf("ORD-1"))))

        val success = service.deleteOrderFromSheet("sheet-id", "ORD-1")
        assertTrue(success)
    }

    @Test
    fun `syncOutcome appends when outcome is not found`() = runBlocking {
        val mockClient: GoogleSheetsApiClient = mock()
        val service = spy(SheetsSyncService())
        doReturn("fake-token").whenever(service).getServiceAccountToken()

        val field = SheetsSyncService::class.java.getDeclaredField("sheetsApiClient")
        field.isAccessible = true
        field.set(service, mockClient)

        whenever(mockClient.getValues(eq("sheet-id"), eq("outcome!A:A"), eq("fake-token")))
            .thenReturn(ValueRange(values = emptyList()))

        val outcome = OutcomeData("1", "10/05/2026", "Soap", "50000", "Beli sabun", "Cash")
        val success = service.syncOutcome("sheet-id", outcome)
        assertTrue(success)
    }

    @Test
    fun `syncPackage appends when package is not found`() = runBlocking {
        val mockClient: GoogleSheetsApiClient = mock()
        val service = spy(SheetsSyncService())
        doReturn("fake-token").whenever(service).getServiceAccountToken()

        val field = SheetsSyncService::class.java.getDeclaredField("sheetsApiClient")
        field.isAccessible = true
        field.set(service, mockClient)

        whenever(mockClient.getValues(eq("sheet-id"), eq("notes!B:B"), eq("fake-token")))
            .thenReturn(ValueRange(values = emptyList()))

        val pkg = PackageData(price = "10000", name = "Cuci Setrika", duration = "24", unit = "Kg")
        val success = service.syncPackage("sheet-id", pkg)
        assertTrue(success)
    }

    @Test
    fun `syncGross syncs successfully`() = runBlocking {
        val mockClient: GoogleSheetsApiClient = mock()
        val service = spy(SheetsSyncService())
        doReturn("fake-token").whenever(service).getServiceAccountToken()

        val field = SheetsSyncService::class.java.getDeclaredField("sheetsApiClient")
        field.isAccessible = true
        field.set(service, mockClient)

        whenever(mockClient.getValues(eq("sheet-id"), eq("gross!A:A"), eq("fake-token")))
            .thenReturn(ValueRange(values = emptyList()))
        whenever(mockClient.appendValues(eq("sheet-id"), eq("gross"), any(), eq("fake-token")))
            .thenReturn(AppendValuesResponse(updates = UpdateValuesResponse(updatedRows = 1, updatedCells = 4)))

        val gross = GrossData(month = "Mei 2026", totalNominal = "Rp3.343.000", orderCount = "115", tax = "Rp16.715")
        val success = service.syncGross("sheet-id", gross)
        assertTrue(success)
    }

    @Test
    fun `syncSummary syncs successfully`() = runBlocking {
        val mockClient: GoogleSheetsApiClient = mock()
        val service = spy(SheetsSyncService())
        doReturn("fake-token").whenever(service).getServiceAccountToken()

        val field = SheetsSyncService::class.java.getDeclaredField("sheetsApiClient")
        field.isAccessible = true
        field.set(service, mockClient)

        whenever(mockClient.getValues(eq("sheet-id"), eq("summary!A:A"), eq("fake-token")))
            .thenReturn(ValueRange(values = emptyList()))
        whenever(mockClient.appendValues(eq("sheet-id"), eq("summary"), any(), eq("fake-token")))
            .thenReturn(AppendValuesResponse(updates = UpdateValuesResponse(updatedRows = 1, updatedCells = 2)))

        val summary = SpreadsheetData("key", "value")
        val success = service.syncSummary("sheet-id", summary)
        assertTrue(success)
    }

    @Test
    fun `syncAndVerifyOrdersBatch syncs and verifies successfully`() = runBlocking {
        val mockClient: GoogleSheetsApiClient = mock()
        val service = spy(SheetsSyncService())
        doReturn("fake-token").whenever(service).getServiceAccountToken()

        val field = SheetsSyncService::class.java.getDeclaredField("sheetsApiClient")
        field.isAccessible = true
        field.set(service, mockClient)

        // Mock sheetsApiClient syncBatch calls
        whenever(mockClient.getValues(eq("sheet-id"), eq("income!A:A"), eq("fake-token")))
            .thenReturn(ValueRange(values = listOf(listOf("header"), listOf("ORD-1"))))
        whenever(mockClient.batchUpdateValues(eq("sheet-id"), any(), eq("fake-token")))
            .thenReturn(BatchUpdateValuesResponse(responses = listOf(UpdateValuesResponse(updatedRows = 1, updatedCells = 12))))

        // Mock fetchOrdersFromSheet for read-back verification
        val order1 = OrderData(
            orderId = "ORD-1", orderDate = "10/05/2026", name = "John", weight = "5", priceKg = "10000",
            totalPrice = "50000", paidStatus = "Lunas", packageName = "Cuci", remark = "", paymentMethod = "Cash",
            phoneNumber = "", dueDate = ""
        )
        whenever(mockClient.getValues(eq("sheet-id"), eq("income!A2:L"), eq("fake-token")))
            .thenReturn(ValueRange(values = listOf(order1.toSheetValues().single())))

        val result = service.syncAndVerifyOrdersBatch("sheet-id", listOf(order1))
        assertEquals(listOf("ORD-1"), result)
    }

    @Test
    fun `syncAndVerifyOutcomesBatch syncs and verifies successfully`() = runBlocking {
        val mockClient: GoogleSheetsApiClient = mock()
        val service = spy(SheetsSyncService())
        doReturn("fake-token").whenever(service).getServiceAccountToken()

        val field = SheetsSyncService::class.java.getDeclaredField("sheetsApiClient")
        field.isAccessible = true
        field.set(service, mockClient)

        whenever(mockClient.getValues(eq("sheet-id"), eq("outcome!A:A"), eq("fake-token")))
            .thenReturn(ValueRange(values = listOf(listOf("header"), listOf("1"))))
        whenever(mockClient.batchUpdateValues(eq("sheet-id"), any(), eq("fake-token")))
            .thenReturn(BatchUpdateValuesResponse(responses = listOf(UpdateValuesResponse(updatedRows = 1))))

        val outcome1 = OutcomeData("1", "10/05/2026", "Soap", "50000", "Beli", "Cash")
        whenever(mockClient.getValues(eq("sheet-id"), eq("outcome!A2:F"), eq("fake-token")))
            .thenReturn(ValueRange(values = listOf(outcome1.toSheetValues().single())))

        val result = service.syncAndVerifyOutcomesBatch("sheet-id", listOf(outcome1))
        assertEquals(listOf("1"), result)
    }

    @Test
    fun `syncAndVerifyPackagesBatch syncs and verifies successfully`() = runBlocking {
        val mockClient: GoogleSheetsApiClient = mock()
        val service = spy(SheetsSyncService())
        doReturn("fake-token").whenever(service).getServiceAccountToken()

        val field = SheetsSyncService::class.java.getDeclaredField("sheetsApiClient")
        field.isAccessible = true
        field.set(service, mockClient)

        whenever(mockClient.getValues(eq("sheet-id"), eq("notes!B:B"), eq("fake-token")))
            .thenReturn(ValueRange(values = listOf(listOf("header"), listOf("Cuci"))))
        whenever(mockClient.batchUpdateValues(eq("sheet-id"), any(), eq("fake-token")))
            .thenReturn(BatchUpdateValuesResponse(responses = listOf(UpdateValuesResponse(updatedRows = 1))))

        val pkg1 = PackageData(price = "10000", name = "Cuci", duration = "24", unit = "Kg")
        whenever(mockClient.getValues(eq("sheet-id"), eq("notes!A2:D"), eq("fake-token")))
            .thenReturn(ValueRange(values = listOf(pkg1.toSheetValues().single())))

        val result = service.syncAndVerifyPackagesBatch("sheet-id", listOf(pkg1))
        assertEquals(listOf("Cuci"), result)
    }

    @Test
    fun `clearDeletedRows successfully processes multiple delete events`() = runBlocking {
        val mockClient: GoogleSheetsApiClient = mock()
        val service = spy(SheetsSyncService())
        doReturn("fake-token").whenever(service).getServiceAccountToken()

        val field = SheetsSyncService::class.java.getDeclaredField("sheetsApiClient")
        field.isAccessible = true
        field.set(service, mockClient)

        // Mock target range searches
        whenever(mockClient.getValues(eq("sheet-id"), eq("income!A:A"), eq("fake-token")))
            .thenReturn(ValueRange(values = listOf(listOf("header"), listOf("ORD-1"), listOf("ORD-2"))))
        whenever(mockClient.getValues(eq("sheet-id"), eq("outcome!A:A"), eq("fake-token")))
            .thenReturn(ValueRange(values = listOf(listOf("header"), listOf("OUT-1"))))

        val events = listOf(
            SyncDeleteEvent(1, SyncEntityType.ORDER, "ORD-2"),
            SyncDeleteEvent(2, SyncEntityType.OUTCOME, "OUT-1"),
            SyncDeleteEvent(3, SyncEntityType.GROSS, "Mei 2026") // should map to null delete target
        )

        val processed = service.clearDeletedRows("sheet-id", events)
        assertEquals(listOf(1, 2, 3), processed)
    }
}

