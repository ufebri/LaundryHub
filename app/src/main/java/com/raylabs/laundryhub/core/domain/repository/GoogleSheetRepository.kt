package com.raylabs.laundryhub.core.domain.repository

import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.model.sheets.OrderData
import com.raylabs.laundryhub.core.domain.model.sheets.PackageData
import com.raylabs.laundryhub.core.domain.model.sheets.RangeDate
import com.raylabs.laundryhub.core.domain.model.sheets.SpreadsheetData
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.ui.common.util.Resource

interface GoogleSheetRepository {
    suspend fun readSummaryTransaction(): Resource<List<SpreadsheetData>>
    suspend fun readIncomeTransaction(
        filter: FILTER,
        rangeDate: RangeDate? = null
    ): Resource<List<TransactionData>>

    suspend fun readPackageData(): Resource<List<PackageData>>
    suspend fun readOtherPackage(): Resource<List<String>>
    suspend fun getLastOrderId(): Resource<String>
    suspend fun addOrder(order: OrderData): Resource<Boolean>
    suspend fun getOrderById(orderId: String): Resource<TransactionData>
}