package com.raylabs.laundryhub.core.reminder

import com.raylabs.laundryhub.core.data.repository.GoogleSheetRepositoryImpl
import com.raylabs.laundryhub.core.data.service.GoogleSheetService
import com.raylabs.laundryhub.core.data.service.GoogleSheetsAuthorizationManager
import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.repository.SpreadsheetIdProvider
import com.raylabs.laundryhub.ui.common.util.Resource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderNotificationSheetReader @Inject constructor(
    private val googleSheetsAuthorizationManager: GoogleSheetsAuthorizationManager,
    private val spreadsheetIdProvider: SpreadsheetIdProvider
) {
    suspend fun readTransactions(): Resource<List<TransactionData>> {
        val repository = GoogleSheetRepositoryImpl(
            googleSheetService = GoogleSheetService(googleSheetsAuthorizationManager),
            spreadsheetIdProvider = spreadsheetIdProvider
        )
        return repository.readIncomeTransaction(filter = FILTER.SHOW_ALL_DATA)
    }
}
