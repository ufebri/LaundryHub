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
import io.ktor.server.response.respondText
import io.ktor.server.request.receive
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing

import com.raylabs.laundryhub.backend.db.repository.GrossRepository
import com.raylabs.laundryhub.backend.db.repository.OutcomeRepository
import com.raylabs.laundryhub.backend.db.repository.PackageRepository
import com.raylabs.laundryhub.backend.db.repository.SummaryRepository
import com.raylabs.laundryhub.backend.routes.grossRoutes
import com.raylabs.laundryhub.backend.routes.outcomeRoutes
import com.raylabs.laundryhub.backend.routes.packageRoutes
import com.raylabs.laundryhub.backend.routes.summaryRoutes
import com.raylabs.laundryhub.backend.service.SheetsBatchSyncJob

fun Application.configureRouting() {
    val syncService = SheetsSyncService()
    val orderRepository = OrderRepository()
    val packageRepository = PackageRepository()
    val outcomeRepository = OutcomeRepository()
    val grossRepository = GrossRepository()
    val summaryRepository = SummaryRepository()
    val sheetsApiClient = GoogleSheetsApiClient(HttpClientProvider.createClient())

    // Start background sync job (Skip in tests to prevent DB connection errors)
    if (System.getProperty("isTest") != "true") {
        val spreadsheetId = System.getenv("SPREADSHEET_ID") ?: "14E1xk_RiQD5Vj1xpte4kPJhzpOFmUDLDEgZ_EmnkMS4"
        val syncJob = SheetsBatchSyncJob(orderRepository, syncService, spreadsheetId)
        syncJob.start()
    }

    routing {
        packageRoutes(packageRepository, sheetsApiClient)
        outcomeRoutes(outcomeRepository, sheetsApiClient)
        grossRoutes(grossRepository, sheetsApiClient)
        summaryRoutes(summaryRepository, sheetsApiClient)
        
        get("/") {
            call.respond(mapOf("status" to "OK", "message" to "LaundryHub KMP Backend is running"))
        }

        // --- CRUD Endpoints for Orders ---
        
        get("/api/orders") {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 50
            val filter = call.request.queryParameters["filter"]
            val startDate = call.request.queryParameters["startDate"]
            val endDate = call.request.queryParameters["endDate"]
            val orders = orderRepository.getAll(page, size, filter, startDate, endDate)
            call.respond(HttpStatusCode.OK, orders)
        }

        post("/api/orders") {
            try {
                val order = call.receive<OrderData>()
                val inserted = orderRepository.insert(order)
                if (inserted) {
                    call.respond(HttpStatusCode.Created, mapOf("status" to "Success", "message" to "Order created"))
                    // Trigger background sync here if needed
                } else {
                    call.respond(HttpStatusCode.Conflict, mapOf("status" to "Error", "message" to "Order already exists"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("status" to "Error", "message" to "Invalid data format"))
            }
        }

        put("/api/orders/{id}") {
            val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest, "Missing id")
            try {
                val order = call.receive<OrderData>()
                val updated = orderRepository.update(id, order)
                if (updated) {
                    call.respond(HttpStatusCode.OK, mapOf("status" to "Success", "message" to "Order updated"))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("status" to "Error", "message" to "Order not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("status" to "Error", "message" to "Invalid data format"))
            }
        }

        delete("/api/orders/{id}") {
            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing id")
            val deleted = orderRepository.delete(id)
            if (deleted) {
                call.respond(HttpStatusCode.OK, mapOf("status" to "Success", "message" to "Order deleted"))
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("status" to "Error", "message" to "Order not found"))
            }
        }

        // --- End CRUD Endpoints ---

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
                call.respond(response) // Kirim full response agar kita bisa lihat meta-nya
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, e.message ?: "Unknown error")
            }
        }

        get("/api/debug-metadata") {
            val spreadsheetId = call.request.queryParameters["spreadsheetId"]
            val accessToken = call.request.queryParameters["accessToken"]

            if (spreadsheetId == null || accessToken == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing spreadsheetId or accessToken")
                return@get
            }

            try {
                val metadata = sheetsApiClient.getSpreadsheet(spreadsheetId, accessToken)
                call.respondText(metadata, io.ktor.http.ContentType.Application.Json)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, e.message ?: "Unknown error")
            }
        }
    }
}
