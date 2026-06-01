package com.raylabs.laundryhub.shared.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResourceTest {

    @Test
    fun testLoading() {
        val resource = Resource.Loading
        assertTrue(resource is Resource.Loading)
    }

    @Test
    fun testSuccess() {
        val resource = Resource.Success("data")
        assertEquals("data", resource.data)
    }

    @Test
    fun testError() {
        val resource = Resource.Error("An error occurred")
        assertEquals("An error occurred", resource.message)
    }

    @Test
    fun testEmpty() {
        val resource = Resource.Empty
        assertTrue(resource is Resource.Empty)
    }
}
