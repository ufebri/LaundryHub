package com.raylabs.laundryhub.core.data.service

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
}
