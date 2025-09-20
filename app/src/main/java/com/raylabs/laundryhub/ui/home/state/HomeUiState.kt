package com.raylabs.laundryhub.ui.home.state

import com.raylabs.laundryhub.ui.common.util.SectionState

// Define available sort options
enum class SortOption {
    ORDER_DATE_DESC, // Default: Newest to oldest order date
    ORDER_DATE_ASC,  // Oldest to newest order date
    DUE_DATE_ASC,    // Earliest to latest due date
    DUE_DATE_DESC    // Latest to earliest due date
}

data class HomeUiState(
    val orderUpdateKey: Long = System.currentTimeMillis(),
    val user: SectionState<UserItem> = SectionState(),
    val todayIncome: SectionState<List<TransactionItem>> = SectionState(),
    val summary: SectionState<List<SummaryItem>> = SectionState(),
    val unpaidOrder: SectionState<List<UnpaidOrderItem>> = SectionState(),
    val detailOrder: SectionState<TransactionItem> = SectionState(),
    val currentSortOption: SortOption = SortOption.ORDER_DATE_DESC, // Default sort option
    val isRefreshing: Boolean = false // Added for swipe-to-refresh
)