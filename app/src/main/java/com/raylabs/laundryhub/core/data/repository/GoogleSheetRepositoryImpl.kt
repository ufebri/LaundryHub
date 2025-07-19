package com.raylabs.laundryhub.core.data.repository

import android.util.Log
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.sheets.v4.model.ValueRange
import com.raylabs.laundryhub.BuildConfig
import com.raylabs.laundryhub.core.data.service.GoogleSheetService
import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.model.sheets.HistoryData
import com.raylabs.laundryhub.core.domain.model.sheets.HistoryFilter
import com.raylabs.laundryhub.core.domain.model.sheets.InventoryData
import com.raylabs.laundryhub.core.domain.model.sheets.OrderData
import com.raylabs.laundryhub.core.domain.model.sheets.PackageData
import com.raylabs.laundryhub.core.domain.model.sheets.RangeDate
import com.raylabs.laundryhub.core.domain.model.sheets.STATUS_ORDER_PENDING
import com.raylabs.laundryhub.core.domain.model.sheets.SpreadsheetData
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.model.sheets.filterRangeDateData
import com.raylabs.laundryhub.core.domain.model.sheets.getAllIncomeData
import com.raylabs.laundryhub.core.domain.model.sheets.getTodayIncomeData
import com.raylabs.laundryhub.core.domain.model.sheets.groupStatus
import com.raylabs.laundryhub.core.domain.model.sheets.isCashData
import com.raylabs.laundryhub.core.domain.model.sheets.isPaidData
import com.raylabs.laundryhub.core.domain.model.sheets.isQRISData
import com.raylabs.laundryhub.core.domain.model.sheets.isUnpaidData
import com.raylabs.laundryhub.core.domain.model.sheets.toHistoryData
import com.raylabs.laundryhub.core.domain.model.sheets.toIncomeList
import com.raylabs.laundryhub.core.domain.model.sheets.toInventoryData
import com.raylabs.laundryhub.core.domain.model.sheets.toPackageData
import com.raylabs.laundryhub.core.domain.model.sheets.toSheetRow
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
        private const val HISTORY_RANGE = "history!A1:V"
        private const val INVENTORY_RANGE = "station!A1:E"
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
                        parseDate(transaction.dueDate.orEmpty())
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

    override suspend fun readInventoryData(): Resource<List<InventoryData>> {
        return withContext(Dispatchers.IO) {
            retry {
                try {
                    val response = googleSheetService.getSheetsService().spreadsheets().values()
                        .get(BuildConfig.SPREAD_SHEET_ID, INVENTORY_RANGE).execute()

                    val headers = response.getValues().firstOrNull() ?: emptyList()
                    val dataRows = response.getValues().drop(1)

                    val data = dataRows.map { row ->
                        val mappedRow = headers.zip(row).associate {
                            it.first.toString() to it.second?.toString().orEmpty()
                        }
                        mappedRow.toInventoryData()
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

    override suspend fun getAvailableMachineByStation(stationType: String): Resource<List<InventoryData>> {
        return withContext(Dispatchers.IO) {
            retry {
                try {
                    val response = googleSheetService.getSheetsService().spreadsheets().values()
                        .get(BuildConfig.SPREAD_SHEET_ID, INVENTORY_RANGE).execute()

                    val headers = response.getValues().firstOrNull() ?: emptyList()
                    val dataRows = response.getValues().drop(1)

                    val machines = dataRows.map { row ->
                        val mappedRow = headers.zip(row).associate {
                            it.first.toString() to it.second?.toString().orEmpty()
                        }
                        mappedRow.toInventoryData()
                    }.filter {
                        it.stationType.equals(
                            stationType,
                            ignoreCase = true
                        ) && it.isAvailable
                    }

                    if (machines.isNotEmpty()) Resource.Success(machines)
                    else Resource.Error("No available machine for $stationType")
                } catch (e: Exception) {
                    Resource.Error(e.message ?: "Unexpected Error")
                }
            } ?: Resource.Error("Failed after 3 attempts.")
        }
    }

    override suspend fun updateMachineAvailability(
        idMachine: String,
        isAvailable: Boolean
    ): Resource<Boolean> {
        return withContext(Dispatchers.IO) {
            retry {
                try {
                    val rowIndex = findInventoryRowById(idMachine)
                        ?: return@retry Resource.Error("Machine with ID $idMachine not found")

                    val response = googleSheetService.getSheetsService().spreadsheets().values()
                        .get(BuildConfig.SPREAD_SHEET_ID, INVENTORY_RANGE).execute()

                    val headers = response.getValues().firstOrNull() ?: emptyList()
                    val columnIndex = headers.indexOf("is_available") + 1

                    // Convert column index to letter (A=1, B=2, ...)
                    val columnLetter = ('A' + (columnIndex - 1)).toString()
                    val cell = "station!$columnLetter$rowIndex"

                    val body = ValueRange().setValues(
                        listOf(listOf(if (isAvailable) "TRUE" else "FALSE"))
                    )

                    googleSheetService.getSheetsService().spreadsheets().values()
                        .update(BuildConfig.SPREAD_SHEET_ID, cell, body)
                        .setValueInputOption("USER_ENTERED")
                        .execute()

                    Resource.Success(true)
                } catch (e: Exception) {
                    Resource.Error(e.message ?: "Failed to update machine availability.")
                }
            } ?: Resource.Error("Failed after 3 attempts.")
        }
    }

    private fun findInventoryRowById(id: String): Int? {
        val response = googleSheetService.getSheetsService().spreadsheets().values()
            .get(BuildConfig.SPREAD_SHEET_ID, INVENTORY_RANGE)
            .execute()

        val rows = response.getValues()
        val index = rows.indexOfFirst { it.firstOrNull() == id }
        return if (index >= 0) index + 2 else null
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
                            order.phoneNumber,
                            STATUS_ORDER_PENDING, // orderStatus awal,
                            order.getSpreadSheetDueDate
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

    override suspend fun addHistoryOrder(history: HistoryData): Resource<Boolean> {
        return withContext(Dispatchers.IO) {
            retry {
                try {
                    val values = listOf(history.toSheetRow())

                    val body = ValueRange().setValues(values)
                    googleSheetService.getSheetsService().spreadsheets().values()
                        .append(BuildConfig.SPREAD_SHEET_ID, HISTORY_RANGE, body)
                        .setValueInputOption("USER_ENTERED")
                        .execute()

                    Resource.Success(true)

                } catch (e: Exception) {
                    Resource.Error(e.message ?: "Failed to append order.")
                }
            } ?: Resource.Error("Gagal menambahkan order setelah 3 kali coba.")
        }
    }

    override suspend fun getOrderById(orderId: String): Resource<HistoryData> {
        return withContext(Dispatchers.IO) {
            retry {
                try {
                    val response = googleSheetService
                        .getSheetsService()
                        .spreadsheets()
                        .values()
                        .get(BuildConfig.SPREAD_SHEET_ID, HISTORY_RANGE)
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
                        val history = mapped.toHistoryData()
                        val groupStatus = history.groupStatus()
                        val enriched = history.copy(status = groupStatus)
                        Resource.Success(enriched)
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

    override suspend fun updateOrderStep(
        orderId: String,
        step: String,
        startedAt: String,
        machineName: String
    ): Resource<Boolean> {
        return withContext(Dispatchers.IO) {
            retry {
                try {
                    val rowIndex = findRowByOrderId(orderId)
                        ?: return@retry Resource.Error("Order ID not found")

                    val (dateCol, machineCol) = when (step) {
                        "Washing" -> "H" to "I"
                        "Drying" -> "J" to "K"
                        "Ironing" -> "L" to "M"
                        "Folding" -> "N" to "O"
                        "Packing" -> "P" to null // Only date for Packing
                        "Ready" -> "Q" to null // Only date for Ready, Q is now the ready date column
                        "Completed" -> "R" to null // Only date for Completed
                        else -> return@retry Resource.Error("Unknown step: $step")
                    }

                    val service = googleSheetService.getSheetsService()

                    val updateDate = ValueRange().setValues(listOf(listOf(startedAt)))
                    Log.d("GoogleSheetRepo", "Updating step '$step' at history!$dateCol$rowIndex with date: $startedAt")
                    service.spreadsheets().values()
                        .update(
                            BuildConfig.SPREAD_SHEET_ID,
                            "history!$dateCol$rowIndex",
                            updateDate
                        )
                        .setValueInputOption("RAW")
                        .execute()

                    if (machineCol != null) {
                        Log.d("GoogleSheetRepo", "Updating machineCol '$machineCol' at history!$machineCol$rowIndex with machineName: $machineName")
                        val updateMachine = ValueRange().setValues(listOf(listOf(machineName)))
                        service.spreadsheets().values()
                            .update(
                                BuildConfig.SPREAD_SHEET_ID,
                                "history!$machineCol$rowIndex",
                                updateMachine
                            )
                            .setValueInputOption("RAW")
                            .execute()
                    }

                    Resource.Success(true)
                } catch (e: Exception) {
                    Resource.Error(e.message ?: "Failed to update step")
                }
            }
        } ?: Resource.Error("Failed after 3 attempts.")
    }

    private fun findRowByOrderId(orderId: String): Int? {
        val response = googleSheetService.getSheetsService().spreadsheets().values()
            .get(BuildConfig.SPREAD_SHEET_ID, HISTORY_RANGE)
            .execute()

        val rows = response.getValues()
        if (rows.isEmpty()) return null
        // Lewati header (index 0), cari di data saja
        rows.forEachIndexed { idx, row ->
            val sheetOrderId = row.firstOrNull()?.toString()?.trim()
            Log.d("GoogleSheetRepo", "Row ${idx + 1}: orderId=$sheetOrderId (target=$orderId)")
        }
        val dataRows = rows.drop(1) // index 0 = header
        val index = dataRows.indexOfFirst { it.firstOrNull()?.toString()?.trim() == orderId.trim() }
        Log.d("GoogleSheetRepo", "Result index for orderId=$orderId is $index (dataRows, offset+2)")
        return if (index >= 0) index + 2 else null // +2 karena baris 2 di sheet = dataRows[0]
    }
}
