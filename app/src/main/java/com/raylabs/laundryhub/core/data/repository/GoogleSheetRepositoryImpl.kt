package com.raylabs.laundryhub.core.data.repository

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest
import com.google.api.services.sheets.v4.model.DeleteDimensionRequest
import com.google.api.services.sheets.v4.model.DimensionRange
import com.google.api.services.sheets.v4.model.Request
import com.google.api.services.sheets.v4.model.ValueRange
import com.raylabs.laundryhub.core.data.service.GoogleSheetService
import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.model.sheets.GrossData
import com.raylabs.laundryhub.core.domain.model.sheets.OrderData
import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.core.domain.model.sheets.PackageData
import com.raylabs.laundryhub.core.domain.model.sheets.RangeDate
import com.raylabs.laundryhub.core.domain.model.sheets.SpreadsheetData
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.model.sheets.filterRangeDateData
import com.raylabs.laundryhub.core.domain.model.sheets.getAllIncomeData
import com.raylabs.laundryhub.core.domain.model.sheets.getTodayIncomeData
import com.raylabs.laundryhub.core.domain.model.sheets.isCashData
import com.raylabs.laundryhub.core.domain.model.sheets.isPaidData
import com.raylabs.laundryhub.core.domain.model.sheets.isQRISData
import com.raylabs.laundryhub.core.domain.model.sheets.isUnpaidData
import com.raylabs.laundryhub.core.domain.model.sheets.toGrossData
import com.raylabs.laundryhub.core.domain.model.sheets.toIncomeList
import com.raylabs.laundryhub.core.domain.model.sheets.toOutcomeList
import com.raylabs.laundryhub.core.domain.model.sheets.toPackageData
import com.raylabs.laundryhub.core.domain.model.sheets.toSheetValues
import com.raylabs.laundryhub.core.domain.model.sheets.toUpdateSheetValues
import com.raylabs.laundryhub.core.domain.repository.GoogleSheetRepository
import com.raylabs.laundryhub.core.domain.repository.SpreadsheetIdProvider
import com.raylabs.laundryhub.ui.common.util.DateUtil
import com.raylabs.laundryhub.ui.common.util.DateUtil.parseDate
import com.raylabs.laundryhub.ui.common.util.Resource
import com.raylabs.laundryhub.ui.common.util.retry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GoogleSheetRepositoryImpl @Inject constructor(
    private val googleSheetService: GoogleSheetService,
    private val spreadsheetIdProvider: SpreadsheetIdProvider
) : GoogleSheetRepository {

    companion object {
        private const val SUMMARY_RANGE = "summary!A2:B"
        private const val GROSS_RANGE = "gross!A1:D"
        private const val INCOME_RANGE = "income!A1:N"
        private const val PACKAGE_RANGE = "notes!A1:D"
        private const val INCOME_REMARKS_RANGE = "income!I2:I"
        private const val ORDER_ID_RANGE = "income!A2:A"
        private const val OUTCOME_RANGE = "outcome!A1:F"

        private const val SHEET_APPEND_DATA = "USER_ENTERED"
    }

    override suspend fun readSummaryTransaction(
    ): Resource<List<SpreadsheetData>> {
        return withContext(Dispatchers.IO) {
            withConfiguredSpreadsheetId { spreadsheetId ->
                retry {
                    try {
                        val response = googleSheetService.getSheetsService().spreadsheets().values()
                            .get(spreadsheetId, SUMMARY_RANGE).execute()

                        val data = response.getValues().map {
                            SpreadsheetData(
                                key = it[0].toString(), value = it[1].toString()
                            )
                        }

                        if (data.isEmpty()) Resource.Empty else Resource.Success(data)
                    } catch (e: GoogleJsonResponseException) {
                        GSheetRepositoryErrorHandling.handleGoogleJsonResponseException(e)
                    } catch (e: Exception) {
                        GSheetRepositoryErrorHandling.handleReadSheetResponseException(e)
                    }
                } ?: GSheetRepositoryErrorHandling.handleFailAfterRetry()
            }
        }
    }

    override suspend fun readGrossData(): Resource<List<GrossData>> {
        return withContext(Dispatchers.IO) {
            withConfiguredSpreadsheetId { spreadsheetId ->
                retry {
                    try {
                        val response = googleSheetService.getSheetsService().spreadsheets().values()
                            .get(spreadsheetId, GROSS_RANGE).execute()

                        val headers = response.getValues().firstOrNull() ?: emptyList()
                        val dataRows = response.getValues().drop(1)

                        val data = dataRows.map { row ->
                            val mappedRow = headers.zip(row).associate {
                                it.first.toString() to it.second?.toString().orEmpty()
                            }
                            mappedRow.toGrossData()
                        }

                        if (data.isEmpty()) Resource.Empty else Resource.Success(data)
                    } catch (e: GoogleJsonResponseException) {
                        GSheetRepositoryErrorHandling.handleGoogleJsonResponseException(e)
                    } catch (e: Exception) {
                        GSheetRepositoryErrorHandling.handleReadSheetResponseException(e)
                    }
                } ?: GSheetRepositoryErrorHandling.handleFailAfterRetry()
            }
        }
    }


    override suspend fun readIncomeTransaction(
        filter: FILTER, rangeDate: RangeDate?
    ): Resource<List<TransactionData>> {
        return withContext(Dispatchers.IO) {
            withConfiguredSpreadsheetId { spreadsheetId ->
                retry {
                    try {
                        val response = googleSheetService.getSheetsService().spreadsheets().values()
                            .get(spreadsheetId, INCOME_RANGE).execute()

                        // Ambil header di baris pertama
                        val headers = response.getValues().firstOrNull() ?: emptyList()
                        val dataRows = response.getValues().drop(1) // Hilangkan header

                        val data = dataRows.map { row ->
                            val mappedRow = headers.zip(row).associate {
                                it.first.toString() to it.second?.toString().orEmpty()
                            }
                            mappedRow.toIncomeList()
                        }.filter { transaction ->
                            when (filter) {
                                FILTER.SHOW_ALL_DATA -> transaction.getAllIncomeData()
                                FILTER.TODAY_TRANSACTION_ONLY -> transaction.getTodayIncomeData()
                                FILTER.RANGE_TRANSACTION_DATA -> transaction.filterRangeDateData(
                                    rangeDate
                                )

                                FILTER.SHOW_PAID_DATA -> transaction.isPaidData()
                                FILTER.SHOW_UNPAID_DATA -> transaction.isUnpaidData()
                                FILTER.SHOW_PAID_BY_QR -> transaction.isQRISData()
                                FILTER.SHOW_PAID_BY_CASH -> transaction.isCashData()
                            }
                        }.sortedByDescending { transaction ->
                            parseDate(transaction.date, formatedDate = "dd/MM/yyyy")
                        }

                        if (data.isEmpty()) Resource.Empty else Resource.Success(data)
                    } catch (e: GoogleJsonResponseException) {
                        GSheetRepositoryErrorHandling.handleGoogleJsonResponseException(e)
                    } catch (e: Exception) {
                        GSheetRepositoryErrorHandling.handleReadSheetResponseException(e)
                    }
                } ?: GSheetRepositoryErrorHandling.handleFailAfterRetry()
            }
        }
    }

    override suspend fun readPackageData(): Resource<List<PackageData>> {
        return withContext(Dispatchers.IO) {
            withConfiguredSpreadsheetId { spreadsheetId ->
                retry {
                    try {
                        val response = googleSheetService.getSheetsService().spreadsheets().values()
                            .get(spreadsheetId, PACKAGE_RANGE).execute()

                        val headers = response.getValues().firstOrNull() ?: emptyList()
                        val dataRows = response.getValues().drop(1)

                        val data = dataRows.map { row ->
                            val mappedRow = headers.zip(row).associate {
                                it.first.toString() to it.second?.toString().orEmpty()
                            }
                            mappedRow.toPackageData()
                        }

                        if (data.isEmpty()) Resource.Empty else Resource.Success(data)
                    } catch (e: GoogleJsonResponseException) {
                        GSheetRepositoryErrorHandling.handleGoogleJsonResponseException(e)
                    } catch (e: Exception) {
                        GSheetRepositoryErrorHandling.handleReadSheetResponseException(e)
                    }
                } ?: GSheetRepositoryErrorHandling.handleFailAfterRetry()
            }
        }
    }

    override suspend fun readOtherPackage(): Resource<List<String>> {
        return withContext(Dispatchers.IO) {
            withConfiguredSpreadsheetId { spreadsheetId ->
                retry {
                    try {
                        val response = googleSheetService.getSheetsService().spreadsheets().values()
                            .get(spreadsheetId, INCOME_REMARKS_RANGE).execute()

                        val rawData = response.getValues() ?: emptyList()
                        val cleanedData = rawData.mapNotNull { row ->
                            row.firstOrNull()?.toString()?.trim()
                        }

                        if (cleanedData.isEmpty()) Resource.Empty
                        else Resource.Success(cleanedData)
                    } catch (e: GoogleJsonResponseException) {
                        GSheetRepositoryErrorHandling.handleGoogleJsonResponseException(e)
                    } catch (e: Exception) {
                        GSheetRepositoryErrorHandling.handleReadSheetResponseException(e)
                    }
                } ?: GSheetRepositoryErrorHandling.handleFailAfterRetry()
            }
        }
    }

    override suspend fun getLastOrderId(): Resource<String> {
        return withContext(Dispatchers.IO) {
            withConfiguredSpreadsheetId { spreadsheetId ->
                retry {
                    try {
                        handlingGetLastId(spreadsheetId, ORDER_ID_RANGE)
                    } catch (e: Exception) {
                        GSheetRepositoryErrorHandling.handleReadSheetResponseException(e)
                    }
                } ?: GSheetRepositoryErrorHandling.handleFailAfterRetry()
            }
        }
    }

    override suspend fun addOrder(order: OrderData): Resource<Boolean> {
        return withContext(Dispatchers.IO) {
            withConfiguredSpreadsheetId { spreadsheetId ->
                retry {
                    try {
                        val body = ValueRange().setValues(order.toSheetValues())
                        handlingSuccessAppendSheet(spreadsheetId, body, INCOME_RANGE)
                    } catch (e: Exception) {
                        GSheetRepositoryErrorHandling.handleFailedAddOrder(e)
                    }
                } ?: GSheetRepositoryErrorHandling.handleFailAfterRetry()
            }
        }
    }

    override suspend fun getOrderById(orderId: String): Resource<TransactionData> {
        return withContext(Dispatchers.IO) {
            withConfiguredSpreadsheetId { spreadsheetId ->
                retry {
                    try {
                        val response = googleSheetService.getSheetsService().spreadsheets().values()
                            .get(spreadsheetId, INCOME_RANGE).execute()

                        val headers = response.getValues().firstOrNull()
                        val dataRows = response.getValues().drop(1)

                        if (headers == null || dataRows.isEmpty()) {
                            return@retry Resource.Empty
                        }

                        val mapped = dataRows.mapIndexed { _, row ->
                            val assoc = headers.zip(row)
                                .associate { (key, value) -> key.toString() to value.toString() }
                            assoc
                        }.firstOrNull { it["orderID"]?.trim() == orderId.trim() }

                        if (mapped != null) {
                            val history = mapped.toIncomeList()
                            Resource.Success(history)
                        } else {
                            Resource.Empty
                        }
                    } catch (e: GoogleJsonResponseException) {
                        GSheetRepositoryErrorHandling.handleGoogleJsonResponseException(e)
                    } catch (e: Exception) {
                        GSheetRepositoryErrorHandling.handleReadSheetResponseException(e)
                    }
                } ?: GSheetRepositoryErrorHandling.handleFailAfterRetry()
            }
        }
    }

    override suspend fun updateOrder(order: OrderData): Resource<Boolean> {
        return withContext(Dispatchers.IO) {
            withConfiguredSpreadsheetId { spreadsheetId ->
                retry {
                    try {
                        val response = googleSheetService.getSheetsService().spreadsheets().values()
                            .get(spreadsheetId, INCOME_RANGE).execute()

                        val dataRows = response.getValues().drop(1)

                        val rowIndex = dataRows.indexOfFirst { row ->
                            row.firstOrNull()?.toString() == order.orderId
                        }

                        if (rowIndex == -1) {
                            return@retry GSheetRepositoryErrorHandling.handleIDNotFound()
                        }

                        val existingDate =
                            dataRows[rowIndex].getOrNull(1) ?: DateUtil.getTodayDate("dd/MM/yyyy")

                        val body =
                            ValueRange().setValues(order.toUpdateSheetValues(existingDate.toString()))
                        handlingSuccessUpdateSheet(spreadsheetId, body, "income!A${rowIndex + 2}:L")
                    } catch (e: Exception) {
                        GSheetRepositoryErrorHandling.handleFailedUpdate(e)
                    }
                } ?: GSheetRepositoryErrorHandling.handleFailAfterRetry()
            }
        }
    }

    override suspend fun deleteOrder(orderId: String): Resource<Boolean> {
        return withContext(Dispatchers.IO) {
            withConfiguredSpreadsheetId { spreadsheetId ->
                retry {
                    try {
                        val response = googleSheetService.getSheetsService().spreadsheets().values()
                            .get(spreadsheetId, INCOME_RANGE).execute()

                        val dataRows = response.getValues().drop(1)
                        val rowIndex = dataRows.indexOfFirst { row ->
                            row.firstOrNull()?.toString() == orderId
                        }

                        if (rowIndex == -1) {
                            return@retry GSheetRepositoryErrorHandling.handleIDNotFound()
                        }

                        deleteSheetRow(
                            spreadsheetId = spreadsheetId,
                            sheetName = "income",
                            rowIndex = rowIndex
                        )
                    } catch (e: Exception) {
                        GSheetRepositoryErrorHandling.handleFailedDelete(e)
                    }
                } ?: GSheetRepositoryErrorHandling.handleFailAfterRetry()
            }
        }
    }

    //    Outcome
    override suspend fun readOutcomeTransaction(): Resource<List<OutcomeData>> {
        return withContext(Dispatchers.IO) {
            withConfiguredSpreadsheetId { spreadsheetId ->
                retry {
                    try {
                        val response = googleSheetService.getSheetsService().spreadsheets().values()
                            .get(spreadsheetId, OUTCOME_RANGE).execute()

                        val headers = response.getValues().firstOrNull() ?: emptyList()
                        val dataRows = response.getValues().drop(1)

                        val data = dataRows.map { row ->
                            val mappedRow = headers.zip(row).associate {
                                it.first.toString() to it.second?.toString().orEmpty()
                            }
                            mappedRow.toOutcomeList()
                        }.sortedByDescending { outcomeData ->
                            parseDate(outcomeData.date, formatedDate = DateUtil.STANDARD_DATE_FORMATED)
                        }

                        if (data.isEmpty()) Resource.Empty else Resource.Success(data)
                    } catch (e: GoogleJsonResponseException) {
                        GSheetRepositoryErrorHandling.handleGoogleJsonResponseException(e)
                    } catch (e: Exception) {
                        GSheetRepositoryErrorHandling.handleReadSheetResponseException(e)
                    }
                } ?: GSheetRepositoryErrorHandling.handleFailAfterRetry()
            }
        }
    }

    override suspend fun addOutcome(outcome: OutcomeData): Resource<Boolean> {
        return withContext(Dispatchers.IO) {
            withConfiguredSpreadsheetId { spreadsheetId ->
                retry {
                    try {
                        val body = ValueRange().setValues(outcome.toSheetValues())
                        handlingSuccessAppendSheet(spreadsheetId, body, OUTCOME_RANGE)
                    } catch (e: Exception) {
                        GSheetRepositoryErrorHandling.handleFailedAddOrder(e)
                    }
                } ?: GSheetRepositoryErrorHandling.handleFailAfterRetry()
            }
        }
    }

    override suspend fun getLastOutcomeId(): Resource<String> {
        return withContext(Dispatchers.IO) {
            withConfiguredSpreadsheetId { spreadsheetId ->
                retry {
                    try {
                        handlingGetLastId(spreadsheetId, OUTCOME_RANGE)
                    } catch (e: Exception) {
                        GSheetRepositoryErrorHandling.handleReadSheetResponseException(e)
                    }
                } ?: GSheetRepositoryErrorHandling.handleFailAfterRetry()
            }
        }
    }

    override suspend fun updateOutcome(outcome: OutcomeData): Resource<Boolean> {
        return withContext(Dispatchers.IO) {
            withConfiguredSpreadsheetId { spreadsheetId ->
                retry {
                    try {
                        val response = googleSheetService.getSheetsService().spreadsheets().values()
                            .get(spreadsheetId, OUTCOME_RANGE).execute()

                        val dataRows = response.getValues().drop(1)

                        val rowIndex = dataRows.indexOfFirst { row ->
                            row.firstOrNull()?.toString() == outcome.id
                        }

                        if (rowIndex == -1) {
                            return@retry GSheetRepositoryErrorHandling.handleIDNotFound()
                        }

                        val existingDate =
                            dataRows[rowIndex].getOrNull(1) ?: DateUtil.getTodayDate("dd/MM/yyyy")

                        val body =
                            ValueRange().setValues(outcome.toUpdateSheetValues(existingDate.toString()))
                        handlingSuccessUpdateSheet(spreadsheetId, body, "outcome!A${rowIndex + 2}:F")

                    } catch (e: Exception) {
                        GSheetRepositoryErrorHandling.handleFailedUpdate(e)
                    }
                } ?: GSheetRepositoryErrorHandling.handleFailAfterRetry()
            }
        }
    }

    override suspend fun deleteOutcome(outcomeId: String): Resource<Boolean> {
        return withContext(Dispatchers.IO) {
            withConfiguredSpreadsheetId { spreadsheetId ->
                retry {
                    try {
                        val response = googleSheetService.getSheetsService().spreadsheets().values()
                            .get(spreadsheetId, OUTCOME_RANGE).execute()

                        val dataRows = response.getValues().drop(1)
                        val rowIndex = dataRows.indexOfFirst { row ->
                            row.firstOrNull()?.toString() == outcomeId
                        }

                        if (rowIndex == -1) {
                            return@retry GSheetRepositoryErrorHandling.handleIDNotFound()
                        }

                        deleteSheetRow(
                            spreadsheetId = spreadsheetId,
                            sheetName = "outcome",
                            rowIndex = rowIndex
                        )
                    } catch (e: Exception) {
                        GSheetRepositoryErrorHandling.handleFailedDelete(e)
                    }
                } ?: GSheetRepositoryErrorHandling.handleFailAfterRetry()
            }
        }
    }

    override suspend fun getOutcomeById(outcomeId: String): Resource<OutcomeData> {
        return withContext(Dispatchers.IO) {
            withConfiguredSpreadsheetId { spreadsheetId ->
                retry {
                    try {
                        val response = googleSheetService.getSheetsService().spreadsheets().values()
                            .get(spreadsheetId, OUTCOME_RANGE).execute()

                        val headers = response.getValues().firstOrNull()
                        val dataRows = response.getValues().drop(1)

                        if (headers == null || dataRows.isEmpty()) {
                            return@retry Resource.Empty
                        }

                        val mapped = dataRows.mapIndexed { _, row ->
                            val assoc = headers.zip(row)
                                .associate { (key, value) -> key.toString() to value.toString() }
                            assoc
                        }.firstOrNull { it["id"]?.trim() == outcomeId.trim() }

                        if (mapped != null) {
                            val history = mapped.toOutcomeList()
                            Resource.Success(history)
                        } else {
                            Resource.Empty
                        }
                    } catch (e: GoogleJsonResponseException) {
                        GSheetRepositoryErrorHandling.handleGoogleJsonResponseException(e)
                    } catch (e: Exception) {
                        GSheetRepositoryErrorHandling.handleReadSheetResponseException(e)
                    }
                } ?: GSheetRepositoryErrorHandling.handleFailAfterRetry()
            }
        }
    }

    //Common Success Handling Response

    private suspend fun <T> withConfiguredSpreadsheetId(
        block: suspend (String) -> Resource<T>
    ): Resource<T> {
        val spreadsheetId = spreadsheetIdProvider.getSpreadsheetId()
        return if (spreadsheetId.isNullOrBlank()) {
            Resource.Error("Spreadsheet not connected yet. Please complete setup first.")
        } else {
            block(spreadsheetId)
        }
    }

    private fun handlingGetLastId(spreadsheetId: String, range: String): Resource.Success<String> {
        val response = googleSheetService.getSheetsService().spreadsheets().values()
            .get(spreadsheetId, range).execute()

        val rows = response.getValues() ?: emptyList()

        val lastRow = rows.drop(1).lastOrNull()
        val lastId = lastRow?.getOrNull(0)?.toString()

        return if (lastId != null) {
            val mLastGenerateID = "${lastId.toInt() + 1}"
            Resource.Success(mLastGenerateID)
        } else {
            Resource.Success("0") // jika belum ada data, mulai dari 0
        }
    }

    private fun handlingSuccessAppendSheet(
        spreadsheetId: String,
        valueRange: ValueRange,
        range: String
    ): Resource<Boolean> {
        googleSheetService.getSheetsService().spreadsheets().values()
            .append(spreadsheetId, range, valueRange)
            .setValueInputOption(SHEET_APPEND_DATA).execute()

        return Resource.Success(true)
    }

    private fun handlingSuccessUpdateSheet(
        spreadsheetId: String,
        valueRange: ValueRange,
        range: String
    ): Resource<Boolean> {
        googleSheetService.getSheetsService().spreadsheets().values()
            .update(spreadsheetId, range, valueRange)
            .setValueInputOption(SHEET_APPEND_DATA)
            .execute()

        return Resource.Success(true)
    }

    private fun deleteSheetRow(
        spreadsheetId: String,
        sheetName: String,
        rowIndex: Int
    ): Resource<Boolean> {
        val spreadsheet = googleSheetService.getSheetsService()
            .spreadsheets()
            .get(spreadsheetId)
            .execute()

        val sheetId = spreadsheet.sheets
            ?.firstOrNull { it.properties?.title == sheetName }
            ?.properties
            ?.sheetId
            ?: return Resource.Error("Sheet not found.")

        val request = BatchUpdateSpreadsheetRequest().setRequests(
            listOf(
                Request().setDeleteDimension(
                    DeleteDimensionRequest().setRange(
                        DimensionRange()
                            .setSheetId(sheetId)
                            .setDimension("ROWS")
                            .setStartIndex(rowIndex + 1)
                            .setEndIndex(rowIndex + 2)
                    )
                )
            )
        )

        googleSheetService.getSheetsService()
            .spreadsheets()
            .batchUpdate(spreadsheetId, request)
            .execute()

        return Resource.Success(true)
    }
}
