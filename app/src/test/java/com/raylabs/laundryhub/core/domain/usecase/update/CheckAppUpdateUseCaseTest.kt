package com.raylabs.laundryhub.core.domain.usecase.update

import com.raylabs.laundryhub.core.domain.repository.UpdateCheckerRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckAppUpdateUseCaseTest {

    private class FakeChecker(private val result: Boolean) : UpdateCheckerRepository {
        override suspend fun checkAndPromptIfNeeded(): Boolean = result
    }

    @Test
    fun `should return true when checker reports update available`() = runTest {
        val sut = CheckAppUpdateUseCase(FakeChecker(true))
        val result = sut()
        assertTrue(result)
    }

    @Test
    fun `should return false when checker reports no update`() = runTest {
        val sut = CheckAppUpdateUseCase(FakeChecker(false))
        val result = sut()
        assertFalse(result)
    }
}