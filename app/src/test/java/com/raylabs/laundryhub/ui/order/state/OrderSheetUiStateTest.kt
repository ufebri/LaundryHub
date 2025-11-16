package com.raylabs.laundryhub.ui.order.state

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderSheetUiStateTest {

    @Test
    fun `openNewSheet sets flags correctly`() {
        val state = OrderSheetUiState().openNewSheet()
        assertTrue(state.showNewOrderSheet)
        assertFalse(state.showEditOrderSheet)
        assertTrue(state.isSheetVisible)
    }

    @Test
    fun `openEditSheet sets flags correctly`() {
        val state = OrderSheetUiState().openEditSheet()
        assertFalse(state.showNewOrderSheet)
        assertTrue(state.showEditOrderSheet)
    }

    @Test
    fun `dismissSheet hides both sheets`() {
        val state = OrderSheetUiState(showNewOrderSheet = true, showEditOrderSheet = true)
            .dismissSheet()
        assertFalse(state.showNewOrderSheet)
        assertFalse(state.showEditOrderSheet)
        assertFalse(state.isSheetVisible)
    }

}
