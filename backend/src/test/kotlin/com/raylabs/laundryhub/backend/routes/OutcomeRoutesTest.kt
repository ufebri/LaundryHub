package com.raylabs.laundryhub.backend.routes

import com.raylabs.laundryhub.backend.db.repository.OutcomeRepository
import com.raylabs.laundryhub.backend.db.repository.SyncDeleteEventRepository
import com.raylabs.laundryhub.backend.db.repository.SyncEntityType
import com.raylabs.laundryhub.backend.service.SheetsPushScheduler
import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.shared.network.api.GoogleSheetsApiClient
import com.raylabs.laundryhub.shared.network.model.sheets.ValueRange
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OutcomeRoutesTest {

    private val repository: OutcomeRepository = mock()
    private val sheetsApiClient: GoogleSheetsApiClient = mock()
    private val syncDeleteEventRepository: SyncDeleteEventRepository = mock()
    private val sheetsPushScheduler: SheetsPushScheduler = mock()

    private val sampleOutcome = OutcomeData(
        id = "1",
        date = "01/01/2025",
        purpose = "Supplies",
        price = "15000",
        remark = "Detergent",
        payment = "cash"
    )

    @Test
    fun `get last-id returns latest outcome id`() = testApplication {
        environment { config = MapApplicationConfig() }
        whenever(repository.getLatestId()).thenReturn("42")

        application {
            install(ContentNegotiation) { json() }
            routing {
                outcomeRoutes(repository, sheetsApiClient, false, syncDeleteEventRepository, sheetsPushScheduler)
            }
        }

        val response = client.get("/api/outcomes/last-id")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("42"))
    }

    @Test
    fun `get by id returns outcome when found`() = testApplication {
        environment { config = MapApplicationConfig() }
        whenever(repository.getById("1")).thenReturn(sampleOutcome)

        application {
            install(ContentNegotiation) { json() }
            routing {
                outcomeRoutes(repository, sheetsApiClient, false, syncDeleteEventRepository, sheetsPushScheduler)
            }
        }

        val response = client.get("/api/outcomes/1")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Supplies"))
    }

    @Test
    fun `get by id returns NotFound when outcome not found`() = testApplication {
        environment { config = MapApplicationConfig() }
        whenever(repository.getById("99")).thenReturn(null)

        application {
            install(ContentNegotiation) { json() }
            routing {
                outcomeRoutes(repository, sheetsApiClient, false, syncDeleteEventRepository, sheetsPushScheduler)
            }
        }

        val response = client.get("/api/outcomes/99")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `get outcomes with params returns list`() = testApplication {
        environment { config = MapApplicationConfig() }
        whenever(repository.getAll(any(), any(), any())).thenReturn(listOf(sampleOutcome))

        application {
            install(ContentNegotiation) { json() }
            routing {
                outcomeRoutes(repository, sheetsApiClient, false, syncDeleteEventRepository, sheetsPushScheduler)
            }
        }

        val response = client.get("/api/outcomes?page=2&size=10&searchQuery=Soap")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Supplies"))
    }

    @Test
    fun `post outcome creates successfully`() = testApplication {
        environment { config = MapApplicationConfig() }
        whenever(repository.insertWithNextId(any())).thenReturn(sampleOutcome)

        application {
            install(ContentNegotiation) { json() }
            routing {
                outcomeRoutes(repository, sheetsApiClient, false, syncDeleteEventRepository, sheetsPushScheduler)
            }
        }

        val response = client.post("/api/outcomes") {
            contentType(ContentType.Application.Json)
            setBody("""{"id":"","date":"01/01/2025","purpose":"Supplies","price":"15000","remark":"Detergent","payment":"cash"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("Outcome created"))
        verify(sheetsPushScheduler).requestPush("outcome created")
    }

    @Test
    fun `post outcome returns Conflict when insert fails`() = testApplication {
        environment { config = MapApplicationConfig() }
        whenever(repository.insertWithNextId(any())).thenReturn(null)

        application {
            install(ContentNegotiation) { json() }
            routing {
                outcomeRoutes(repository, sheetsApiClient, false, syncDeleteEventRepository, sheetsPushScheduler)
            }
        }

        val response = client.post("/api/outcomes") {
            contentType(ContentType.Application.Json)
            setBody("""{"id":"","date":"01/01/2025","purpose":"Supplies","price":"15000","remark":"Detergent","payment":"cash"}""")
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `post outcome returns BadRequest on invalid body`() = testApplication {
        environment { config = MapApplicationConfig() }

        application {
            install(ContentNegotiation) { json() }
            routing {
                outcomeRoutes(repository, sheetsApiClient, false, syncDeleteEventRepository, sheetsPushScheduler)
            }
        }

        val response = client.post("/api/outcomes") {
            contentType(ContentType.Application.Json)
            setBody("invalid-json")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `put outcome updates successfully`() = testApplication {
        environment { config = MapApplicationConfig() }
        whenever(repository.update(eq("1"), any())).thenReturn(true)

        application {
            install(ContentNegotiation) { json() }
            routing {
                outcomeRoutes(repository, sheetsApiClient, false, syncDeleteEventRepository, sheetsPushScheduler)
            }
        }

        val response = client.put("/api/outcomes/1") {
            contentType(ContentType.Application.Json)
            setBody("""{"id":"1","date":"01/01/2025","purpose":"Supplies","price":"15000","remark":"Detergent","payment":"cash"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Success"))
        verify(sheetsPushScheduler).requestPush("outcome updated")
    }

    @Test
    fun `put outcome returns NotFound when update fails`() = testApplication {
        environment { config = MapApplicationConfig() }
        whenever(repository.update(eq("99"), any())).thenReturn(false)

        application {
            install(ContentNegotiation) { json() }
            routing {
                outcomeRoutes(repository, sheetsApiClient, false, syncDeleteEventRepository, sheetsPushScheduler)
            }
        }

        val response = client.put("/api/outcomes/99") {
            contentType(ContentType.Application.Json)
            setBody("""{"id":"99","date":"01/01/2025","purpose":"Supplies","price":"15000","remark":"Detergent","payment":"cash"}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `put outcome returns BadRequest on invalid body`() = testApplication {
        environment { config = MapApplicationConfig() }

        application {
            install(ContentNegotiation) { json() }
            routing {
                outcomeRoutes(repository, sheetsApiClient, false, syncDeleteEventRepository, sheetsPushScheduler)
            }
        }

        val response = client.put("/api/outcomes/1") {
            contentType(ContentType.Application.Json)
            setBody("invalid")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `delete outcome deletes successfully`() = testApplication {
        environment { config = MapApplicationConfig() }
        whenever(repository.delete("1")).thenReturn(true)

        application {
            install(ContentNegotiation) { json() }
            routing {
                outcomeRoutes(repository, sheetsApiClient, false, syncDeleteEventRepository, sheetsPushScheduler)
            }
        }

        val response = client.delete("/api/outcomes/1")
        assertEquals(HttpStatusCode.OK, response.status)
        verify(syncDeleteEventRepository).record(eq(SyncEntityType.OUTCOME), eq("1"))
        verify(sheetsPushScheduler).requestPush("outcome deleted")
    }

    @Test
    fun `delete outcome returns NotFound when delete fails`() = testApplication {
        environment { config = MapApplicationConfig() }
        whenever(repository.delete("99")).thenReturn(false)

        application {
            install(ContentNegotiation) { json() }
            routing {
                outcomeRoutes(repository, sheetsApiClient, false, syncDeleteEventRepository, sheetsPushScheduler)
            }
        }

        val response = client.delete("/api/outcomes/99")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `migrate outcomes migrates successfully`() = testApplication {
        environment { config = MapApplicationConfig() }
        val fakeResponse = ValueRange(
            range = "outcome",
            majorDimension = "ROWS",
            values = listOf(
                listOf("id", "date", "purpose", "price", "remark", "payment"),
                listOf("1", "01/01/2025", "Supplies", "15000", "Detergent", "cash")
            )
        )
        whenever(sheetsApiClient.getValues(any(), any(), any())).thenReturn(fakeResponse)
        whenever(repository.insertAll(any())).thenReturn(1)

        application {
            install(ContentNegotiation) { json() }
            routing {
                outcomeRoutes(repository, sheetsApiClient, true, syncDeleteEventRepository, sheetsPushScheduler)
            }
        }

        val response = client.post("/api/outcomes/migrate?spreadsheetId=sid&accessToken=tok")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("migrated"))
    }

    @Test
    fun `migrate outcomes returns BadRequest when params missing`() = testApplication {
        environment { config = MapApplicationConfig() }

        application {
            install(ContentNegotiation) { json() }
            routing {
                outcomeRoutes(repository, sheetsApiClient, true, syncDeleteEventRepository, sheetsPushScheduler)
            }
        }

        val response = client.post("/api/outcomes/migrate?spreadsheetId=sid")
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `migrate outcomes returns InternalServerError on exception`() = testApplication {
        environment { config = MapApplicationConfig() }
        whenever(sheetsApiClient.getValues(any(), any(), any())).thenThrow(RuntimeException("API error"))

        application {
            install(ContentNegotiation) { json() }
            routing {
                outcomeRoutes(repository, sheetsApiClient, true, syncDeleteEventRepository, sheetsPushScheduler)
            }
        }

        val response = client.post("/api/outcomes/migrate?spreadsheetId=sid&accessToken=tok")
        assertEquals(HttpStatusCode.InternalServerError, response.status)
    }
}
