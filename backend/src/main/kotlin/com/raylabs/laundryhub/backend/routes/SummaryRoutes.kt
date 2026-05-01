package com.raylabs.laundryhub.backend.routes

import com.raylabs.laundryhub.backend.db.repository.SummaryRepository
import com.raylabs.laundryhub.core.domain.model.sheets.SpreadsheetData
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

fun Route.summaryRoutes(
    repository: SummaryRepository,
    sheetsApiClient: GoogleSheetsApiClient
) {
    route("/api/summary") {
        get {
            call.respond(HttpStatusCode.OK, repository.getAll())
        }
        post {
            try {
                val summary = call.receive<SpreadsheetData>()
                val inserted = repository.insert(summary)
                if (inserted) call.respond(HttpStatusCode.Created, mapOf("status" to "Success"))
                else call.respond(HttpStatusCode.Conflict, mapOf("status" to "Error"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("status" to "Error", "message" to (e.message ?: "")))
            }
        }
        put("/{key}") {
            val key = call.parameters["key"] ?: return@put call.respond(HttpStatusCode.BadRequest)
            try {
                val summary = call.receive<SpreadsheetData>()
                val updated = repository.update(key, summary)
                if (updated) call.respond(HttpStatusCode.OK, mapOf("status" to "Success"))
                else call.respond(HttpStatusCode.NotFound, mapOf("status" to "Error"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("status" to "Error"))
            }
        }
        delete("/{key}") {
            val key = call.parameters["key"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            if (repository.delete(key)) call.respond(HttpStatusCode.OK, mapOf("status" to "Success"))
            else call.respond(HttpStatusCode.NotFound, mapOf("status" to "Error"))
        }
        
        post("/migrate") {
            val spreadsheetId = call.request.queryParameters["spreadsheetId"]
            val accessToken = call.request.queryParameters["accessToken"]
            if (spreadsheetId == null || accessToken == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing params")
                return@post
            }
            try {
                val response = sheetsApiClient.getValues(spreadsheetId, "summary", accessToken)
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
    }
}
