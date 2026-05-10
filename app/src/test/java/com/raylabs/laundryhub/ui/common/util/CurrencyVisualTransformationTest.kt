package com.raylabs.laundryhub.ui.common.util

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyVisualTransformationTest {

    @Test
    fun testCurrencyTransformation() {
        val transformation = CurrencyVisualTransformation()
        
        val input1 = "1000"
        val result1 = transformation.filter(AnnotatedString(input1))
        assertEquals("Rp 1.000", result1.text.text)

        val input2 = "1000000"
        val result2 = transformation.filter(AnnotatedString(input2))
        assertEquals("Rp 1.000.000", result2.text.text)
        
        val input3 = ""
        val result3 = transformation.filter(AnnotatedString(input3))
        assertEquals("", result3.text.text)
    }

    @Test
    fun testOffsetMapping() {
        val transformation = CurrencyVisualTransformation()
        val input = "1000" // "Rp 1.000"
        val result = transformation.filter(AnnotatedString(input))
        
        val mapping = result.offsetMapping
        
        // original to transformed
        assertEquals(3, mapping.originalToTransformed(0)) // "Rp " (3 chars) + 0 digits
        assertEquals(4, mapping.originalToTransformed(1)) // "Rp 1"
        assertEquals(5, mapping.originalToTransformed(2)) // "Rp 10"
        assertEquals(6, mapping.originalToTransformed(3)) // "Rp 100"
        assertEquals(8, mapping.originalToTransformed(4)) // "Rp 1.000"
    }
}
