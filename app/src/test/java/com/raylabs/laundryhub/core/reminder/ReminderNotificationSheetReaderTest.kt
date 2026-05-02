package com.raylabs.laundryhub.core.reminder

import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.repository.GoogleSheetRepository
import com.raylabs.laundryhub.shared.util.Resource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ReminderNotificationSheetReaderTest {

    @Test
    fun `readTransactions delegates to GoogleSheetRepository with show all filter`() = runTest {
        val repository: GoogleSheetRepository = mock()
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

        whenever(repository.readIncomeTransaction(FILTER.SHOW_ALL_DATA)).thenReturn(expected)

        val actual = ReminderNotificationSheetReader(repository).readTransactions()

        assertSame(expected, actual)
    }
}
