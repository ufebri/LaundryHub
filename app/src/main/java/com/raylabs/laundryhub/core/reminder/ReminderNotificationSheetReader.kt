package com.raylabs.laundryhub.core.reminder

import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.repository.GoogleSheetRepository
import com.raylabs.laundryhub.shared.util.Resource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderNotificationSheetReader @Inject constructor(
    private val repository: GoogleSheetRepository
) {
    suspend fun readTransactions(): Resource<List<TransactionData>> {
        return repository.readIncomeTransaction(filter = FILTER.SHOW_ALL_DATA)
    }
}
