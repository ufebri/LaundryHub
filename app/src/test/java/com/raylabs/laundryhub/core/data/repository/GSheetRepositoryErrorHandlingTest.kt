package com.raylabs.laundryhub.core.data.repository

import com.google.api.client.googleapis.json.GoogleJsonError
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.raylabs.laundryhub.ui.common.util.Resource
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GSheetRepositoryErrorHandlingTest {

    @Test
    fun `handleGoogleJsonResponseException formats message`() {
        val details = GoogleJsonError().apply { message = "Sheet missing" }
        val exception = mock<GoogleJsonResponseException>()
        whenever(exception.statusCode).thenReturn(404)
        whenever(exception.statusMessage).thenReturn("Not Found")
        whenever(exception.details).thenReturn(details)

        val result = GSheetRepositoryErrorHandling.handleGoogleJsonResponseException(exception)

        assertEquals(
            "Error 404: Not Found\nDetails: Sheet missing",
            result.message
        )
    }

    @Test
    fun `handleReadSheetResponseException returns error with message`() {
        val result =
            GSheetRepositoryErrorHandling.handleReadSheetResponseException(Exception("boom"))

        assertEquals(Resource.Error("boom"), result)
    }

    @Test
    fun `handleFailAfterRetry returns standard error`() {
        val result = GSheetRepositoryErrorHandling.handleFailAfterRetry()

        assertEquals(Resource.Error("Failed after 3 attempts."), result)
    }

    @Test
    fun `handleFailedAddOrder returns error`() {
        val result = GSheetRepositoryErrorHandling.handleFailedAddOrder(Exception("add failed"))

        assertEquals(Resource.Error("add failed"), result)
    }

    @Test
    fun `handleFailedUpdate returns error`() {
        val result = GSheetRepositoryErrorHandling.handleFailedUpdate(Exception("update failed"))

        assertEquals(Resource.Error("update failed"), result)
    }

    @Test
    fun `handleIDNotFound returns error`() {
        val result = GSheetRepositoryErrorHandling.handleIDNotFound()

        assertEquals(Resource.Error("ID not found."), result)
    }
}
