package com.raylabs.laundryhub.shared.network.api

import com.raylabs.laundryhub.shared.network.model.sheets.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GoogleSheetsApiClientTest {

    @Test
    fun `BatchUpdateValuesRequest serializes required valueInputOption`() {
        val json = Json {
            ignoreUnknownKeys = true
        }.encodeToString(batchUpdateRequest())

        assertTrue(json.contains("\"valueInputOption\":\"USER_ENTERED\""))
    }

    @Test
    fun `batchUpdateValues sends valueInputOption in request body`() = runTest {
        var requestBody = ""
        val client = GoogleSheetsApiClient(
            httpClient = mockClient(
                status = HttpStatusCode.OK,
                content = """
                    {
                      "spreadsheetId": "sheet-id",
                      "totalUpdatedRows": 1,
                      "totalUpdatedCells": 12,
                      "responses": [
                        { "updatedRows": 1, "updatedCells": 12 }
                      ]
                    }
                """.trimIndent(),
                onBody = { requestBody = it }
            )
        )

        val response = client.batchUpdateValues(
            spreadsheetId = "sheet-id",
            request = batchUpdateRequest(),
            accessToken = "token"
        )

        assertEquals(1, response.totalUpdatedRows)
        assertTrue(requestBody.contains("\"valueInputOption\":\"USER_ENTERED\""))
    }

    @Test
    fun `batchUpdateValues exposes Google error body when request fails`() = runTest {
        val client = GoogleSheetsApiClient(
            httpClient = mockClient(
                status = HttpStatusCode.BadRequest,
                content = """
                    {
                      "error": {
                        "code": 400,
                        "message": "Invalid valueInputOption",
                        "status": "INVALID_ARGUMENT"
                      }
                    }
                """.trimIndent()
            )
        )

        val error = assertFailsWith<IllegalStateException> {
            client.batchUpdateValues(
                spreadsheetId = "sheet-id",
                request = batchUpdateRequest(),
                accessToken = "token"
            )
        }

        assertTrue(error.message.orEmpty().contains("Google Sheets API error 400"))
        assertTrue(error.message.orEmpty().contains("Invalid valueInputOption"))
    }

    @Test
    fun `getSpreadsheet returns JSON text`() = runTest {
        val client = GoogleSheetsApiClient(
            httpClient = mockClient(
                status = HttpStatusCode.OK,
                content = """{"spreadsheetId":"sheet-id"}"""
            )
        )
        val response = client.getSpreadsheet("sheet-id", "token")
        assertTrue(response.contains("\"spreadsheetId\":\"sheet-id\""))
    }

    @Test
    fun `getValues returns ValueRange`() = runTest {
        val client = GoogleSheetsApiClient(
            httpClient = mockClient(
                status = HttpStatusCode.OK,
                content = """{"range":"income!A2:B3","majorDimension":"ROWS","values":[["A","B"]]}"""
            )
        )
        val response = client.getValues("sheet-id", "income!A2:B3", "token")
        assertEquals("income!A2:B3", response.range)
        assertEquals(listOf(listOf("A", "B")), response.values)
    }

    @Test
    fun `updateValues returns UpdateValuesResponse`() = runTest {
        val client = GoogleSheetsApiClient(
            httpClient = mockClient(
                status = HttpStatusCode.OK,
                content = """{"spreadsheetId":"sheet-id","updatedRows":1}"""
            )
        )
        val response = client.updateValues("sheet-id", "income!A2", ValueRange(), "token")
        assertEquals(1, response.updatedRows)
    }

    @Test
    fun `appendValues returns AppendValuesResponse`() = runTest {
        val client = GoogleSheetsApiClient(
            httpClient = mockClient(
                status = HttpStatusCode.OK,
                content = """{"spreadsheetId":"sheet-id","updates":{"updatedRows":2}}"""
            )
        )
        val response = client.appendValues("sheet-id", "income", ValueRange(), "token")
        assertEquals(2, response.updates?.updatedRows)
    }

    @Test
    fun `clearValues returns ClearValuesResponse`() = runTest {
        val client = GoogleSheetsApiClient(
            httpClient = mockClient(
                status = HttpStatusCode.OK,
                content = """{"spreadsheetId":"sheet-id","clearedRange":"income!A2"}"""
            )
        )
        val response = client.clearValues("sheet-id", "income!A2", "token")
        assertEquals("income!A2", response.clearedRange)
    }

    @Test
    fun `batchClearValues returns BatchClearValuesResponse`() = runTest {
        val client = GoogleSheetsApiClient(
            httpClient = mockClient(
                status = HttpStatusCode.OK,
                content = """{"spreadsheetId":"sheet-id","clearedRanges":["income!A2"]}"""
            )
        )
        val response = client.batchClearValues("sheet-id", BatchClearValuesRequest(ranges = listOf("income!A2")), "token")
        assertEquals(listOf("income!A2"), response.clearedRanges)
    }

    @Test
    fun `batchUpdate returns BatchUpdateSpreadsheetResponse`() = runTest {
        val client = GoogleSheetsApiClient(
            httpClient = mockClient(
                status = HttpStatusCode.OK,
                content = """{"spreadsheetId":"sheet-id"}"""
            )
        )
        val response = client.batchUpdate("sheet-id", BatchUpdateSpreadsheetRequest(requests = emptyList()), "token")
        assertEquals("sheet-id", response.spreadsheetId)
    }

    private fun batchUpdateRequest() = BatchUpdateValuesRequest(
        valueInputOption = "USER_ENTERED",
        data = listOf(
            ValueRange(
                range = "income!A2:L2",
                majorDimension = "ROWS",
                values = listOf(listOf("1674", "29/05/2026"))
            )
        )
    )

    private fun mockClient(
        status: HttpStatusCode,
        content: String,
        onBody: (String) -> Unit = {}
    ) = HttpClient(MockEngine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        engine {
            addHandler { request ->
                val bodyText = (request.body as? TextContent)?.text.orEmpty()
                onBody(bodyText)
                respond(
                    content = content,
                    status = status,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        }
    }
}
