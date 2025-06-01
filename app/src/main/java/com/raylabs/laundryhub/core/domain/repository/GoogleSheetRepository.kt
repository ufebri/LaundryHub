package com.raylabs.laundryhub.core.domain.repository

import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.model.sheets.HistoryData
import com.raylabs.laundryhub.core.domain.model.sheets.HistoryFilter
import com.raylabs.laundryhub.core.domain.model.sheets.RangeDate
import com.raylabs.laundryhub.core.domain.model.sheets.SpreadsheetData
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.ui.common.util.Resource

interface GoogleSheetRepository {
    fun createData(spreadsheetId: String, range: String, data: SpreadsheetData)
    suspend fun readSummaryTransaction(): Resource<List<SpreadsheetData>>
    suspend fun readIncomeTransaction(filter: FILTER, rangeDate: RangeDate? = null): Resource<List<TransactionData>>
    suspend fun readHistoryData(filterHistory: HistoryFilter): Resource<List<HistoryData>>
}
