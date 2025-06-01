package com.raylabs.laundryhub.core.data.service

import android.content.Context
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.InputStream
import javax.inject.Inject

class GoogleSheetService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val APPLICATION_NAME = "LaundryHub App"
        private const val SCOPES = "https://www.googleapis.com/auth/spreadsheets"
    }

    private var service: Sheets

    init {
        val credentialsStream: InputStream = context.resources.openRawResource(
            context.resources.getIdentifier("serviceacc", "raw", context.packageName)
        )

        val credentials = GoogleCredential.fromStream(credentialsStream)
            .createScoped(listOf(SCOPES))

        service = Sheets.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credentials
        )
            .setApplicationName(APPLICATION_NAME)
            .build()
    }

    fun getSheetsService(): Sheets = service
}
