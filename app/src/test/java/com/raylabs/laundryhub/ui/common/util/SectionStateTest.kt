package com.raylabs.laundryhub.ui.common.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SectionStateTest {

    @Test
    fun `loading() should set isLoading true and clear errorMessage`() {
        val initial = SectionState(data = "Some Data", errorMessage = "Error occurred")
        val result = initial.loading()

        assertTrue(result.isLoading)
        assertEquals("Some Data", result.data) // data tetap
        assertNull(result.errorMessage)
    }

    @Test
    fun `success() should set isLoading false and update data`() {
        val initial = SectionState<String>(isLoading = true)
        val result = initial.success("Success Data")

        assertFalse(result.isLoading)
        assertEquals("Success Data", result.data)
        assertNull(result.errorMessage)
    }

    @Test
    fun `error() should set isLoading false and update errorMessage`() {
        val initial = SectionState<String>(isLoading = true, data = null)
        val result = initial.error("Something went wrong")

        assertFalse(result.isLoading)
        assertEquals(null, result.data) // data tetap
        assertEquals("Something went wrong", result.errorMessage)
    }
}