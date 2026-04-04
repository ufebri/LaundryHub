package com.raylabs.laundryhub.core.domain.usecase.settings

import com.raylabs.laundryhub.core.domain.repository.SpreadsheetConfigRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class ClearSpreadsheetConnectionUseCaseTest {

    private val repository: SpreadsheetConfigRepository = mock()

    @Test
    fun `invoke delegates to repository`() = runTest {
        val useCase = ClearSpreadsheetConnectionUseCase(repository)

        useCase()

        verify(repository).clearSpreadsheetConnection()
    }
}
