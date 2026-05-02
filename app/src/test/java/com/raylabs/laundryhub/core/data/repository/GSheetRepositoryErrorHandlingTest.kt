package com.raylabs.laundryhub.core.data.repository

import com.raylabs.laundryhub.core.data.service.GoogleSheetService
import org.junit.Assert.assertEquals
import org.junit.Test

class GSheetRepositoryErrorHandlingTest {

    @Test
    fun `handleException maps drive api disabled details`() {
        val result = GSheetRepositoryErrorHandling.handleException(
            Exception("Google Drive API has not been used in project 655099386324 before or it is disabled.")
        )

        assertEquals(
            GSheetRepositoryErrorHandling.DRIVE_API_NOT_ENABLED_MESSAGE,
            result.message
        )
    }

    @Test
    fun `handleException maps invalid credentials to reconnect message`() {
        val result = GSheetRepositoryErrorHandling.handleException(
            Exception("Request had invalid authentication credentials. Expected OAuth 2 access token, login cookie or other valid authentication credential.")
        )

        assertEquals(
            GSheetRepositoryErrorHandling.AUTHORIZATION_RECONNECT_REQUIRED_MESSAGE,
            result.message
        )
    }

    @Test
    fun `handleException returns generic error for other failures`() {
        val result = GSheetRepositoryErrorHandling.handleException(
            Exception("Spreadsheet exploded.")
        )

        assertEquals(
            "Spreadsheet exploded.",
            result.message
        )
    }

    @Test
    fun `handleReadSheetResponseException maps authorization configuration issue`() {
        val result = GSheetRepositoryErrorHandling.handleReadSheetResponseException(
            Exception("DEVELOPER_ERROR: Unknown calling package name com.google.android.gms")
        )

        assertEquals(
            GSheetRepositoryErrorHandling.AUTHORIZATION_CONFIGURATION_MESSAGE,
            result.message
        )
    }

    @Test
    fun `handleReadSheetResponseException maps invalid credential message to reconnect message`() {
        val result = GSheetRepositoryErrorHandling.handleReadSheetResponseException(
            Exception("Error 401: Unauthorized. Request had invalid authentication credentials.")
        )

        assertEquals(
            GSheetRepositoryErrorHandling.AUTHORIZATION_RECONNECT_REQUIRED_MESSAGE,
            result.message
        )
    }

    @Test
    fun `handleReadSheetResponseException falls back to exception message`() {
        val result = GSheetRepositoryErrorHandling.handleReadSheetResponseException(
            Exception("something else happened")
        )

        assertEquals("something else happened", result.message)
    }

    @Test
    fun `handleReadSheetResponseException falls back to unexpected error when message is null`() {
        val result = GSheetRepositoryErrorHandling.handleReadSheetResponseException(
            Exception()
        )

        assertEquals("Unexpected Error", result.message)
    }

    @Test
    fun `handleFailAfterRetry returns fixed message`() {
        assertEquals(
            "Failed after 3 attempts.",
            GSheetRepositoryErrorHandling.handleFailAfterRetry().message
        )
    }

    @Test
    fun `handleFailedAddOrder maps drive api disabled message to setup message`() {
        val result = GSheetRepositoryErrorHandling.handleFailedAddOrder(
            Exception("Drive API accessNotConfigured for drive.googleapis.com, enable it first.")
        )

        assertEquals(
            GSheetRepositoryErrorHandling.DRIVE_API_NOT_ENABLED_MESSAGE,
            result.message
        )
    }

    @Test
    fun `handleFailedUpdate maps auth configuration issue`() {
        val result = GSheetRepositoryErrorHandling.handleFailedUpdate(
            Exception("DEVELOPER_ERROR: Unknown calling package name com.google.android.gms")
        )

        assertEquals(
            GSheetRepositoryErrorHandling.AUTHORIZATION_CONFIGURATION_MESSAGE,
            result.message
        )
    }

    @Test
    fun `handleFailedUpdate maps missing access token to reconnect message`() {
        val result = GSheetRepositoryErrorHandling.handleFailedUpdate(
            Exception("access token is unavailable for owner@laundryhub.com")
        )

        assertEquals(
            GSheetRepositoryErrorHandling.AUTHORIZATION_RECONNECT_REQUIRED_MESSAGE,
            result.message
        )
    }

    @Test
    fun `handleFailedUpdate falls back to exception message`() {
        val result = GSheetRepositoryErrorHandling.handleFailedUpdate(
            Exception("failed to update row 99")
        )

        assertEquals("failed to update row 99", result.message)
    }

    @Test
    fun `handleFailedDelete falls back to exception message`() {
        val result = GSheetRepositoryErrorHandling.handleFailedDelete(Exception("failed to delete row"))
        assertEquals("failed to delete row", result.message)
    }

    @Test
    fun `handleReadSheetResponseException maps accessNotConfigured message to drive api not enabled`() {
        val result = GSheetRepositoryErrorHandling.handleReadSheetResponseException(
            Exception("accessNotConfigured for project")
        )
        assertEquals(GSheetRepositoryErrorHandling.DRIVE_API_NOT_ENABLED_MESSAGE, result.message)
    }

    @Test
    fun `handleFailedUpdate maps accessNotConfigured message to drive api not enabled`() {
        val result = GSheetRepositoryErrorHandling.handleFailedUpdate(
            Exception("accessNotConfigured for project")
        )
        assertEquals(GSheetRepositoryErrorHandling.DRIVE_API_NOT_ENABLED_MESSAGE, result.message)
    }

    @Test
    fun `handleIDNotFound returns fixed message`() {
        assertEquals("ID not found.", GSheetRepositoryErrorHandling.handleIDNotFound().message)
    }
}
