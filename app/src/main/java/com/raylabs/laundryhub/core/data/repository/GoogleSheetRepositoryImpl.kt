package com.raylabs.laundryhub.core.data.repository

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.sheets.v4.model.ValueRange
import com.raylabs.laundryhub.BuildConfig
import com.raylabs.laundryhub.core.data.service.GoogleSheetService
import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.model.sheets.HistoryData
import com.raylabs.laundryhub.core.domain.model.sheets.HistoryFilter
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
import com.raylabs.laundryhub.core.domain.model.sheets.toHistoryData
import com.raylabs.laundryhub.core.domain.model.sheets.toIncomeList
import com.raylabs.laundryhub.core.domain.repository.GoogleSheetRepository
import com.raylabs.laundryhub.ui.common.util.DateUtil.parseDate
import com.raylabs.laundryhub.ui.common.util.Resource
import com.raylabs.laundryhub.ui.common.util.retry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GoogleSheetRepositoryImpl @Inject constructor(
    private val googleSheetService: GoogleSheetService
) : GoogleSheetRepository {

    companion object {
        private const val SUMMARY_RANGE = "summary!A2:B14"
        private const val INCOME_RANGE = "income!A1:N"
        private const val HISTORY_RANGE = "history!A1:V"
    }

    override fun createData(spreadsheetId: String, range: String, data: SpreadsheetData) {
        val body = ValueRange().setValues(listOf(listOf(data.key, data.value)))
        googleSheetService.getSheetsService().spreadsheets().values()
            .append(spreadsheetId, range, body).setValueInputOption("RAW").execute()
    }

    override suspend fun readSummaryTransaction(
    ): Resource<List<SpreadsheetData>> {
        return withContext(Dispatchers.IO) {
            retry {
                try {
                    val response = googleSheetService.getSheetsService().spreadsheets().values()
                        .get(BuildConfig.SPREAD_SHEET_ID, SUMMARY_RANGE).execute()

                    val data = response.getValues().map {
                        SpreadsheetData(
                            key = it[0].toString(), value = it[1].toString()
                        )
                    }

                    if (data.isEmpty()) Resource.Empty else Resource.Success(data)
                } catch (e: GoogleJsonResponseException) {
                    // Tangkap error detail dari Google API
                    val statusCode = e.statusCode
                    val statusMessage = e.statusMessage
                    val details = e.details?.message ?: "Unknown Error"

                    Resource.Error("Error $statusCode: $statusMessage\nDetails: $details")
                } catch (e: Exception) {
                    Resource.Error(e.message ?: "Unexpected Error")
                }
            } ?: Resource.Error("Failed after 3 attempts.")
        }
    }


    override suspend fun readIncomeTransaction(
        filter: FILTER,
        rangeDate: RangeDate?
    ): Resource<List<TransactionData>> {
        return withContext(Dispatchers.IO) {
            retry {
                try {
                    val response = googleSheetService.getSheetsService().spreadsheets().values()
                        .get(BuildConfig.SPREAD_SHEET_ID, INCOME_RANGE).execute()

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
                        parseDate(transaction.date)
                    }

                    if (data.isEmpty()) Resource.Empty else Resource.Success(data)
                } catch (e: GoogleJsonResponseException) {
                    // Tangkap error detail dari Google API
                    val statusCode = e.statusCode
                    val statusMessage = e.statusMessage
                    val details = e.details?.message ?: "Unknown Error"

                    Resource.Error("Error $statusCode: $statusMessage\nDetails: $details")
                } catch (e: Exception) {
                    Resource.Error(e.message ?: "Unexpected Error")
                }
            } ?: Resource.Error("Failed after 3 attempts.")
        }
    }

    override suspend fun readHistoryData(filterHistory: HistoryFilter): Resource<List<HistoryData>> {
        return withContext(Dispatchers.IO) {
            retry {
                try {
                    val response = googleSheetService.getSheetsService().spreadsheets().values()
                        .get(BuildConfig.SPREAD_SHEET_ID, HISTORY_RANGE).execute()

                    val headers = response.getValues().firstOrNull() ?: emptyList()
                    val dataRows = response.getValues().drop(1)

                    val data = dataRows.map { row ->
                        val mappedRow = headers.zip(row).associate {
                            it.first.toString() to it.second?.toString().orEmpty()
                        }
                        mappedRow.toHistoryData()
                    }.sortedByDescending { transaction ->
                        parseDate(transaction.dueDate)
                    }

                    if (filterHistory == HistoryFilter.SHOW_UNDONE_ORDER) {
                        data.filterNot {
                            it.status in listOf(
                                "Ready for Pickup",
                                "Delivered",
                                "Overdue Pickup"
                            )
                        }
                    }

                    if (data.isEmpty()) Resource.Empty else Resource.Success(data)
                } catch (e: GoogleJsonResponseException) {
                    // Tangkap error detail dari Google API
                    val statusCode = e.statusCode
                    val statusMessage = e.statusMessage
                    val details = e.details?.message ?: "Unknown Error"

                    Resource.Error("Error $statusCode: $statusMessage\nDetails: $details")
                } catch (e: Exception) {
                    Resource.Error(e.message ?: "Unexpected Error")
                }
            }
        } ?: Resource.Error("Failed after 3 attempts.")
    }
}
