package com.raylabs.laundryhub.backend.routes

import com.raylabs.laundryhub.backend.db.repository.GrossRepository
import com.raylabs.laundryhub.core.domain.model.sheets.GrossData
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

fun Route.grossRoutes(
    repository: GrossRepository,
    sheetsApiClient: GoogleSheetsApiClient
) {
    route("/api/gross") {
        get {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 50
            call.respond(HttpStatusCode.OK, repository.getAll(page, size))
        }
        post {
            try {
                val gross = call.receive<GrossData>()
                val inserted = repository.insert(gross)
                if (inserted) call.respond(HttpStatusCode.Created, mapOf("status" to "Success"))
                else call.respond(HttpStatusCode.Conflict, mapOf("status" to "Error"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("status" to "Error", "message" to (e.message ?: "")))
            }
        }
        put("/{month}") {
            val month = call.parameters["month"] ?: return@put call.respond(HttpStatusCode.BadRequest)
            try {
                val gross = call.receive<GrossData>()
                val updated = repository.update(month, gross)
                if (updated) call.respond(HttpStatusCode.OK, mapOf("status" to "Success"))
                else call.respond(HttpStatusCode.NotFound, mapOf("status" to "Error"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("status" to "Error"))
            }
        }
        delete("/{month}") {
            val month = call.parameters["month"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            if (repository.delete(month)) call.respond(HttpStatusCode.OK, mapOf("status" to "Success"))
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
                val response = sheetsApiClient.getValues(spreadsheetId, "gross", accessToken)
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
    }
}
