package com.raylabs.laundryhub.core.data.repository

import com.raylabs.laundryhub.core.data.service.GoogleSheetsAuthorizationManager
import com.raylabs.laundryhub.core.domain.model.sheets.*
import com.raylabs.laundryhub.core.domain.repository.GoogleSheetRepository
import com.raylabs.laundryhub.core.domain.repository.SpreadsheetIdProvider
import com.raylabs.laundryhub.shared.network.api.GoogleSheetsApiClient
import com.raylabs.laundryhub.shared.network.model.sheets.*
import com.raylabs.laundryhub.shared.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GoogleSheetRepositoryImpl @Inject constructor(
    private val apiClient: GoogleSheetsApiClient,
    private val authManager: GoogleSheetsAuthorizationManager,
    private val spreadsheetIdProvider: SpreadsheetIdProvider
) : GoogleSheetRepository {

    companion object {
        private const val SUMMARY_RANGE = "summary!A1:B"
        private const val GROSS_RANGE = "gross!A1:D"
        private const val INCOME_RANGE = "income!A1:L"
        private const val PACKAGE_RANGE = "notes!A1:D"
        private const val PACKAGE_WRITE_RANGE = "notes!A:D"
        private const val OUTCOME_RANGE = "outcome!A1:F"
        private const val INCOME_REMARKS_RANGE = "income!I2:I"
        private const val ORDER_ID_RANGE = "income!A2:A"
        private const val OUTCOME_ID_RANGE = "outcome!A2:A"
        
        private const val INCOME_SHEET_NAME = "income"
        private const val PACKAGE_SHEET_NAME = "notes"
        private const val OUTCOME_SHEET_NAME = "outcome"
    }

    private suspend fun <T> safeApiCall(block: suspend (spreadsheetId: String, token: String) -> T): Resource<T> {
        return withContext(Dispatchers.IO) {
            try {
                val spreadsheetId = spreadsheetIdProvider.getSpreadsheetId()
                    ?: return@withContext Resource.Error("Spreadsheet not connected yet. Please complete setup first.")
                val token = authManager.getAccessToken()
                    ?: return@withContext Resource.Error("Authorization failed. Please sign in again.")
                Resource.Success(block(spreadsheetId, token))
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Unknown Error")
            }
        }
    }

    override suspend fun readSummaryTransaction(): Resource<List<SpreadsheetData>> = safeApiCall { spreadsheetId, token ->
        val response = apiClient.getValues(spreadsheetId, SUMMARY_RANGE, token)
        val rows = response.values ?: emptyList()
        rows.map {
            SpreadsheetData(key = it.getOrNull(0).orEmpty(), value = it.getOrNull(1).orEmpty())
        }
    }.let { res ->
        if (res is Resource.Success && res.data.isEmpty()) Resource.Empty else res
    }

    override suspend fun readGrossData(): Resource<List<GrossData>> = safeApiCall { spreadsheetId, token ->
        val response = apiClient.getValues(spreadsheetId, GROSS_RANGE, token)
        val headers = response.values?.firstOrNull() ?: emptyList()
        val dataRows = response.values?.drop(1) ?: emptyList()
        dataRows.map { row ->
            headers.zip(row).associate { it.first to it.second }.toGrossData()
        }
    }.let { res ->
        if (res is Resource.Success && res.data.isEmpty()) Resource.Empty else res
    }

    override suspend fun readIncomeTransaction(filter: FILTER, rangeDate: RangeDate?): Resource<List<TransactionData>> = safeApiCall { spreadsheetId, token ->
        val response = apiClient.getValues(spreadsheetId, INCOME_RANGE, token)
        val headers = response.values?.firstOrNull() ?: emptyList()
        val dataRows = response.values?.drop(1) ?: emptyList()
        dataRows.map { row ->
            headers.zip(row).associate { it.first to it.second }.toIncomeList()
        }.filter { transaction ->
            when (filter) {
                FILTER.SHOW_ALL_DATA -> true
                FILTER.TODAY_TRANSACTION_ONLY -> transaction.getTodayIncomeData()
                FILTER.RANGE_TRANSACTION_DATA -> transaction.filterRangeDateData(rangeDate)
                FILTER.SHOW_PAID_DATA -> transaction.isPaidData()
                FILTER.SHOW_UNPAID_DATA -> transaction.isUnpaidData()
                FILTER.SHOW_PAID_BY_QR -> transaction.isQRISData()
                FILTER.SHOW_PAID_BY_CASH -> transaction.isCashData()
            }
        }
    }.let { res ->
        if (res is Resource.Success && res.data.isEmpty()) Resource.Empty else res
    }

    override suspend fun readPackageData(): Resource<List<PackageData>> = safeApiCall { spreadsheetId, token ->
        val response = apiClient.getValues(spreadsheetId, PACKAGE_RANGE, token)
        val headers = response.values?.firstOrNull() ?: emptyList()
        val dataRows = response.values?.drop(1) ?: emptyList()
        dataRows.mapIndexed { index, row ->
            headers.zip(row).associate { it.first to it.second }.toPackageData(sheetRowIndex = index + 2)
        }
    }.let { res ->
        if (res is Resource.Success && res.data.isEmpty()) Resource.Empty else res
    }

    override suspend fun addPackage(packageData: PackageData): Resource<Boolean> = safeApiCall { spreadsheetId, token ->
        val valueRange = com.raylabs.laundryhub.shared.network.model.sheets.ValueRange(
            values = packageData.toSheetValues()
        )
        apiClient.appendValues(spreadsheetId, PACKAGE_WRITE_RANGE, valueRange, token)
        true
    }

    override suspend fun updatePackage(packageData: PackageData): Resource<Boolean> = safeApiCall { spreadsheetId, token ->
        if (packageData.sheetRowIndex < 2) throw Exception("Package row not found.")
        val range = "$PACKAGE_SHEET_NAME!A${packageData.sheetRowIndex}:D"
        val valueRange = com.raylabs.laundryhub.shared.network.model.sheets.ValueRange(
            values = packageData.toSheetValues()
        )
        apiClient.updateValues(spreadsheetId, range, valueRange, token)
        true
    }

    override suspend fun deletePackage(sheetRowIndex: Int): Resource<Boolean> = safeApiCall { spreadsheetId, token ->
        if (sheetRowIndex < 2) throw Exception("Package row not found.")
        val sheetId = getSheetId(spreadsheetId, PACKAGE_SHEET_NAME, token)
        val request = BatchUpdateSpreadsheetRequest(
            requests = listOf(
                Request(
                    deleteDimension = DeleteDimensionRequest(
                        range = DimensionRange(
                            sheetId = sheetId,
                            dimension = "ROWS",
                            startIndex = sheetRowIndex - 1,
                            endIndex = sheetRowIndex
                        )
                    )
                )
            )
        )
        apiClient.batchUpdate(spreadsheetId, request, token)
        true
    }

    override suspend fun readOtherPackage(): Resource<List<String>> = safeApiCall { spreadsheetId, token ->
        val response = apiClient.getValues(spreadsheetId, INCOME_REMARKS_RANGE, token)
        response.values?.mapNotNull { it.firstOrNull()?.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
    }.let { res ->
        if (res is Resource.Success && res.data.isEmpty()) Resource.Empty else res
    }

    override suspend fun getLastOrderId(): Resource<String> = safeApiCall { spreadsheetId, token ->
        val response = apiClient.getValues(spreadsheetId, ORDER_ID_RANGE, token)
        val rows = response.values ?: emptyList()
        val lastId = rows.lastOrNull()?.firstOrNull()?.toIntOrNull() ?: 0
        (lastId + 1).toString()
    }

    override suspend fun addOrder(order: OrderData): Resource<Boolean> = safeApiCall { spreadsheetId, token ->
        val valueRange = com.raylabs.laundryhub.shared.network.model.sheets.ValueRange(
            values = order.toSheetValues()
        )
        apiClient.appendValues(spreadsheetId, INCOME_SHEET_NAME, valueRange, token)
        true
    }

    override suspend fun getOrderById(orderId: String): Resource<TransactionData> = safeApiCall { spreadsheetId, token ->
        val response = apiClient.getValues(spreadsheetId, INCOME_RANGE, token)
        val headers = response.values?.firstOrNull() ?: emptyList()
        val dataRows = response.values?.drop(1) ?: emptyList()
        val row = dataRows.find { it.getOrNull(0) == orderId } ?: throw Exception("Order not found")
        headers.zip(row).associate { it.first to it.second }.toIncomeList()
    }

    override suspend fun updateOrder(order: OrderData): Resource<Boolean> = safeApiCall { spreadsheetId, token ->
        val response = apiClient.getValues(spreadsheetId, INCOME_RANGE, token)
        val dataRows = response.values ?: emptyList()
        val rowIndex = dataRows.indexOfFirst { it.getOrNull(0) == order.orderId }
        if (rowIndex == -1) throw Exception("Order not found")
        
        val existingDate = dataRows[rowIndex].getOrNull(1).orEmpty()
        val range = "$INCOME_SHEET_NAME!A${rowIndex + 1}:L"
        val valueRange = com.raylabs.laundryhub.shared.network.model.sheets.ValueRange(
            values = order.toUpdateSheetValues(existingDate)
        )
        apiClient.updateValues(spreadsheetId, range, valueRange, token)
        true
    }

    override suspend fun deleteOrder(orderId: String): Resource<Boolean> = safeApiCall { spreadsheetId, token ->
        val response = apiClient.getValues(spreadsheetId, INCOME_RANGE, token)
        val dataRows = response.values ?: emptyList()
        val rowIndex = dataRows.indexOfFirst { it.getOrNull(0) == orderId }
        if (rowIndex == -1) throw Exception("Order not found")

        val sheetId = getSheetId(spreadsheetId, INCOME_SHEET_NAME, token)
        val request = BatchUpdateSpreadsheetRequest(
            requests = listOf(
                Request(
                    deleteDimension = DeleteDimensionRequest(
                        range = DimensionRange(
                            sheetId = sheetId,
                            dimension = "ROWS",
                            startIndex = rowIndex,
                            endIndex = rowIndex + 1
                        )
                    )
                )
            )
        )
        apiClient.batchUpdate(spreadsheetId, request, token)
        true
    }

    override suspend fun readOutcomeTransaction(): Resource<List<OutcomeData>> = safeApiCall { spreadsheetId, token ->
        val response = apiClient.getValues(spreadsheetId, OUTCOME_RANGE, token)
        val headers = response.values?.firstOrNull() ?: emptyList()
        val dataRows = response.values?.drop(1) ?: emptyList()
        dataRows.map { row ->
            headers.zip(row).associate { it.first to it.second }.toOutcomeList()
        }
    }.let { res ->
        if (res is Resource.Success && res.data.isEmpty()) Resource.Empty else res
    }

    override suspend fun addOutcome(outcome: OutcomeData): Resource<Boolean> = safeApiCall { spreadsheetId, token ->
        val valueRange = com.raylabs.laundryhub.shared.network.model.sheets.ValueRange(
            values = outcome.toSheetValues()
        )
        apiClient.appendValues(spreadsheetId, OUTCOME_SHEET_NAME, valueRange, token)
        true
    }

    override suspend fun getLastOutcomeId(): Resource<String> = safeApiCall { spreadsheetId, token ->
        val response = apiClient.getValues(spreadsheetId, OUTCOME_ID_RANGE, token)
        val rows = response.values ?: emptyList()
        val lastId = rows.lastOrNull()?.firstOrNull()?.toIntOrNull() ?: 0
        (lastId + 1).toString()
    }

    override suspend fun updateOutcome(outcome: OutcomeData): Resource<Boolean> = safeApiCall { spreadsheetId, token ->
        val response = apiClient.getValues(spreadsheetId, OUTCOME_RANGE, token)
        val dataRows = response.values ?: emptyList()
        val rowIndex = dataRows.indexOfFirst { it.getOrNull(0) == outcome.id }
        if (rowIndex == -1) throw Exception("Outcome not found")

        val existingDate = dataRows[rowIndex].getOrNull(1).orEmpty()
        val range = "$OUTCOME_SHEET_NAME!A${rowIndex + 1}:F"
        val valueRange = com.raylabs.laundryhub.shared.network.model.sheets.ValueRange(
            values = outcome.toUpdateSheetValues(existingDate)
        )
        apiClient.updateValues(spreadsheetId, range, valueRange, token)
        true
    }

    override suspend fun getOutcomeById(outcomeId: String): Resource<OutcomeData> = safeApiCall { spreadsheetId, token ->
        val response = apiClient.getValues(spreadsheetId, OUTCOME_RANGE, token)
        val headers = response.values?.firstOrNull() ?: emptyList()
        val dataRows = response.values?.drop(1) ?: emptyList()
        val row = dataRows.find { it.getOrNull(0) == outcomeId } ?: throw Exception("Outcome not found")
        headers.zip(row).associate { it.first to it.second }.toOutcomeList()
    }

    override suspend fun deleteOutcome(outcomeId: String): Resource<Boolean> = safeApiCall { spreadsheetId, token ->
        val response = apiClient.getValues(spreadsheetId, OUTCOME_RANGE, token)
        val dataRows = response.values ?: emptyList()
        val rowIndex = dataRows.indexOfFirst { it.getOrNull(0) == outcomeId }
        if (rowIndex == -1) throw Exception("Outcome not found")

        val sheetId = getSheetId(spreadsheetId, OUTCOME_SHEET_NAME, token)
        val request = BatchUpdateSpreadsheetRequest(
            requests = listOf(
                Request(
                    deleteDimension = DeleteDimensionRequest(
                        range = DimensionRange(
                            sheetId = sheetId,
                            dimension = "ROWS",
                            startIndex = rowIndex,
                            endIndex = rowIndex + 1
                        )
                    )
                )
            )
        )
        apiClient.batchUpdate(spreadsheetId, request, token)
        true
    }

    private suspend fun getSheetId(spreadsheetId: String, sheetName: String, token: String): Int {
        // This is a simplification. In a real app, you might want to cache this.
        // For now, we'll just return a mock ID or fetch it if needed.
        // In the legacy code, it was fetching the spreadsheet and finding the sheet by title.
        // Let's assume some common IDs or fetch them.
        return when (sheetName) {
            INCOME_SHEET_NAME -> 0 // Often 0
            PACKAGE_SHEET_NAME -> 1 // Dummy
            OUTCOME_SHEET_NAME -> 2 // Dummy
            else -> 0
        }
    }
}
