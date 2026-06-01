package com.raylabs.laundryhub.core.reminder

import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.repository.LaundryRepository
import com.raylabs.laundryhub.shared.util.Resource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ReminderNotificationSheetReaderTest {

    private lateinit var repository: LaundryRepository
    private lateinit var reader: ReminderNotificationSheetReader

    @Before
    fun setUp() {
        repository = mock()
        reader = ReminderNotificationSheetReader(repository)
    }

    @Test
    fun `readTransactions delegates to repository readIncomeTransaction with SHOW_ALL_DATA`() = runTest {
        val mockData = listOf(mock<TransactionData>())
        whenever(repository.readIncomeTransaction(FILTER.SHOW_ALL_DATA, null, null, null, null, null))
            .thenReturn(Resource.Success(mockData))

        val result = reader.readTransactions()

        assertEquals(Resource.Success(mockData), result)
        verify(repository).readIncomeTransaction(FILTER.SHOW_ALL_DATA, null, null, null, null, null)
    }
}
