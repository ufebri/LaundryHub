package com.raylabs.laundryhub.ui.common.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ResourceTest {

    @Test
    fun `test success resource holds correct data`() {
        val result = Resource.Success("Hello")
        assertEquals("Hello", result.data)
    }

    @Test
    fun `test error resource holds correct message`() {
        val errorMessage = "Something went wrong"
        val result = Resource.Error(errorMessage)
        assertEquals(errorMessage, result.message)
    }

    @Test
    fun `test loading and empty are singleton`() {
        assertEquals(Resource.Loading, Resource.Loading)
        assertEquals(Resource.Empty, Resource.Empty)
    }
}