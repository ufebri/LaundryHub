package com.raylabs.laundryhub.core.data.repository

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.raylabs.laundryhub.ui.common.util.Resource

object GSheetRepositoryErrorHandling {

    // REPOSITORY
    fun handleGoogleJsonResponseException(e: GoogleJsonResponseException): Resource.Error {
        val statusCode = e.statusCode
        val statusMessage = e.statusMessage
        val details = e.details?.message ?: "Unknown Error"
        return Resource.Error("Error $statusCode: $statusMessage\nDetails: $details")
    }

    fun handleReadSheetResponseException(e: Exception): Resource.Error {
        return Resource.Error(e.message ?: "Unexpected Error")
    }

    fun handleFailAfterRetry(): Resource.Error =
        Resource.Error("Failed after 3 attempts.")

    fun handleFailedAddOrder(e: Exception): Resource.Error =
        Resource.Error(e.message ?: "Failed to add order.")

    fun handleFailedUpdate(e: Exception): Resource.Error =
        Resource.Error(e.message ?: "Failed to update order.")

    fun handleIDNotFound(): Resource.Error =
        Resource.Error("ID not found.")
}