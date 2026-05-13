package com.raylabs.laundryhub.backend.service

import com.google.auth.oauth2.GoogleCredentials
import com.raylabs.laundryhub.core.domain.model.sheets.GrossData
import com.raylabs.laundryhub.core.domain.model.sheets.OrderData
import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.core.domain.model.sheets.PackageData
import com.raylabs.laundryhub.core.domain.model.sheets.SpreadsheetData
import com.raylabs.laundryhub.core.domain.model.sheets.toSheetValues
import com.raylabs.laundryhub.shared.network.HttpClientProvider
import com.raylabs.laundryhub.shared.network.api.GoogleSheetsApiClient
import com.raylabs.laundryhub.shared.network.model.sheets.ValueRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream

class SheetsSyncService {

    private val logger = LoggerFactory.getLogger(SheetsSyncService::class.java)

    private val httpClient = HttpClientProvider.createClient(enableLogging = true)
    private val sheetsApiClient = GoogleSheetsApiClient(httpClient)

    private fun getServiceAccountToken(): String {
        val jsonEnv = System.getenv("GOOGLE_SERVICE_ACCOUNT_JSON")
        if (jsonEnv.isNullOrBlank()) {
            throw IllegalStateException("GOOGLE_SERVICE_ACCOUNT_JSON environment variable is not set")
        }
        
        val credentials = GoogleCredentials.fromStream(ByteArrayInputStream(jsonEnv.toByteArray()))
            .createScoped(listOf("https://www.googleapis.com/auth/spreadsheets"))
        
        credentials.refreshIfExpired()
        return credentials.accessToken.tokenValue
    }

    /**
     * Finds the row index (1-based for A1 notation) of an Order ID in the spreadsheet.
     * Returns -1 if not found.
     */
    private suspend fun findRowIndex(spreadsheetId: String, id: String, range: String, token: String): Int {
        val response = sheetsApiClient.getValues(spreadsheetId, range, token)
        val values = response.values ?: return -1
        
        val index = values.indexOfFirst { it.getOrNull(0) == id }
        return if (index != -1) index + 1 else -1 // +1 for 0-indexed to 1-indexed, but getValues usually includes header or start from A2
    }

    /**
     * Smart Sync: If the order exists, UPDATE it. If not, APPEND it.
     */
    suspend fun syncOrder(spreadsheetId: String, order: OrderData): Boolean = withContext(Dispatchers.IO) {
        try {
            val token = getServiceAccountToken()
            val sheetName = "income"
            
            // 1. Coba cari apakah ID sudah ada di sheet (Cek kolom A)
            val rows = sheetsApiClient.getValues(spreadsheetId, "$sheetName!A:A", token).values ?: emptyList()
            val rowIndex = rows.indexOfFirst { it.getOrNull(0) == order.orderId }

            val valueRange = ValueRange(
                range = sheetName,
                majorDimension = "ROWS",
                values = listOf(
                    listOf(
                        order.orderId, order.orderDate, order.name, order.weight, 
                        order.priceKg, order.totalPrice, order.paidStatus, 
                        order.packageName, order.remark, order.paymentMethod, 
                        order.phoneNumber, order.dueDate
                    )
                )
            )

            if (rowIndex != -1) {
                // DATA ADA -> Gunakan UPDATE_VALUES
                logger.info("Order ${order.orderId} found at row ${rowIndex + 1}. Updating...")
                val updateRange = "$sheetName!A${rowIndex + 1}:L${rowIndex + 1}"
                sheetsApiClient.updateValues(spreadsheetId, updateRange, valueRange, token)
            } else {
                // DATA TIDAK ADA -> Gunakan APPEND_VALUES
                logger.info("Order ${order.orderId} not found. Appending...")
                sheetsApiClient.appendValues(spreadsheetId, sheetName, valueRange, token)
            }
            true
        } catch (e: Exception) {
            logger.error("Error syncing order ${order.orderId}: ${e.message}")
            false
        }
    }

    /**
     * Smart Delete: Finds the row and CLEARS it.
     */
    suspend fun deleteOrderFromSheet(spreadsheetId: String, orderId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val token = getServiceAccountToken()
            val sheetName = "income"
            
            val rows = sheetsApiClient.getValues(spreadsheetId, "$sheetName!A:A", token).values ?: emptyList()
            val rowIndex = rows.indexOfFirst { it.getOrNull(0) == orderId }

            if (rowIndex != -1) {
                logger.info("Order $orderId found at row ${rowIndex + 1}. Clearing...")
                val clearRange = "$sheetName!A${rowIndex + 1}:L${rowIndex + 1}"
                sheetsApiClient.clearValues(spreadsheetId, clearRange, token)
                true
            } else {
                logger.info("Order $orderId not found in sheet. Nothing to clear.")
                true
            }
        } catch (e: Exception) {
            logger.error("Error clearing order $orderId: ${e.message}")
            false
        }
    }

    suspend fun syncOutcome(spreadsheetId: String, outcome: OutcomeData): Boolean = withContext(Dispatchers.IO) {
        try {
            val token = getServiceAccountToken()
            val sheetName = "outcome"
            val rowIndex = findRowIndex(spreadsheetId, outcome.id, "$sheetName!A:A", token)
            val valueRange = ValueRange(
                range = sheetName,
                majorDimension = "ROWS",
                values = outcome.toSheetValues()
            )

            if (rowIndex != -1) {
                logger.info("Outcome ${outcome.id} found at row $rowIndex. Updating...")
                sheetsApiClient.updateValues(spreadsheetId, "$sheetName!A$rowIndex:F$rowIndex", valueRange, token)
            } else {
                logger.info("Outcome ${outcome.id} not found. Appending...")
                sheetsApiClient.appendValues(spreadsheetId, sheetName, valueRange, token)
            }
            true
        } catch (e: Exception) {
            logger.error("Error syncing outcome ${outcome.id}: ${e.message}")
            false
        }
    }

    suspend fun syncPackage(spreadsheetId: String, pkg: PackageData): Boolean = withContext(Dispatchers.IO) {
        try {
            val token = getServiceAccountToken()
            val sheetName = "notes"
            val rowIndex = findRowIndex(spreadsheetId, pkg.name, "$sheetName!B:B", token)
            val valueRange = ValueRange(
                range = sheetName,
                majorDimension = "ROWS",
                values = pkg.toSheetValues()
            )

            if (rowIndex != -1) {
                logger.info("Package ${pkg.name} found at row $rowIndex. Updating...")
                sheetsApiClient.updateValues(spreadsheetId, "$sheetName!A$rowIndex:D$rowIndex", valueRange, token)
            } else {
                logger.info("Package ${pkg.name} not found. Appending...")
                sheetsApiClient.appendValues(spreadsheetId, sheetName, valueRange, token)
            }
            true
        } catch (e: Exception) {
            logger.error("Error syncing package ${pkg.name}: ${e.message}")
            false
        }
    }

    suspend fun deleteOutcomeFromSheet(spreadsheetId: String, outcomeId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val token = getServiceAccountToken()
            val sheetName = "outcome"
            val rowIndex = findRowIndex(spreadsheetId, outcomeId, "$sheetName!A:A", token)

            if (rowIndex != -1) {
                logger.info("Outcome $outcomeId found at row $rowIndex. Clearing...")
                sheetsApiClient.clearValues(spreadsheetId, "$sheetName!A$rowIndex:F$rowIndex", token)
            } else {
                logger.info("Outcome $outcomeId not found in sheet. Nothing to clear.")
            }
            true
        } catch (e: Exception) {
            logger.error("Error clearing outcome $outcomeId: ${e.message}")
            false
        }
    }

    suspend fun deletePackageFromSheet(spreadsheetId: String, packageName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val token = getServiceAccountToken()
            val sheetName = "notes"
            val rowIndex = findRowIndex(spreadsheetId, packageName, "$sheetName!B:B", token)

            if (rowIndex != -1) {
                logger.info("Package $packageName found at row $rowIndex. Clearing...")
                sheetsApiClient.clearValues(spreadsheetId, "$sheetName!A$rowIndex:D$rowIndex", token)
            } else {
                logger.info("Package $packageName not found in sheet. Nothing to clear.")
            }
            true
        } catch (e: Exception) {
            logger.error("Error clearing package $packageName: ${e.message}")
            false
        }
    }

    suspend fun fetchOrdersFromSheet(spreadsheetId: String): List<OrderData> = withContext(Dispatchers.IO) {
        try {
            val token = getServiceAccountToken()
            val sheetName = "income"
            // Get all data, assuming row 1 is header
            val response = sheetsApiClient.getValues(spreadsheetId, "$sheetName!A2:L", token)
            val values = response.values ?: return@withContext emptyList()

            values.mapNotNull { row ->
                try {
                    OrderData(
                        orderId = row.getOrNull(0) ?: "",
                        orderDate = row.getOrNull(1) ?: "",
                        name = row.getOrNull(2) ?: "",
                        weight = row.getOrNull(3) ?: "",
                        priceKg = row.getOrNull(4) ?: "",
                        totalPrice = row.getOrNull(5) ?: "",
                        paidStatus = row.getOrNull(6) ?: "",
                        packageName = row.getOrNull(7) ?: "",
                        remark = row.getOrNull(8) ?: "",
                        paymentMethod = row.getOrNull(9) ?: "",
                        phoneNumber = row.getOrNull(10) ?: "",
                        dueDate = row.getOrNull(11) ?: ""
                    ).takeIf { it.orderId.isNotBlank() }
                } catch (e: Exception) {
                    logger.warn("Skipping invalid row: $row")
                    null
                }
            }
        } catch (e: Exception) {
            logger.error("Error fetching orders from sheets: ${e.message}")
            emptyList()
        }
    }

    suspend fun fetchOutcomesFromSheet(spreadsheetId: String): List<OutcomeData> = withContext(Dispatchers.IO) {
        try {
            val token = getServiceAccountToken()
            val sheetName = "outcome"
            val response = sheetsApiClient.getValues(spreadsheetId, "$sheetName!A2:F", token)
            val values = response.values ?: return@withContext emptyList()

            values.mapNotNull { row ->
                try {
                    OutcomeData(
                        id = row.getOrNull(0) ?: "",
                        date = row.getOrNull(1) ?: "",
                        purpose = row.getOrNull(2) ?: "",
                        price = row.getOrNull(3) ?: "",
                        remark = row.getOrNull(4) ?: "",
                        payment = row.getOrNull(5) ?: ""
                    ).takeIf { it.id.isNotBlank() }
                } catch (e: Exception) {
                    logger.warn("Skipping invalid row: $row")
                    null
                }
            }
        } catch (e: Exception) {
            logger.error("Error fetching outcomes from sheets: ${e.message}")
            emptyList()
        }
    }

    suspend fun fetchPackagesFromSheet(spreadsheetId: String): List<PackageData> = withContext(Dispatchers.IO) {
        try {
            val token = getServiceAccountToken()
            val sheetName = "notes"
            // Usually starts at B for packages
            val response = sheetsApiClient.getValues(spreadsheetId, "$sheetName!A2:D", token)
            val values = response.values ?: return@withContext emptyList()

            values.mapNotNull { row ->
                try {
                    PackageData(
                        price = row.getOrNull(0) ?: "",
                        name = row.getOrNull(1) ?: "",
                        duration = row.getOrNull(2) ?: "",
                        unit = row.getOrNull(3) ?: "Kg" // Defaulting to Kg if missing
                    ).takeIf { it.name.isNotBlank() }
                } catch (e: Exception) {
                    logger.warn("Skipping invalid row: $row")
                    null
                }
            }
        } catch (e: Exception) {
            logger.error("Error fetching packages from sheets: ${e.message}")
            emptyList()
        }
    }

    suspend fun fetchGrossFromSheet(spreadsheetId: String): List<GrossData> = withContext(Dispatchers.IO) {
        try {
            val token = getServiceAccountToken()
            val sheetName = "gross"
            val response = sheetsApiClient.getValues(spreadsheetId, "$sheetName!A2:D", token)
            val values = response.values ?: return@withContext emptyList()

            values.mapNotNull { row ->
                if (row.isEmpty()) return@mapNotNull null
                try {
                    GrossData(
                        month = row.getOrNull(0) ?: "",
                        totalNominal = row.getOrNull(1) ?: "",
                        orderCount = row.getOrNull(2) ?: "",
                        tax = row.getOrNull(3) ?: ""
                    ).takeIf { it.month.isNotBlank() }
                } catch (e: Exception) {
                    logger.warn("Skipping invalid row: $row")
                    null
                }
            }
        } catch (e: Exception) {
            logger.error("Error fetching gross from sheets: ${e.message}")
            emptyList()
        }
    }

    suspend fun fetchSummaryFromSheet(spreadsheetId: String): List<SpreadsheetData> = withContext(Dispatchers.IO) {
        try {
            val token = getServiceAccountToken()
            val sheetName = "summary"
            val response = sheetsApiClient.getValues(spreadsheetId, "$sheetName!A2:B", token)
            val values = response.values ?: return@withContext emptyList()

            values.mapNotNull { row ->
                if (row.isEmpty()) return@mapNotNull null
                try {
                    SpreadsheetData(
                        key = row.getOrNull(0) ?: "",
                        value = row.getOrNull(1) ?: ""
                    ).takeIf { it.key.isNotBlank() }
                } catch (e: Exception) {
                    logger.warn("Skipping invalid row: $row")
                    null
                }
            }
        } catch (e: Exception) {
            logger.error("Error fetching summary from sheets: ${e.message}")
            emptyList()
        }
    }
}
