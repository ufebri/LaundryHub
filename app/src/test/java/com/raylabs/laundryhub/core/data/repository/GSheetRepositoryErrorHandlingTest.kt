package com.raylabs.laundryhub.core.data.repository

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.googleapis.json.GoogleJsonError
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.raylabs.laundryhub.core.data.service.GoogleSheetService
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GSheetRepositoryErrorHandlingTest {

    @Test
    fun `handleGoogleJsonResponseException maps drive api disabled details`() {
        val result = GSheetRepositoryErrorHandling.handleGoogleJsonResponseException(
            googleJsonException(
                statusCode = 403,
                statusMessage = "Forbidden",
                detailsMessage = "Google Drive API has not been used in project 655099386324 before or it is disabled."
            )
        )

        assertEquals(
            GSheetRepositoryErrorHandling.DRIVE_API_NOT_ENABLED_MESSAGE,
            result.message
        )
    }

    @Test
    fun `handleGoogleJsonResponseException maps invalid credentials to reconnect message`() {
        val result = GSheetRepositoryErrorHandling.handleGoogleJsonResponseException(
            googleJsonException(
                statusCode = 401,
                statusMessage = "Unauthorized",
                detailsMessage = "Request had invalid authentication credentials. Expected OAuth 2 access token, login cookie or other valid authentication credential."
            )
        )

        assertEquals(
            GSheetRepositoryErrorHandling.AUTHORIZATION_RECONNECT_REQUIRED_MESSAGE,
            result.message
        )
    }

    @Test
    fun `handleGoogleJsonResponseException returns generic error for non drive failures`() {
        val result = GSheetRepositoryErrorHandling.handleGoogleJsonResponseException(
            googleJsonException(
                statusCode = 500,
                statusMessage = "Internal Server Error",
                detailsMessage = "Spreadsheet exploded."
            )
        )

        assertEquals(
            "Error 500: Internal Server Error\nDetails: Spreadsheet exploded.",
            result.message
        )
    }

    @Test
    fun `handleReadSheetResponseException maps user recoverable auth error`() {
        val result = GSheetRepositoryErrorHandling.handleReadSheetResponseException(
            mock<UserRecoverableAuthIOException>()
        )

        assertEquals(GoogleSheetService.MISSING_ACCESS_MESSAGE, result.message)
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
    fun `handleFailedAddOrder maps google 403 write denial to edit access required`() {
        val result = GSheetRepositoryErrorHandling.handleFailedAddOrder(
            googleJsonException(
                statusCode = 403,
                statusMessage = "Forbidden",
                detailsMessage = "The caller does not have permission"
            )
        )

        assertEquals(
            GSheetRepositoryErrorHandling.EDIT_ACCESS_REQUIRED_MESSAGE,
            result.message
        )
    }

    @Test
    fun `handleFailedAddOrder maps user recoverable auth error`() {
        val result = GSheetRepositoryErrorHandling.handleFailedAddOrder(
            mock<UserRecoverableAuthIOException>()
        )

        assertEquals(GoogleSheetService.MISSING_ACCESS_MESSAGE, result.message)
    }

    @Test
    fun `handleFailedAddOrder maps drive api disabled 403 to setup message`() {
        val result = GSheetRepositoryErrorHandling.handleFailedAddOrder(
            googleJsonException(
                statusCode = 403,
                statusMessage = "Forbidden",
                detailsMessage = "Drive API accessNotConfigured for drive.googleapis.com, enable it first."
            )
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
    fun `handleFailedUpdate uses fallback message when exception message is null`() {
        val result = GSheetRepositoryErrorHandling.handleFailedUpdate(
            Exception()
        )

        assertEquals("Failed to update order.", result.message)
    }

    @Test
    fun `handleFailedUpdate falls back to exception message`() {
        val result = GSheetRepositoryErrorHandling.handleFailedUpdate(
            Exception("failed to update row 99")
        )

        assertEquals("failed to update row 99", result.message)
    }

    @Test
    fun `handleIDNotFound returns fixed message`() {
        assertEquals("ID not found.", GSheetRepositoryErrorHandling.handleIDNotFound().message)
    }

    private fun googleJsonException(
        statusCode: Int,
        statusMessage: String,
        detailsMessage: String
    ): GoogleJsonResponseException {
        val error = GoogleJsonError().apply {
            message = detailsMessage
        }
        return mock<GoogleJsonResponseException>().also { mocked ->
            whenever(mocked.statusCode).thenReturn(statusCode)
            whenever(mocked.statusMessage).thenReturn(statusMessage)
            whenever(mocked.details).thenReturn(error)
            whenever(mocked.message).thenReturn("Error $statusCode: $statusMessage\nDetails: $detailsMessage")
        }
    }
}
