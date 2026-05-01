package com.raylabs.laundryhub.backend.routes

import com.raylabs.laundryhub.backend.db.repository.OutcomeRepository
import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
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

fun Route.outcomeRoutes(
    repository: OutcomeRepository,
    sheetsApiClient: GoogleSheetsApiClient
) {
    route("/api/outcomes") {
        get {
            call.respond(HttpStatusCode.OK, repository.getAll())
        }
        post {
            try {
                val outcome = call.receive<OutcomeData>()
                val inserted = repository.insert(outcome)
                if (inserted) call.respond(HttpStatusCode.Created, mapOf("status" to "Success"))
                else call.respond(HttpStatusCode.Conflict, mapOf("status" to "Error"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("status" to "Error", "message" to (e.message ?: "")))
            }
        }
        put("/{id}") {
            val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
            try {
                val outcome = call.receive<OutcomeData>()
                val updated = repository.update(id, outcome)
                if (updated) call.respond(HttpStatusCode.OK, mapOf("status" to "Success"))
                else call.respond(HttpStatusCode.NotFound, mapOf("status" to "Error"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("status" to "Error"))
            }
        }
        delete("/{id}") {
            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            if (repository.delete(id)) call.respond(HttpStatusCode.OK, mapOf("status" to "Success"))
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
                val response = sheetsApiClient.getValues(spreadsheetId, "outcome", accessToken)
                val rows = response.values ?: emptyList()
                val outcomes = rows.drop(1).mapNotNull { row -> // Assuming row 0 is header
                    if (row.isEmpty()) return@mapNotNull null
                    OutcomeData(
                        id = row.getOrNull(0) ?: "",
                        date = row.getOrNull(1) ?: "",
                        purpose = row.getOrNull(2) ?: "",
                        price = row.getOrNull(3) ?: "",
                        remark = row.getOrNull(4) ?: "",
                        payment = row.getOrNull(5) ?: ""
                    )
                }
                val inserted = repository.insertAll(outcomes)
                call.respond(HttpStatusCode.OK, mapOf("migrated" to inserted))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, e.message ?: "Error")
            }
        }
    }
}
