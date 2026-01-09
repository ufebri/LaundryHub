package com.raylabs.laundryhub.core.domain.repository

import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.model.sheets.GrossData
import com.raylabs.laundryhub.core.domain.model.sheets.OrderData
import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.core.domain.model.sheets.PackageData
import com.raylabs.laundryhub.core.domain.model.sheets.RangeDate
import com.raylabs.laundryhub.core.domain.model.sheets.SpreadsheetData
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.ui.common.util.Resource

interface GoogleSheetRepository {
    suspend fun readSummaryTransaction(): Resource<List<SpreadsheetData>>
    suspend fun readGrossData(): Resource<List<GrossData>>
    suspend fun readIncomeTransaction(
        filter: FILTER,
        rangeDate: RangeDate? = null
    ): Resource<List<TransactionData>>

    suspend fun readPackageData(): Resource<List<PackageData>>
    suspend fun readOtherPackage(): Resource<List<String>>
    suspend fun getLastOrderId(): Resource<String>
    suspend fun addOrder(order: OrderData): Resource<Boolean>
    suspend fun getOrderById(orderId: String): Resource<TransactionData>
    suspend fun updateOrder(order: OrderData): Resource<Boolean>

    //Outcome
    suspend fun readOutcomeTransaction(): Resource<List<OutcomeData>>
    suspend fun addOutcome(outcome: OutcomeData): Resource<Boolean>
    suspend fun getLastOutcomeId(): Resource<String>
    suspend fun updateOutcome(outcome: OutcomeData): Resource<Boolean>
    suspend fun getOutcomeById(outcomeId: String): Resource<OutcomeData>

}
