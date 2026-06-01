package com.raylabs.laundryhub.core.data.config

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Test

class KtorBackendHealthCheckerTest {

    @Test
    fun `isHealthy returns false for invalid or unreachable baseUrl`() = runTest {
        val healthChecker = KtorBackendHealthChecker()
        
        // This will fail instantly due to invalid URL or host lookup failure
        val isHealthyResult = healthChecker.isHealthy("https://invalid-unreachable-host.xyz")
        
        assertFalse(isHealthyResult)
    }

    @Test
    fun `isHealthy returns false for blank baseUrl`() = runTest {
        val healthChecker = KtorBackendHealthChecker()
        val isHealthyResult = healthChecker.isHealthy("   ")
        
        assertFalse(isHealthyResult)
    }
}
