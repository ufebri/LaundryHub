package com.raylabs.laundryhub.core.data.repository

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.raylabs.laundryhub.core.data.service.GoogleSheetService
import com.raylabs.laundryhub.ui.common.util.Resource

object GSheetRepositoryErrorHandling {

    const val EDIT_ACCESS_REQUIRED_MESSAGE =
        "This Google account can view the spreadsheet but cannot edit it. Ask the owner to grant Editor access before continuing."
    const val AUTHORIZATION_CONFIGURATION_MESSAGE =
        "Google Sheets authorization couldn't be completed on this device. Reconnect Google Sheets access and try again."
    const val DRIVE_API_NOT_ENABLED_MESSAGE =
        "LaundryHub still can't verify spreadsheet access because Google Drive API isn't enabled for this project yet."

    // REPOSITORY
    fun handleGoogleJsonResponseException(e: GoogleJsonResponseException): Resource.Error {
        val statusCode = e.statusCode
        val statusMessage = e.statusMessage
        val details = e.details?.message ?: "Unknown Error"
        return when {
            details.contains("Google Drive API has not been used", ignoreCase = true) ||
                details.contains("accessNotConfigured", ignoreCase = true) ||
                details.contains("drive.googleapis.com", ignoreCase = true) &&
                details.contains("enable", ignoreCase = true) ->
                Resource.Error(DRIVE_API_NOT_ENABLED_MESSAGE)

            else -> Resource.Error("Error $statusCode: $statusMessage\nDetails: $details")
        }
    }

    fun handleReadSheetResponseException(e: Exception): Resource.Error {
        if (e is UserRecoverableAuthIOException) {
            return Resource.Error(GoogleSheetService.MISSING_ACCESS_MESSAGE)
        }
        if (isDriveApiDisabledMessage(e.message)) {
            return Resource.Error(DRIVE_API_NOT_ENABLED_MESSAGE)
        }
        if (
            e.message?.contains("access token is unavailable", ignoreCase = true) == true ||
            e.message?.contains("DEVELOPER_ERROR", ignoreCase = true) == true ||
            e.message?.contains("Unknown calling package name", ignoreCase = true) == true
        ) {
            return Resource.Error(AUTHORIZATION_CONFIGURATION_MESSAGE)
        }
        return Resource.Error(e.message ?: "Unexpected Error")
    }

    fun handleFailAfterRetry(): Resource.Error =
        Resource.Error("Failed after 3 attempts.")

    fun handleFailedAddOrder(e: Exception): Resource.Error =
        handleWriteException(e, fallbackMessage = "Failed to add order.")

    fun handleFailedUpdate(e: Exception): Resource.Error =
        handleWriteException(e, fallbackMessage = "Failed to update order.")

    fun handleFailedDelete(e: Exception): Resource.Error =
        handleWriteException(e, fallbackMessage = "Failed to delete data.")

    fun handleIDNotFound(): Resource.Error =
        Resource.Error("ID not found.")

    private fun handleWriteException(
        exception: Exception,
        fallbackMessage: String
    ): Resource.Error {
        if (exception is UserRecoverableAuthIOException) {
            return Resource.Error(GoogleSheetService.MISSING_ACCESS_MESSAGE)
        }

        if (exception is GoogleJsonResponseException) {
            val details = exception.details?.message.orEmpty()
            return if (
                exception.statusCode == 403 &&
                !details.contains("Google Drive API has not been used", ignoreCase = true) &&
                !details.contains("accessNotConfigured", ignoreCase = true)
            ) {
                Resource.Error(EDIT_ACCESS_REQUIRED_MESSAGE)
            } else {
                handleGoogleJsonResponseException(exception)
            }
        }

        if (isDriveApiDisabledMessage(exception.message)) {
            return Resource.Error(DRIVE_API_NOT_ENABLED_MESSAGE)
        }

        if (
            exception.message?.contains("access token is unavailable", ignoreCase = true) == true ||
            exception.message?.contains("DEVELOPER_ERROR", ignoreCase = true) == true ||
            exception.message?.contains("Unknown calling package name", ignoreCase = true) == true
        ) {
            return Resource.Error(AUTHORIZATION_CONFIGURATION_MESSAGE)
        }

        return Resource.Error(exception.message ?: fallbackMessage)
    }

    private fun isDriveApiDisabledMessage(message: String?): Boolean {
        val raw = message.orEmpty()
        return raw.contains("Google Drive API has not been used", ignoreCase = true) ||
            raw.contains("drive.googleapis.com", ignoreCase = true) && raw.contains("enable", ignoreCase = true) ||
            raw.contains("accessNotConfigured", ignoreCase = true)
    }
}
