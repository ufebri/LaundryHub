package com.raylabs.laundryhub.backend.routes

import com.raylabs.laundryhub.backend.db.repository.SummaryRepository
import com.raylabs.laundryhub.backend.service.SheetsSyncService
import com.raylabs.laundryhub.core.domain.model.sheets.SpreadsheetData
import com.raylabs.laundryhub.shared.network.api.GoogleSheetsApiClient
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
import org.mockito.kotlin.whenever
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SummaryRoutesTest {

    private val repository: SummaryRepository = mock()
    private val sheetsApiClient: GoogleSheetsApiClient = mock()
    private val syncService: SheetsSyncService = mock()

    @Test
    fun `get summary returns ok and repository items when sheets data is empty`() = testApplication {
        environment {
            config = MapApplicationConfig()
        }
        whenever(syncService.fetchSummaryFromSheet(any())).thenReturn(emptyList())
        whenever(repository.getAll()).thenReturn(listOf(SpreadsheetData(key = "income", value = "500000")))

        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                summaryRoutes(
                    repository = repository,
                    sheetsApiClient = sheetsApiClient,
                    syncService = syncService,
                    spreadsheetId = "sheet-id",
                    migrationRoutesEnabled = false
                )
            }
        }

        val response = client.get("/api/summary")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("income"))
    }

    @Test
    fun `post summary inserts data successfully`() = testApplication {
        environment {
            config = MapApplicationConfig()
        }
        whenever(repository.insert(any())).thenReturn(true)

        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                summaryRoutes(
                    repository = repository,
                    sheetsApiClient = sheetsApiClient,
                    syncService = syncService,
                    spreadsheetId = "sheet-id",
                    migrationRoutesEnabled = false
                )
            }
        }

        val response = client.post("/api/summary") {
            contentType(ContentType.Application.Json)
            setBody("""{"key":"income","value":"500000"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("Success"))
    }

    @Test
    fun `put summary updates key successfully`() = testApplication {
        environment {
            config = MapApplicationConfig()
        }
        whenever(repository.update(eq("income"), any())).thenReturn(true)

        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                summaryRoutes(
                    repository = repository,
                    sheetsApiClient = sheetsApiClient,
                    syncService = syncService,
                    spreadsheetId = "sheet-id",
                    migrationRoutesEnabled = false
                )
            }
        }

        val response = client.put("/api/summary/income") {
            contentType(ContentType.Application.Json)
            setBody("""{"key":"income","value":"600000"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Success"))
    }

    @Test
    fun `delete summary deletes successfully`() = testApplication {
        environment {
            config = MapApplicationConfig()
        }
        whenever(repository.delete("income")).thenReturn(true)

        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                summaryRoutes(
                    repository = repository,
                    sheetsApiClient = sheetsApiClient,
                    syncService = syncService,
                    spreadsheetId = "sheet-id",
                    migrationRoutesEnabled = false
                )
            }
        }

        val response = client.delete("/api/summary/income")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("sheetSynced"))
    }

    @Test
    fun `post summary returns InternalServerError when repository insert fails`() = testApplication {
        environment { config = MapApplicationConfig() }
        whenever(repository.insert(any())).thenReturn(false)

        application {
            install(ContentNegotiation) { json() }
            routing {
                summaryRoutes(
                    repository = repository,
                    sheetsApiClient = sheetsApiClient,
                    syncService = syncService,
                    spreadsheetId = "sheet-id",
                    migrationRoutesEnabled = false
                )
            }
        }

        val response = client.post("/api/summary") {
            contentType(ContentType.Application.Json)
            setBody("""{"key":"income","value":"500000"}""")
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertTrue(response.bodyAsText().contains("Error"))
    }

    @Test
    fun `put summary returns NotFound when repository update fails`() = testApplication {
        environment { config = MapApplicationConfig() }
        whenever(repository.update(eq("income"), any())).thenReturn(false)

        application {
            install(ContentNegotiation) { json() }
            routing {
                summaryRoutes(
                    repository = repository,
                    sheetsApiClient = sheetsApiClient,
                    syncService = syncService,
                    spreadsheetId = "sheet-id",
                    migrationRoutesEnabled = false
                )
            }
        }

        val response = client.put("/api/summary/income") {
            contentType(ContentType.Application.Json)
            setBody("""{"key":"income","value":"600000"}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Error"))
    }

    @Test
    fun `delete summary returns NotFound when repository delete fails`() = testApplication {
        environment { config = MapApplicationConfig() }
        whenever(repository.delete("income")).thenReturn(false)

        application {
            install(ContentNegotiation) { json() }
            routing {
                summaryRoutes(
                    repository = repository,
                    sheetsApiClient = sheetsApiClient,
                    syncService = syncService,
                    spreadsheetId = "sheet-id",
                    migrationRoutesEnabled = false
                )
            }
        }

        val response = client.delete("/api/summary/income")
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Error"))
    }

    @Test
    fun `migrate summary returns BadRequest when params are missing`() = testApplication {
        environment { config = MapApplicationConfig() }
        application {
            install(ContentNegotiation) { json() }
            routing {
                summaryRoutes(
                    repository = repository,
                    sheetsApiClient = sheetsApiClient,
                    syncService = syncService,
                    spreadsheetId = "sheet-id",
                    migrationRoutesEnabled = true
                )
            }
        }

        val response = client.post("/api/summary/migrate")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("Missing params"))
    }

    @Test
    fun `migrate summary migrates successfully when spreadsheet data is present`() = testApplication {
        environment { config = MapApplicationConfig() }
        val valueRange = com.raylabs.laundryhub.shared.network.model.sheets.ValueRange(
            values = listOf(
                listOf("Key", "Value"),
                listOf("income", "500000"),
                emptyList()
            )
        )
        whenever(sheetsApiClient.getValues("mig-sheet", "summary", "mig-token")).thenReturn(valueRange)
        whenever(repository.insertAll(any())).thenReturn(1)

        application {
            install(ContentNegotiation) { json() }
            routing {
                summaryRoutes(
                    repository = repository,
                    sheetsApiClient = sheetsApiClient,
                    syncService = syncService,
                    spreadsheetId = "sheet-id",
                    migrationRoutesEnabled = true
                )
            }
        }

        val response = client.post("/api/summary/migrate?spreadsheetId=mig-sheet&accessToken=mig-token")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("migrated"))
    }

    @Test
    fun `migrate summary returns InternalServerError when sheetsApiClient throws exception`() = testApplication {
        environment { config = MapApplicationConfig() }
        whenever(sheetsApiClient.getValues(any(), any(), any())).thenThrow(RuntimeException("API failure"))

        application {
            install(ContentNegotiation) { json() }
            routing {
                summaryRoutes(
                    repository = repository,
                    sheetsApiClient = sheetsApiClient,
                    syncService = syncService,
                    spreadsheetId = "sheet-id",
                    migrationRoutesEnabled = true
                )
            }
        }

        val response = client.post("/api/summary/migrate?spreadsheetId=mig-sheet&accessToken=mig-token")
        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertTrue(response.bodyAsText().contains("API failure"))
    }
}
