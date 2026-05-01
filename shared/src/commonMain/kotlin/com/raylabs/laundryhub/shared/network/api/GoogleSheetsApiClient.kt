package com.raylabs.laundryhub.shared.network.api

import com.raylabs.laundryhub.shared.network.model.sheets.AppendValuesResponse
import com.raylabs.laundryhub.shared.network.model.sheets.BatchUpdateSpreadsheetRequest
import com.raylabs.laundryhub.shared.network.model.sheets.BatchUpdateSpreadsheetResponse
import com.raylabs.laundryhub.shared.network.model.sheets.ClearValuesResponse
import com.raylabs.laundryhub.shared.network.model.sheets.UpdateValuesResponse
import com.raylabs.laundryhub.shared.network.model.sheets.ValueRange
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class GoogleSheetsApiClient(
    private val httpClient: HttpClient
) {
    private val baseUrl = "https://sheets.googleapis.com/v4/spreadsheets"

    suspend fun getValues(
        spreadsheetId: String,
        range: String,
        accessToken: String
    ): ValueRange {
        return httpClient.get("$baseUrl/$spreadsheetId/values/$range") {
            bearerAuth(accessToken)
        }.body()
    }

    suspend fun updateValues(
        spreadsheetId: String,
        range: String,
        valueRange: ValueRange,
        accessToken: String
    ): UpdateValuesResponse {
        return httpClient.put("$baseUrl/$spreadsheetId/values/$range?valueInputOption=USER_ENTERED") {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(valueRange)
        }.body()
    }

    suspend fun appendValues(
        spreadsheetId: String,
        range: String,
        valueRange: ValueRange,
        accessToken: String
    ): AppendValuesResponse {
        return httpClient.post("$baseUrl/$spreadsheetId/values/$range:append?valueInputOption=USER_ENTERED") {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(valueRange)
        }.body()
    }

    suspend fun clearValues(
        spreadsheetId: String,
        range: String,
        accessToken: String
    ): ClearValuesResponse {
        return httpClient.post("$baseUrl/$spreadsheetId/values/$range:clear") {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(mapOf<String, String>())
        }.body()
    }

    suspend fun batchUpdate(
        spreadsheetId: String,
        request: BatchUpdateSpreadsheetRequest,
        accessToken: String
    ): BatchUpdateSpreadsheetResponse {
        return httpClient.post("$baseUrl/$spreadsheetId:batchUpdate") {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}
