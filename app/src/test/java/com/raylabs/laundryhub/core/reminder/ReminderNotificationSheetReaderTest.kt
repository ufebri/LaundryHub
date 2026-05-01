package com.raylabs.laundryhub.core.reminder

import com.raylabs.laundryhub.core.data.repository.GoogleSheetRepositoryImpl
import com.raylabs.laundryhub.core.data.service.GoogleSheetsAuthorizationManager
import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.repository.SpreadsheetIdProvider
import com.raylabs.laundryhub.shared.util.Resource
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.Mockito.mockConstruction
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ReminderNotificationSheetReaderTest {

    @Test
    fun `readTransactions delegates to GoogleSheetRepositoryImpl with show all filter`() = runTest {
        val authorizationManager: GoogleSheetsAuthorizationManager = mock()
        val spreadsheetIdProvider: SpreadsheetIdProvider = mock()
        val expected = Resource.Success(
            listOf(
                TransactionData(
                    orderID = "ORD-1",
                    date = "01/04/2026",
                    name = "Rina",
                    totalPrice = "12000",
                    packageType = "Regular",
                    paymentStatus = "UNPAID",
                    paymentMethod = "Cash",
                    weight = "1",
                    pricePerKg = "12000",
                    remark = "",
                    phoneNumber = "08123",
                    dueDate = "02/04/2026"
                )
            )
        )

        val mockedConstruction = mockConstruction(GoogleSheetRepositoryImpl::class.java) { mock, _ ->
            runBlocking {
                whenever(mock.readIncomeTransaction(FILTER.SHOW_ALL_DATA)).thenReturn(expected)
            }
        }

        try {
            val actual = ReminderNotificationSheetReader(
                googleSheetsAuthorizationManager = authorizationManager,
                spreadsheetIdProvider = spreadsheetIdProvider
            ).readTransactions()

            assertSame(expected, actual)
            assertEquals(1, mockedConstruction.constructed().size)
            runBlocking {
                verify(mockedConstruction.constructed().single()).readIncomeTransaction(FILTER.SHOW_ALL_DATA)
            }
        } finally {
            mockedConstruction.close()
        }
    }
}
