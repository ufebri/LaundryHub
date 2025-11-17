package com.raylabs.laundryhub.ui.outcome

import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.outcome.state.OutcomeUiItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OutcomeHistoryDisplayStateTest {

    @Test
    fun `returns loading when history is fetching`() {
        val displayState = SectionState<List<OutcomeUiItem>>(isLoading = true).toDisplayState()

        assertTrue(displayState is OutcomeHistoryDisplayState.Loading)
    }

    @Test
    fun `returns error when message is present`() {
        val displayState = SectionState<List<OutcomeUiItem>>(errorMessage = "Oops").toDisplayState()

        assertTrue(displayState is OutcomeHistoryDisplayState.Error)
        assertEquals("Oops", (displayState as OutcomeHistoryDisplayState.Error).message)
    }

    @Test
    fun `returns empty when no items available`() {
        val displayState = SectionState<List<OutcomeUiItem>>(data = emptyList()).toDisplayState()

        assertTrue(displayState is OutcomeHistoryDisplayState.Empty)
    }

    @Test
    fun `returns populated when items exist`() {
        val rows = listOf(OutcomeUiItem.Header("Today"))
        val displayState = SectionState<List<OutcomeUiItem>>(data = rows).toDisplayState()

        assertTrue(displayState is OutcomeHistoryDisplayState.Populated)
        assertEquals(rows, (displayState as OutcomeHistoryDisplayState.Populated).items)
    }
}
