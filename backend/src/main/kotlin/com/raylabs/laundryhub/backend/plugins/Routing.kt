package com.raylabs.laundryhub.backend.plugins

import com.raylabs.laundryhub.backend.db.repository.OrderRepository
import com.raylabs.laundryhub.backend.service.SheetsSyncService
import com.raylabs.laundryhub.core.domain.model.sheets.OrderData
import com.raylabs.laundryhub.shared.network.HttpClientProvider
import com.raylabs.laundryhub.shared.network.api.GoogleSheetsApiClient
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    val syncService = SheetsSyncService()
    val orderRepository = OrderRepository()
    val sheetsApiClient = GoogleSheetsApiClient(HttpClientProvider.createClient())

    routing {
        get("/") {
            call.respond(mapOf("status" to "OK", "message" to "LaundryHub KMP Backend is running"))
        }

        get("/api/test-shared") {
            // Test that we can use models from the shared module
            val dummyOrder = OrderData(
                orderId = "ORD-123",
                name = "John Doe",
                phoneNumber = "08123456789",
                packageName = "Cuci Komplit",
                weight = "5",
                priceKg = "6000",
                totalPrice = "30000",
                orderDate = "2024-05-01",
                dueDate = "2024-05-03",
                paidStatus = "Paid",
                paymentMethod = "Cash",
                remark = "Baju warna putih dipisah"
            )
            call.respond(HttpStatusCode.OK, dummyOrder)
        }

        get("/api/sync") {
            val spreadsheetId = call.request.queryParameters["spreadsheetId"]
            val range = call.request.queryParameters["range"] ?: "Sheet1!A1"
            val accessToken = call.request.queryParameters["accessToken"]

            if (spreadsheetId == null || accessToken == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("status" to "Error", "message" to "Missing spreadsheetId or accessToken")
                )
                return@get
            }

            val success = syncService.syncDataToSheet(spreadsheetId, range, accessToken)
            if (success) {
                call.respond(HttpStatusCode.OK, mapOf("status" to "Success", "message" to "Data synced successfully"))
            } else {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("status" to "Error", "message" to "Failed to sync data")
                )
            }
        }

        post("/api/migrate-orders") {
            val spreadsheetId = call.request.queryParameters["spreadsheetId"]
            val accessToken = call.request.queryParameters["accessToken"]
            val range = "income" // Based on typical order columns

            if (spreadsheetId == null || accessToken == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("status" to "Error", "message" to "Missing spreadsheetId or accessToken")
                )
                return@post
            }

            try {
                // 1. Fetch from legacy Google Sheets
                val response = sheetsApiClient.getValues(spreadsheetId, range, accessToken)
                val rows = response.values ?: emptyList()

                // 2. Map to Shared DTO
                val orders = rows.mapNotNull { row ->
                    if (row.isEmpty()) return@mapNotNull null // Lewati baris yang benar-benar kosong
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
                    )
                }

                // 3. Save to new PostgreSQL Database
                val insertedCount = orderRepository.insertAll(orders)

                call.respond(
                    HttpStatusCode.OK, 
                    mapOf(
                        "status" to "Success", 
                        "message" to "Migrated $insertedCount orders from Google Sheets to PostgreSQL successfully"
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("status" to "Error", "message" to (e.message ?: "Unknown error during migration"))
                )
            }
        }

        get("/api/debug-sheets") {
            val spreadsheetId = call.request.queryParameters["spreadsheetId"]
            val accessToken = call.request.queryParameters["accessToken"]
            val range = "income"

            if (spreadsheetId == null || accessToken == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing spreadsheetId or accessToken")
                return@get
            }

            try {
                val response = sheetsApiClient.getValues(spreadsheetId, range, accessToken)
                call.respond(response.values ?: emptyList<List<String>>())
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, e.message ?: "Unknown error")
            }
        }
    }
}
