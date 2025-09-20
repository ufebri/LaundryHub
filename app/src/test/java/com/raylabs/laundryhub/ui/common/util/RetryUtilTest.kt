package com.raylabs.laundryhub.ui.common.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RetryUtilTest {

    @Test
    fun `retry should succeed on first attempt`() = runTest {
        var attempts = 0
        val result = retry {
            attempts++
            "Success"
        }
        assertEquals("Success", result)
        assertEquals(1, attempts)
    }

    @Test
    fun `retry should retry and then succeed`() = runTest {
        var attempts = 0
        val result = retry {
            if (++attempts < 3) {
                throw RuntimeException("Fail attempt $attempts")
            }
            "Recovered"
        }
        assertEquals("Recovered", result)
        assertEquals(3, attempts)
    }

    @Test
    fun `retry should return null after max attempts`() = runTest {
        var attempts = 0
        assertNull(
            retry<String>(times = 3) {
                attempts++
                throw RuntimeException("Always fail")
            }
        )
        assertEquals(3, attempts)
    }

    @Test
    fun `retry should call onRetry callback`() = runTest {
        val retryCalled = mutableListOf<Int>()

        // Inline the call to avoid IDE warning "Value of 'result' is always null"
        assertNull(
            retry<String>(times = 3, onRetry = { retryCalled.add(it) }) {
                throw RuntimeException("Failing always")
            }
        )

        // Verify the onRetry callback was invoked for each retry attempt
        assertEquals(listOf(1, 2), retryCalled) // only 2 retries, then final fail
    }
}