package com.raylabs.laundryhub.backend.service

import com.raylabs.laundryhub.shared.network.HttpClientProvider
import com.raylabs.laundryhub.shared.network.api.GoogleSheetsApiClient
import com.raylabs.laundryhub.shared.network.model.sheets.ValueRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SheetsSyncService {

    // Ideally, this should be injected via DI (like Koin) in a real backend,
    // but for this sprint we instantiate it directly to keep things simple.
    private val httpClient = HttpClientProvider.createClient(enableLogging = true)
    private val sheetsApiClient = GoogleSheetsApiClient(httpClient)

    /**
     * POC: Sync mock data to a Google Sheet.
     * Note: A real access token is required to execute this successfully.
     */
    suspend fun syncDataToSheet(spreadsheetId: String, range: String, accessToken: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Simulated data to append/update
            val mockValues = listOf(
                listOf("Backend-Sync-1", "Test User A", "10000", "2024-06-01"),
                listOf("Backend-Sync-2", "Test User B", "25000", "2024-06-02")
            )
            
            val valueRange = ValueRange(
                range = range,
                majorDimension = "ROWS",
                values = mockValues
            )

            // Attempt to append values to the sheet
            val response = sheetsApiClient.appendValues(
                spreadsheetId = spreadsheetId,
                range = range,
                valueRange = valueRange,
                accessToken = accessToken
            )

            // If response has updates, we consider it a success
            response.updates != null
        } catch (e: Exception) {
            println("Error syncing to Google Sheets: \${e.message}")
            e.printStackTrace()
            false
        }
    }
}
