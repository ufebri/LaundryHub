package com.raylabs.laundryhub.backend.routes

import com.raylabs.laundryhub.backend.db.repository.SummaryRepository
import com.raylabs.laundryhub.backend.service.SheetsSyncService
import com.raylabs.laundryhub.core.domain.model.sheets.SpreadsheetData
import com.raylabs.laundryhub.shared.network.api.GoogleSheetsApiClient
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.summaryRoutes(
    repository: SummaryRepository,
    sheetsApiClient: GoogleSheetsApiClient,
    syncService: SheetsSyncService,
    spreadsheetId: String?,
    migrationRoutesEnabled: Boolean = false
) {
    route("/api/summary") {
        get {
            handleGetSummary(call, repository, syncService, spreadsheetId)
        }
        post {
            handlePostSummary(call, repository)
        }
        put("/{key}") {
            handlePutSummary(call, repository)
        }
        delete("/{key}") {
            handleDeleteSummary(call, repository)
        }

        if (migrationRoutesEnabled) {
            post("/migrate") {
                handleMigrateSummary(call, repository, sheetsApiClient)
            }
        }
    }
}

private suspend fun handleGetSummary(
    call: ApplicationCall,
    repository: SummaryRepository,
    syncService: SheetsSyncService,
    spreadsheetId: String?
) {
    val sheetSummary = spreadsheetId
        ?.let { syncService.fetchSummaryFromSheet(it) }
        .orEmpty()
    call.respond(HttpStatusCode.OK, sheetSummary.ifEmpty { repository.getAll() })
}

private suspend fun handlePostSummary(
    call: ApplicationCall,
    repository: SummaryRepository
) {
    try {
        val summary = call.receive<SpreadsheetData>()
        val inserted = repository.insert(summary)
        if (inserted) {
            call.respond(HttpStatusCode.Created, mapOf("status" to "Success"))
        } else {
            call.respond(HttpStatusCode.Conflict, mapOf("status" to "Error"))
        }
    } catch (e: Exception) {
        call.respond(HttpStatusCode.BadRequest, mapOf("status" to "Error", "message" to (e.message ?: "")))
    }
}

private suspend fun handlePutSummary(
    call: ApplicationCall,
    repository: SummaryRepository
) {
    val key = call.parameters["key"] ?: return call.respond(HttpStatusCode.BadRequest)
    try {
        val summary = call.receive<SpreadsheetData>()
        val updated = repository.update(key, summary)
        if (updated) {
            call.respond(HttpStatusCode.OK, mapOf("status" to "Success"))
        } else {
            call.respond(HttpStatusCode.NotFound, mapOf("status" to "Error"))
        }
    } catch (e: Exception) {
        call.respond(HttpStatusCode.BadRequest, mapOf("status" to "Error"))
    }
}

private suspend fun handleDeleteSummary(
    call: ApplicationCall,
    repository: SummaryRepository
) {
    val key = call.parameters["key"] ?: return call.respond(HttpStatusCode.BadRequest)
    if (repository.delete(key)) {
        call.respond(HttpStatusCode.OK, mapOf("status" to "Success", "sheetSynced" to "sheet-owned"))
    } else {
        call.respond(HttpStatusCode.NotFound, mapOf("status" to "Error"))
    }
}

private suspend fun handleMigrateSummary(
    call: ApplicationCall,
    repository: SummaryRepository,
    sheetsApiClient: GoogleSheetsApiClient
) {
    val migrationSpreadsheetId = call.request.queryParameters["spreadsheetId"]
    val accessToken = call.request.queryParameters["accessToken"]
    if (migrationSpreadsheetId == null || accessToken == null) {
        call.respond(HttpStatusCode.BadRequest, "Missing params")
        return
    }
    try {
        val response = sheetsApiClient.getValues(migrationSpreadsheetId, "summary", accessToken)
        val rows = response.values ?: emptyList()
        val summaries = rows.drop(1).mapNotNull { row ->
            if (row.isEmpty()) return@mapNotNull null
            SpreadsheetData(
                key = row.getOrNull(0) ?: "",
                value = row.getOrNull(1) ?: ""
            )
        }
        val inserted = repository.insertAll(summaries)
        call.respond(HttpStatusCode.OK, mapOf("migrated" to inserted))
    } catch (e: Exception) {
        call.respond(HttpStatusCode.InternalServerError, e.message ?: "Error")
    }
}
