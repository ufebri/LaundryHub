package com.raylabs.laundryhub.core.data.repository

import com.raylabs.laundryhub.shared.util.Resource
import org.junit.Assert.assertEquals
import org.junit.Test

class LaundryRepositoryErrorHandlingTest {

    @Test
    fun `handleException returns generic error for normal exceptions`() {
        val exception = RuntimeException("Normal Database Error")
        val result = LaundryRepositoryErrorHandling.handleException(exception)
        
        assertEquals(Resource.Error("Normal Database Error"), result)
    }

    @Test
    fun `handleException returns unauthorized error for invalid credentials exception details`() {
        val exception = RuntimeException("The request failed due to invalid authentication credentials.")
        val result = LaundryRepositoryErrorHandling.handleException(exception)
        
        assertEquals(Resource.Error(LaundryRepositoryErrorHandling.AUTHORIZATION_RECONNECT_REQUIRED_MESSAGE), result)
    }

    @Test
    fun `handleReadSheetResponseException returns generic error for normal exceptions`() {
        val exception = RuntimeException("Network timeout")
        val result = LaundryRepositoryErrorHandling.handleReadSheetResponseException(exception)
        
        assertEquals(Resource.Error("Network timeout"), result)
    }

    @Test
    fun `handleReadSheetResponseException returns unauthorized error for invalid credentials exception details`() {
        val exception = RuntimeException("contains invalid authentication credentials in it")
        val result = LaundryRepositoryErrorHandling.handleReadSheetResponseException(exception)
        
        assertEquals(Resource.Error(LaundryRepositoryErrorHandling.AUTHORIZATION_RECONNECT_REQUIRED_MESSAGE), result)
    }

    @Test
    fun `handleFailAfterRetry returns standard failed message`() {
        val result = LaundryRepositoryErrorHandling.handleFailAfterRetry()
        assertEquals(Resource.Error("Failed after 3 attempts."), result)
    }

    @Test
    fun `handleFailedAddOrder delegates correctly`() {
        val exception = RuntimeException("invalid authentication credentials")
        val result = LaundryRepositoryErrorHandling.handleFailedAddOrder(exception)
        assertEquals(Resource.Error(LaundryRepositoryErrorHandling.AUTHORIZATION_RECONNECT_REQUIRED_MESSAGE), result)
    }

    @Test
    fun `handleFailedUpdate delegates correctly`() {
        val exception = RuntimeException("Network Error")
        val result = LaundryRepositoryErrorHandling.handleFailedUpdate(exception)
        assertEquals(Resource.Error("Network Error"), result)
    }

    @Test
    fun `handleFailedDelete delegates correctly`() {
        val exception = RuntimeException("Network Error")
        val result = LaundryRepositoryErrorHandling.handleFailedDelete(exception)
        assertEquals(Resource.Error("Network Error"), result)
    }

    @Test
    fun `handleIDNotFound returns ID not found error`() {
        val result = LaundryRepositoryErrorHandling.handleIDNotFound()
        assertEquals(Resource.Error("ID not found."), result)
    }
}
