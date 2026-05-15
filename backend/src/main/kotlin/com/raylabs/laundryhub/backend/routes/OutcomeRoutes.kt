package com.raylabs.laundryhub.backend.routes

import com.raylabs.laundryhub.backend.db.repository.OutcomeRepository
import com.raylabs.laundryhub.backend.db.repository.SyncDeleteEventRepository
import com.raylabs.laundryhub.backend.db.repository.SyncEntityType
import com.raylabs.laundryhub.backend.service.SheetsPushScheduler
import com.raylabs.laundryhub.core.domain.model.sheets.CreateOutcomeResponse
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
    sheetsApiClient: GoogleSheetsApiClient,
    migrationRoutesEnabled: Boolean = false,
    syncDeleteEventRepository: SyncDeleteEventRepository? = null,
    sheetsPushScheduler: SheetsPushScheduler? = null
) {
    route("/api/outcomes") {
        get("/last-id") {
            call.respond(HttpStatusCode.OK, mapOf("lastId" to repository.getLatestId()))
        }

        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val outcome = repository.getById(id)
            if (outcome != null) {
                call.respond(HttpStatusCode.OK, outcome)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("status" to "Error"))
            }
        }

        get {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 50
            call.respond(HttpStatusCode.OK, repository.getAll(page, size))
        }
        post {
            try {
                val outcome = call.receive<OutcomeData>()
                val createdOutcome = repository.insertWithNextId(outcome)
                if (createdOutcome != null) {
                    sheetsPushScheduler?.requestPush("outcome created")
                    call.respond(
                        HttpStatusCode.Created,
                        CreateOutcomeResponse(
                            status = "Success",
                            message = "Outcome created",
                            outcomeId = createdOutcome.id
                        )
                    )
                } else {
                    call.respond(
                        HttpStatusCode.Conflict,
                        mapOf("status" to "Error", "message" to "Outcome id allocation failed")
                    )
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("status" to "Error", "message" to (e.message ?: "")))
            }
        }
        put("/{id}") {
            val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
            try {
                val outcome = call.receive<OutcomeData>()
                val updated = repository.update(id, outcome)
                if (updated) {
                    sheetsPushScheduler?.requestPush("outcome updated")
                    call.respond(HttpStatusCode.OK, mapOf("status" to "Success"))
                }
                else call.respond(HttpStatusCode.NotFound, mapOf("status" to "Error"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("status" to "Error"))
            }
        }
        delete("/{id}") {
            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            if (repository.delete(id)) {
                syncDeleteEventRepository?.record(SyncEntityType.OUTCOME, id)
                sheetsPushScheduler?.requestPush("outcome deleted")
                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "status" to "Success",
                        "message" to "Outcome deleted",
                        "sheetSynced" to "queued"
                    )
                )
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("status" to "Error"))
            }
        }
        
        if (migrationRoutesEnabled) {
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
}
