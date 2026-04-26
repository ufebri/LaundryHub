package com.raylabs.laundryhub.core.data.service

import android.net.Uri
import android.util.Log
import androidx.annotation.Keep
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.GenericUrl
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class GoogleSheetService @Inject constructor(
    private val googleSheetsAuthorizationManager: GoogleSheetsAuthorizationManager
) {

    companion object {
        private const val APPLICATION_NAME = "LaundryHub App"
        private const val TAG = "GoogleSheetService"
        const val DRIVE_METADATA_READONLY_SCOPE =
            "https://www.googleapis.com/auth/drive.metadata.readonly"
        const val MISSING_ACCESS_MESSAGE =
            "Google Sheets access is not connected yet. Grant spreadsheet access from your Google account first."
    }

    fun hasAuthorizedSheetsAccount(): Boolean =
        !googleSheetsAuthorizationManager.getSignedInEmail().isNullOrBlank()

    fun getAuthorizedAccountEmail(): String? = googleSheetsAuthorizationManager.getSignedInEmail()

    fun getSheetsService(): Sheets {
        val accountEmail = googleSheetsAuthorizationManager.getSignedInEmail()
            ?: throw IllegalStateException(MISSING_ACCESS_MESSAGE)
        val accessToken = requireAccessToken(accountEmail)
        Log.d(TAG, "Building Sheets service for email=$accountEmail accessTokenPresent=true")

        return Sheets.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            authorizedRequestInitializer(accessToken)
        )
            .setApplicationName(APPLICATION_NAME)
            .build()
    }

    fun hasSpreadsheetEditAccess(spreadsheetId: String): Boolean {
        val accountEmail = googleSheetsAuthorizationManager.getSignedInEmail()
            ?: throw IllegalStateException(MISSING_ACCESS_MESSAGE)
        val accessToken = requireAccessToken(accountEmail)
        Log.d(TAG, "Checking spreadsheet edit access for email=$accountEmail spreadsheetId=$spreadsheetId")
        val requestFactory = GoogleNetHttpTransport.newTrustedTransport()
            .createRequestFactory(authorizedRequestInitializer(accessToken))
        val url = GenericUrl(
            Uri.parse("https://www.googleapis.com/drive/v3/files/$spreadsheetId")
                .buildUpon()
                .appendQueryParameter("fields", "capabilities(canEdit,canModifyContent)")
                .appendQueryParameter("supportsAllDrives", "true")
                .build()
                .toString()
        )
        val response = requestFactory.buildGetRequest(url).execute()

        return try {
            val payload = Gson().fromJson(
                response.parseAsString(),
                DriveFileMetadataResponse::class.java
            )
            Log.d(
                TAG,
                "Spreadsheet capability result spreadsheetId=$spreadsheetId canEdit=${payload.capabilities?.canEdit} canModifyContent=${payload.capabilities?.canModifyContent}"
            )
            payload.capabilities?.canModifyContent ?: payload.capabilities?.canEdit ?: false
        } finally {
            response.disconnect()
        }
    }

    private fun requireAccessToken(accountEmail: String): String {
        return runBlocking {
            googleSheetsAuthorizationManager.getAccessToken()
        }?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException(
                "Google Sheets access token is unavailable for $accountEmail. Reconnect Google Sheets access and try again."
            )
    }

    private fun authorizedRequestInitializer(accessToken: String): HttpRequestInitializer =
        HttpRequestInitializer { request ->
            request.headers.authorization = "Bearer $accessToken"
        }

    @Keep
    private data class DriveFileMetadataResponse(
        @SerializedName("capabilities")
        val capabilities: DriveFileCapabilities? = null
    )

    @Keep
    private data class DriveFileCapabilities(
        @SerializedName("canEdit")
        val canEdit: Boolean? = null,
        @SerializedName("canModifyContent")
        val canModifyContent: Boolean? = null
    )
}
