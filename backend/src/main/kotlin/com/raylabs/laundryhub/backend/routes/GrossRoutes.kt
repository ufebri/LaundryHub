package com.raylabs.laundryhub.backend.routes

import com.raylabs.laundryhub.backend.db.repository.GrossRepository
import com.raylabs.laundryhub.backend.db.repository.OrderRepository
import com.raylabs.laundryhub.backend.service.SheetsSyncService
import com.raylabs.laundryhub.core.domain.model.sheets.GrossData
import com.raylabs.laundryhub.core.domain.model.sheets.grossMonthKey
import com.raylabs.laundryhub.core.domain.model.sheets.sortedByGrossMonthDescending
import com.raylabs.laundryhub.shared.network.api.GoogleSheetsApiClient
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import java.util.Calendar

fun Route.grossRoutes(
    repository: GrossRepository,
    sheetsApiClient: GoogleSheetsApiClient,
    syncService: SheetsSyncService,
    spreadsheetId: String?,
    orderRepository: OrderRepository? = null,
    migrationRoutesEnabled: Boolean = false
) {
    route("/api/gross") {
        get {
            handleGetGross(call, repository, syncService, spreadsheetId, orderRepository)
        }
        post {
            handlePostGross(call, repository)
        }
        put("/{month}") {
            handlePutGross(call, repository)
        }
        delete("/{month}") {
            handleDeleteGross(call, repository)
        }
        
        if (migrationRoutesEnabled) {
            post("/migrate") {
                handleMigrateGross(call, repository, sheetsApiClient)
            }
        }
    }
}

private suspend fun handleGetGross(
    call: io.ktor.server.application.ApplicationCall,
    repository: GrossRepository,
    syncService: SheetsSyncService,
    spreadsheetId: String?,
    orderRepository: OrderRepository?
) {
    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
    val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 50
    call.respond(HttpStatusCode.OK, fetchGrossForResponse(repository, syncService, spreadsheetId, page, size, orderRepository))
}

private suspend fun handlePostGross(
    call: io.ktor.server.application.ApplicationCall,
    repository: GrossRepository
) {
    try {
        val gross = call.receive<GrossData>()
        val inserted = repository.insert(gross)
        if (inserted) {
            call.respond(HttpStatusCode.Created, mapOf("status" to "Success"))
        } else {
            call.respond(HttpStatusCode.Conflict, mapOf("status" to "Error"))
        }
    } catch (e: Exception) {
        call.respond(HttpStatusCode.BadRequest, mapOf("status" to "Error", "message" to (e.message ?: "")))
    }
}

private suspend fun handlePutGross(
    call: io.ktor.server.application.ApplicationCall,
    repository: GrossRepository
) {
    val month = call.parameters["month"] ?: return call.respond(HttpStatusCode.BadRequest)
    try {
        val gross = call.receive<GrossData>()
        val updated = repository.update(month, gross)
        if (updated) {
            call.respond(HttpStatusCode.OK, mapOf("status" to "Success"))
        } else {
            call.respond(HttpStatusCode.NotFound, mapOf("status" to "Error"))
        }
    } catch (e: Exception) {
        call.respond(HttpStatusCode.BadRequest, mapOf("status" to "Error"))
    }
}

private suspend fun handleDeleteGross(
    call: io.ktor.server.application.ApplicationCall,
    repository: GrossRepository
) {
    val month = call.parameters["month"] ?: return call.respond(HttpStatusCode.BadRequest)
    if (repository.delete(month)) {
        call.respond(HttpStatusCode.OK, mapOf("status" to "Success", "sheetSynced" to "sheet-owned"))
    } else {
        call.respond(HttpStatusCode.NotFound, mapOf("status" to "Error"))
    }
}

private suspend fun handleMigrateGross(
    call: io.ktor.server.application.ApplicationCall,
    repository: GrossRepository,
    sheetsApiClient: GoogleSheetsApiClient
) {
    val migrationSpreadsheetId = call.request.queryParameters["spreadsheetId"]
    val accessToken = call.request.queryParameters["accessToken"]
    if (migrationSpreadsheetId == null || accessToken == null) {
        call.respond(HttpStatusCode.BadRequest, "Missing params")
        return
    }
    try {
        val response = sheetsApiClient.getValues(migrationSpreadsheetId, "gross", accessToken)
        val rows = response.values ?: emptyList()
        val grossList = rows.drop(1).mapNotNull { row ->
            if (row.isEmpty()) return@mapNotNull null
            GrossData(
                month = row.getOrNull(0) ?: "",
                totalNominal = row.getOrNull(1) ?: "",
                orderCount = row.getOrNull(2) ?: "",
                tax = row.getOrNull(3) ?: ""
            )
        }
        val inserted = repository.insertAll(grossList)
        call.respond(HttpStatusCode.OK, mapOf("migrated" to inserted))
    } catch (e: Exception) {
        call.respond(HttpStatusCode.InternalServerError, e.message ?: "Error")
    }
}

internal suspend fun fetchGrossForResponse(
    repository: GrossRepository,
    syncService: SheetsSyncService,
    spreadsheetId: String?,
    page: Int,
    size: Int,
    orderRepository: OrderRepository? = null,
    currentYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    currentMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1
): List<GrossData> {
    val sheetGross = spreadsheetId
        ?.let { syncService.fetchGrossFromSheet(it) }
        .orEmpty()
    val sourceRows = sheetGross.ifEmpty { repository.getAll(page, size) }
    val rows = sourceRows.withCurrentMonthGross(orderRepository, currentYear, currentMonth)

    val safePage = page.coerceAtLeast(1)
    val safeSize = size.coerceAtLeast(1)
    val offset = (safePage - 1) * safeSize
    return rows
        .sortedByGrossMonthDescending()
        .drop(offset)
        .take(safeSize)
}

private suspend fun List<GrossData>.withCurrentMonthGross(
    orderRepository: OrderRepository?,
    currentYear: Int,
    currentMonth: Int
): List<GrossData> {
    if (orderRepository == null) return this
    val currentKey = currentYear * 100 + currentMonth
    if (any { it.grossMonthKey() == currentKey }) return this
    val computedGross = orderRepository.getGrossForMonth(currentYear, currentMonth) ?: return this
    return this + computedGross
}
