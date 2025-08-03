package com.raylabs.laundryhub.ui.history.state

import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.history.state.HistoryUiItem
import com.raylabs.laundryhub.ui.history.state.HistoryUiState
import org.junit.Assert.*
import org.junit.Test

class HistoryUiStateTest {

    @Test
    fun `default state should have empty SectionState list`() {
        val state = HistoryUiState()
        assertNotNull(state.history)
        assertTrue(state.history.data == null || state.history.data!!.isEmpty())
    }

    @Test
    fun `state with loaded data returns correct items`() {
        val items = listOf<HistoryUiItem>(
            HistoryUiItem.Header("2025-08-01")
        )
        val loadedSection = SectionState<List<HistoryUiItem>>(
            data = items,
            isLoading = false,
            errorMessage = null
        )
        val state = HistoryUiState(history = loadedSection)
        assertEquals(items, state.history.data)
        assertFalse(state.history.isLoading)
        assertNull(state.history.errorMessage)
    }

    @Test
    fun `state with error returns error message`() {
        val section = SectionState<List<HistoryUiItem>>(
            errorMessage = "Network error",
            isLoading = false,
            data = null
        )
        val state = HistoryUiState(history = section)
        assertEquals("Network error", state.history.errorMessage)
        assertNull(state.history.data)
        assertFalse(state.history.isLoading)
    }

    @Test
    fun `state with loading returns loading true`() {
        val section = SectionState<List<HistoryUiItem>>(
            isLoading = true,
            errorMessage = null,
            data = null
        )
        val state = HistoryUiState(history = section)
        assertTrue(state.history.isLoading)
        assertNull(state.history.errorMessage)
        assertNull(state.history.data)
    }
}