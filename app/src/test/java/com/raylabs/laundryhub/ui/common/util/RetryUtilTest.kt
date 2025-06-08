package com.raylabs.laundryhub.ui.common.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
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
        val result = retry(times = 3) {
            attempts++
            throw RuntimeException("Always fail")
        }
        assertNull(result)
        assertEquals(3, attempts)
    }

    @Test
    fun `retry should call onRetry callback`() = runTest {
        val retryCalled = mutableListOf<Int>()
        val result = retry(times = 3, onRetry = { retryCalled.add(it) }) {
            throw RuntimeException("Failing always")
        }
        assertNull(result)
        assertEquals(listOf(1, 2), retryCalled) // only 2 retries, then final fail
    }
}