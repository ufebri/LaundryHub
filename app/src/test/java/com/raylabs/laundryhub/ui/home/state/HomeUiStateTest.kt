package com.raylabs.laundryhub.ui.home.state

import androidx.compose.ui.graphics.Color
import com.raylabs.laundryhub.ui.common.util.SectionState
import org.junit.Assert.*
import org.junit.Test

class HomeUiStateTest {

    @Test
    fun `default HomeUiState has expected default values`() {
        val state = HomeUiState()
        // orderUpdateKey default harus sekarang (tidak null, > 0)
        assertTrue(state.orderUpdateKey > 0)
        // Semua SectionState default kosong
        assertNull(state.user.data)
        assertNull(state.todayIncome.data)
        assertNull(state.summary.data)
        assertNull(state.unpaidOrder.data)
        assertNull(state.detailOrder.data)
        // Tidak error & tidak loading
        assertNull(state.user.errorMessage)
        assertFalse(state.user.isLoading)
    }

    @Test
    fun `state with loaded todayIncome and summary returns correct values`() {
        val todayIncomeList = listOf(TransactionItem("ORD1", "Test", "01/08/2025", "10000", Color(0xFFFEF7FF), "1 Week"))
        val summaryList = listOf(SummaryItem(title = "Ready To Pick", body = "32 Orders", footer = "Send message →", backgroundColor = Color(0xFFFEF7FF), textColor = Color.Black))
        val loadedTodayIncome = SectionState(data = todayIncomeList)
        val loadedSummary = SectionState(data = summaryList)
        val state = HomeUiState(
            todayIncome = loadedTodayIncome,
            summary = loadedSummary
        )
        assertEquals(todayIncomeList, state.todayIncome.data)
        assertEquals(summaryList, state.summary.data)
        assertNull(state.todayIncome.errorMessage)
        assertFalse(state.summary.isLoading)
    }

    @Test
    fun `can update orderUpdateKey value`() {
        val newKey = 123456789L
        val state = HomeUiState(orderUpdateKey = newKey)
        assertEquals(newKey, state.orderUpdateKey)
    }

    @Test
    fun `state with error and loading states set correctly`() {
        val sectionError = SectionState<List<TransactionItem>>(errorMessage = "Failed", isLoading = false)
        val sectionLoading = SectionState<List<SummaryItem>>(isLoading = true)
        val state = HomeUiState(
            todayIncome = sectionError,
            summary = sectionLoading
        )
        assertEquals("Failed", state.todayIncome.errorMessage)
        assertTrue(state.summary.isLoading)
    }
}