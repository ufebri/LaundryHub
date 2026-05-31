package com.raylabs.laundryhub.backend.routes

import com.raylabs.laundryhub.backend.db.repository.GrossRepository
import com.raylabs.laundryhub.backend.service.SheetsSyncService
import com.raylabs.laundryhub.core.domain.model.sheets.GrossData
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
import kotlinx.coroutines.test.runTest
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GrossRouteBehaviorTest {

    private val repository: GrossRepository = mock()
    private val syncService: SheetsSyncService = mock()
    private val sheetsApiClient: GoogleSheetsApiClient = mock()

    @Test
    fun `gross response uses Sheet data when available`() = runTest {
        whenever(syncService.fetchGrossFromSheet(any())).doReturn(
            listOf(GrossData(month = "Mei 2026", totalNominal = "Rp3.343.000", orderCount = "115 order", tax = "Rp16.715"))
        )

        val rows = fetchGrossForResponse(repository, syncService, spreadsheetId = "sheet-id", page = 1, size = 1)

        assertEquals("Rp3.343.000", rows.single().totalNominal)
        verify(repository, never()).getAll(any(), any())
    }

    @Test
    fun `gross response sorts Sheet rows newest first and applies paging`() = runTest {
        whenever(syncService.fetchGrossFromSheet(any())).doReturn(
            listOf(
                GrossData(month = "Maret 2025", totalNominal = "Rp1.038.150", orderCount = "35", tax = "Rp5.191"),
                GrossData(month = "April 2026", totalNominal = "Rp4.101.000", orderCount = "148", tax = "Rp20.505"),
                GrossData(month = "Mei 2026", totalNominal = "Rp3.343.000", orderCount = "115", tax = "Rp16.715")
            )
        )

        val firstPage = fetchGrossForResponse(repository, syncService, spreadsheetId = "sheet-id", page = 1, size = 2)
        val secondPage = fetchGrossForResponse(repository, syncService, spreadsheetId = "sheet-id", page = 2, size = 2)

        assertEquals(listOf("Mei 2026", "April 2026"), firstPage.map { it.month })
        assertEquals(listOf("Maret 2025"), secondPage.map { it.month })
        verify(repository, never()).getAll(any(), any())
    }

    @Test
    fun `gross response falls back to database cache when Sheet data is unavailable`() = runTest {
        whenever(syncService.fetchGrossFromSheet(any())).doReturn(emptyList())
        whenever(repository.getAll(any(), any())).doReturn(
            listOf(GrossData(month = "Mei 2026", totalNominal = "Rp3.139.000", orderCount = "108 order", tax = "Rp15.695"))
        )

        val rows = fetchGrossForResponse(repository, syncService, spreadsheetId = "sheet-id", page = 1, size = 1)

        assertEquals("Rp3.139.000", rows.single().totalNominal)
    }

    @Test
    fun `gross response falls back to database cache when spreadsheet id is missing`() = runTest {
        whenever(repository.getAll(any(), any())).doReturn(
            listOf(GrossData(month = "Mei 2026", totalNominal = "Rp3.139.000", orderCount = "108 order", tax = "Rp15.695"))
        )

        val rows = fetchGrossForResponse(repository, syncService, spreadsheetId = null, page = 1, size = 1)

        assertEquals("108 order", rows.single().orderCount)
        verify(syncService, never()).fetchGrossFromSheet(any())
    }

    @Test
    fun `get gross endpoint returns ok`() = testApplication {
        environment {
            config = MapApplicationConfig()
        }
        whenever(syncService.fetchGrossFromSheet(any())).thenReturn(emptyList())
        whenever(repository.getAll(any(), any())).thenReturn(
            listOf(GrossData(month = "Mei 2026", totalNominal = "Rp3.139.000", orderCount = "108 order", tax = "Rp15.695"))
        )

        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                grossRoutes(
                    repository = repository,
                    sheetsApiClient = sheetsApiClient,
                    syncService = syncService,
                    spreadsheetId = "sheet-id",
                    migrationRoutesEnabled = false
                )
            }
        }

        val response = client.get("/api/gross?page=1&size=10")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Mei 2026"))
    }

    @Test
    fun `post gross inserts successfully`() = testApplication {
        environment {
            config = MapApplicationConfig()
        }
        whenever(repository.insert(any())).thenReturn(true)

        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                grossRoutes(
                    repository = repository,
                    sheetsApiClient = sheetsApiClient,
                    syncService = syncService,
                    spreadsheetId = "sheet-id",
                    migrationRoutesEnabled = false
                )
            }
        }

        val response = client.post("/api/gross") {
            contentType(ContentType.Application.Json)
            setBody("""{"month":"Mei 2026","totalNominal":"Rp3.139.000","orderCount":"108","tax":"Rp15.695"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("Success"))
    }

    @Test
    fun `put gross updates successfully`() = testApplication {
        environment {
            config = MapApplicationConfig()
        }
        whenever(repository.update(eq("Mei 2026"), any())).thenReturn(true)

        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                grossRoutes(
                    repository = repository,
                    sheetsApiClient = sheetsApiClient,
                    syncService = syncService,
                    spreadsheetId = "sheet-id",
                    migrationRoutesEnabled = false
                )
            }
        }

        val response = client.put("/api/gross/Mei 2026") {
            contentType(ContentType.Application.Json)
            setBody("""{"month":"Mei 2026","totalNominal":"Rp3.139.000","orderCount":"108","tax":"Rp15.695"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Success"))
    }

    @Test
    fun `delete gross deletes successfully`() = testApplication {
        environment {
            config = MapApplicationConfig()
        }
        whenever(repository.delete("Mei 2026")).thenReturn(true)

        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                grossRoutes(
                    repository = repository,
                    sheetsApiClient = sheetsApiClient,
                    syncService = syncService,
                    spreadsheetId = "sheet-id",
                    migrationRoutesEnabled = false
                )
            }
        }

        val response = client.delete("/api/gross/Mei 2026")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Success"))
    }

    @Test
    fun `post gross returns Conflict when insertion fails`() = testApplication {
        environment {
            config = MapApplicationConfig()
        }
        whenever(repository.insert(any())).thenReturn(false)

        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                grossRoutes(
                    repository = repository,
                    sheetsApiClient = sheetsApiClient,
                    syncService = syncService,
                    spreadsheetId = "sheet-id",
                    migrationRoutesEnabled = false
                )
            }
        }

        val response = client.post("/api/gross") {
            contentType(ContentType.Application.Json)
            setBody("""{"month":"Mei 2026","totalNominal":"Rp3.139.000","orderCount":"108","tax":"Rp15.695"}""")
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertTrue(response.bodyAsText().contains("Error"))
    }

    @Test
    fun `post gross returns BadRequest on invalid body`() = testApplication {
        environment {
            config = MapApplicationConfig()
        }

        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                grossRoutes(
                    repository = repository,
                    sheetsApiClient = sheetsApiClient,
                    syncService = syncService,
                    spreadsheetId = "sheet-id",
                    migrationRoutesEnabled = false
                )
            }
        }

        val response = client.post("/api/gross") {
            contentType(ContentType.Application.Json)
            setBody("invalid-json")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("Error"))
    }

    @Test
    fun `put gross returns NotFound when update fails`() = testApplication {
        environment {
            config = MapApplicationConfig()
        }
        whenever(repository.update(eq("Mei 2026"), any())).thenReturn(false)

        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                grossRoutes(
                    repository = repository,
                    sheetsApiClient = sheetsApiClient,
                    syncService = syncService,
                    spreadsheetId = "sheet-id",
                    migrationRoutesEnabled = false
                )
            }
        }

        val response = client.put("/api/gross/Mei 2026") {
            contentType(ContentType.Application.Json)
            setBody("""{"month":"Mei 2026","totalNominal":"Rp3.139.000","orderCount":"108","tax":"Rp15.695"}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Error"))
    }

    @Test
    fun `put gross returns BadRequest on invalid body`() = testApplication {
        environment {
            config = MapApplicationConfig()
        }

        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                grossRoutes(
                    repository = repository,
                    sheetsApiClient = sheetsApiClient,
                    syncService = syncService,
                    spreadsheetId = "sheet-id",
                    migrationRoutesEnabled = false
                )
            }
        }

        val response = client.put("/api/gross/Mei 2026") {
            contentType(ContentType.Application.Json)
            setBody("invalid-json")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("Error"))
    }

    @Test
    fun `delete gross returns NotFound when delete fails`() = testApplication {
        environment {
            config = MapApplicationConfig()
        }
        whenever(repository.delete("Mei 2026")).thenReturn(false)

        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                grossRoutes(
                    repository = repository,
                    sheetsApiClient = sheetsApiClient,
                    syncService = syncService,
                    spreadsheetId = "sheet-id",
                    migrationRoutesEnabled = false
                )
            }
        }

        val response = client.delete("/api/gross/Mei 2026")
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Error"))
    }

    @Test
    fun `migrate gross returns BadRequest when spreadsheetId is missing`() = testApplication {
        environment {
            config = MapApplicationConfig()
        }

        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                grossRoutes(
                    repository = repository,
                    sheetsApiClient = sheetsApiClient,
                    syncService = syncService,
                    spreadsheetId = "sheet-id",
                    migrationRoutesEnabled = true
                )
            }
        }

        val response = client.post("/api/gross/migrate?accessToken=token")
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `migrate gross returns InternalServerError when api client fails`() = testApplication {
        environment {
            config = MapApplicationConfig()
        }
        whenever(sheetsApiClient.getValues(any(), any(), any())).thenThrow(RuntimeException("API Error"))

        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                grossRoutes(
                    repository = repository,
                    sheetsApiClient = sheetsApiClient,
                    syncService = syncService,
                    spreadsheetId = "sheet-id",
                    migrationRoutesEnabled = true
                )
            }
        }

        val response = client.post("/api/gross/migrate?spreadsheetId=sid&accessToken=token")
        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertEquals("API Error", response.bodyAsText())
    }
}
