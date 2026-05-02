package com.raylabs.laundryhub.core.data.repository

import com.raylabs.laundryhub.shared.util.Resource

object LaundryRepositoryErrorHandling {

    const val EDIT_ACCESS_REQUIRED_MESSAGE =
        "This Google account can view the spreadsheet but cannot edit it. Ask the owner to grant Editor access before continuing."
    const val AUTHORIZATION_RECONNECT_REQUIRED_MESSAGE =
        "Access expired or became invalid. Please login again."
    const val AUTHORIZATION_CONFIGURATION_MESSAGE =
        "Authorization couldn't be completed on this device. Please login again."
    const val DRIVE_API_NOT_ENABLED_MESSAGE =
        "Backend still can't verify spreadsheet access because Google Drive API isn't enabled for this project yet."

    // REPOSITORY
    fun handleException(e: Exception): Resource.Error {
        val details = e.message ?: "Unknown Error"
        return when {
            isInvalidCredentialFailure(details = details) ->
                Resource.Error(AUTHORIZATION_RECONNECT_REQUIRED_MESSAGE)

            else -> Resource.Error(details)
        }
    }

    fun handleReadSheetResponseException(e: Exception): Resource.Error {
        if (isInvalidCredentialFailure(details = e.message)) {
            return Resource.Error(AUTHORIZATION_RECONNECT_REQUIRED_MESSAGE)
        }
        return Resource.Error(e.message ?: "Unexpected Error")
    }

    fun handleFailAfterRetry(): Resource.Error =
        Resource.Error("Failed after 3 attempts.")

    fun handleFailedAddOrder(e: Exception): Resource.Error =
        handleReadSheetResponseException(e)

    fun handleFailedUpdate(e: Exception): Resource.Error =
        handleReadSheetResponseException(e)

    fun handleFailedDelete(e: Exception): Resource.Error =
        handleReadSheetResponseException(e)

    fun handleIDNotFound(): Resource.Error =
        Resource.Error("ID not found.")

    private fun isInvalidCredentialFailure(
        statusCode: Int? = null,
        statusMessage: String? = null,
        details: String?
    ): Boolean {
        val rawDetails = details.orEmpty()
        val rawStatus = statusMessage.orEmpty()
        return statusCode == 401 ||
            rawStatus.contains("Unauthorized", ignoreCase = true) ||
            rawDetails.contains("invalid authentication credentials", ignoreCase = true)
    }
}
