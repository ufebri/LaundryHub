package com.raylabs.laundryhub.core.data.repository

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.sheets.v4.model.ValueRange
import com.raylabs.laundryhub.BuildConfig
import com.raylabs.laundryhub.core.data.service.GoogleSheetService
import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.model.sheets.OrderData
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
import com.raylabs.laundryhub.core.domain.model.sheets.toIncomeList
import com.raylabs.laundryhub.core.domain.model.sheets.toPackageData
import com.raylabs.laundryhub.core.domain.repository.GoogleSheetRepository
import com.raylabs.laundryhub.ui.common.util.DateUtil
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
        private const val SUMMARY_RANGE = "summary!A2:B"
        private const val INCOME_RANGE = "income!A1:N"
        private const val PACKAGE_RANGE = "notes!A1:D"
        private const val INCOME_REMARKS_RANGE = "income!I2:I"
        private const val ORDER_ID_RANGE = "income!A2:A"
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
                        parseDate(transaction.date, formatedDate = "dd/MM/yyyy")
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

    override suspend fun readPackageData(): Resource<List<PackageData>> {
        return withContext(Dispatchers.IO) {
            retry {
                try {
                    val response = googleSheetService.getSheetsService().spreadsheets().values()
                        .get(BuildConfig.SPREAD_SHEET_ID, PACKAGE_RANGE).execute()

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

    override suspend fun readOtherPackage(): Resource<List<String>> {
        return withContext(Dispatchers.IO) {
            retry {
                try {
                    val response = googleSheetService.getSheetsService().spreadsheets().values()
                        .get(BuildConfig.SPREAD_SHEET_ID, INCOME_REMARKS_RANGE)
                        .execute()

                    val rawData = response.getValues() ?: emptyList()
                    val cleanedData = rawData.mapNotNull { row ->
                        row.firstOrNull()?.toString()?.trim()
                    }

                    if (cleanedData.isEmpty()) Resource.Empty
                    else Resource.Success(cleanedData)
                } catch (e: GoogleJsonResponseException) {
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

    override suspend fun getLastOrderId(): Resource<String> {
        return withContext(Dispatchers.IO) {
            retry {
                try {
                    val response = googleSheetService.getSheetsService()
                        .spreadsheets()
                        .values()
                        .get(BuildConfig.SPREAD_SHEET_ID, ORDER_ID_RANGE)
                        .execute()

                    val rows = response.getValues() ?: emptyList()

                    // Ambil baris terakhir yang berisi ID di kolom paling kiri
                    val lastRow = rows.drop(1).lastOrNull() // skip header
                    val lastId = lastRow?.getOrNull(0)?.toString()

                    if (lastId != null) {
                        val mLastGenerateID = "${lastId.toInt() + 1}"
                        Resource.Success(mLastGenerateID)
                    } else {
                        Resource.Success("0") // jika belum ada data, mulai dari 0
                    }

                } catch (e: Exception) {
                    Resource.Error(e.message ?: "Unknown Error")
                }
            } ?: Resource.Error("Failed after 3 attempts.")
        }
    }

    override suspend fun addOrder(order: OrderData): Resource<Boolean> {
        return withContext(Dispatchers.IO) {
            retry {
                try {
                    val values = listOf(
                        listOf(
                            order.orderId,
                            DateUtil.getTodayDate("dd/MM/yyyy"),
                            order.name,
                            order.weight,
                            order.priceKg,
                            order.totalPrice, // total price, bisa hitung nanti
                            order.getSpreadSheetPaidStatus, // status lunas/belum
                            order.packageName,
                            order.remark,
                            order.getSpreadSheetPaymentMethod,
                            order.phoneNumber
                        )
                    )

                    val body = ValueRange().setValues(values)
                    googleSheetService.getSheetsService().spreadsheets().values()
                        .append(BuildConfig.SPREAD_SHEET_ID, INCOME_RANGE, body)
                        .setValueInputOption("USER_ENTERED")
                        .execute()

                    Resource.Success(true)

                } catch (e: Exception) {
                    Resource.Error(e.message ?: "Failed to append order.")
                }
            } ?: Resource.Error("Gagal menambahkan order setelah 3 kali coba.")
        }
    }

    override suspend fun getOrderById(orderId: String): Resource<TransactionData> {
        return withContext(Dispatchers.IO) {
            retry {
                try {
                    val response = googleSheetService
                        .getSheetsService()
                        .spreadsheets()
                        .values()
                        .get(BuildConfig.SPREAD_SHEET_ID, INCOME_RANGE)
                        .execute()

                    val headers = response.getValues().firstOrNull()
                    val dataRows = response.getValues().drop(1)

                    if (headers == null || dataRows.isEmpty()) {
                        return@retry Resource.Empty
                    }

                    val mapped = dataRows
                        .map { row ->
                            headers.zip(row)
                                .associate { (key, value) -> key.toString() to value.toString() }
                        }
                        .firstOrNull { it["order_id"] == orderId }

                    if (mapped != null) {
                        val history = mapped.toIncomeList()
                        Resource.Success(history)
                    } else {
                        Resource.Empty
                    }
                } catch (e: GoogleJsonResponseException) {
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
