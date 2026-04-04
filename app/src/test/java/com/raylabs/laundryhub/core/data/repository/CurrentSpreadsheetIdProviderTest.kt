package com.raylabs.laundryhub.core.data.repository

import com.raylabs.laundryhub.core.domain.model.settings.SpreadsheetConfig
import com.raylabs.laundryhub.core.domain.repository.SpreadsheetConfigRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class CurrentSpreadsheetIdProviderTest {

    private val repository: SpreadsheetConfigRepository = mock()

    @Test
    fun `getSpreadsheetId returns configured spreadsheet id`() = runTest {
        whenever(repository.spreadsheetConfig).thenReturn(
            flowOf(SpreadsheetConfig(spreadsheetId = "sheet-123"))
        )

        val provider = CurrentSpreadsheetIdProvider(repository)

        assertEquals("sheet-123", provider.getSpreadsheetId())
    }

    @Test
    fun `getSpreadsheetId returns null when no spreadsheet is configured`() = runTest {
        whenever(repository.spreadsheetConfig).thenReturn(flowOf(SpreadsheetConfig()))

        val provider = CurrentSpreadsheetIdProvider(repository)

        assertNull(provider.getSpreadsheetId())
    }
}
