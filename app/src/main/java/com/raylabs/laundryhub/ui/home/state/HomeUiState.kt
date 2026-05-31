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
    val gross: SectionState<List<GrossItem>> = SectionState(),
    val unpaidOrder: SectionState<List<UnpaidOrderItem>> = SectionState(),
    val detailOrder: SectionState<TransactionItem> = SectionState(),
    val reminderDiscovery: ReminderDiscoveryUiState? = null,
    val currentSortOption: SortOption = SortOption.ORDER_DATE_DESC, // Default sort option
    val isRefreshing: Boolean = false, // Added for swipe-to-refresh
    val searchQuery: String = "", // Added for search functionality
    val isSearchActive: Boolean = false, // Added for search functionality
    val refreshCounter: Int = 0, // Added to trigger Paging 3 refresh
    val isSummaryRefreshing: Boolean = false, // Added for reactive sync UI feedback
    val optimisticOrders: List<UnpaidOrderItem> = emptyList(), // Added for Optimistic UI
    val optimisticUpdates: Map<String, UnpaidOrderItem> = emptyMap() // Added for Optimistic UI updates
)

data class ReminderDiscoveryUiState(
    val eligibleCount: Int,
    val headline: String,
    val supportingText: String,
    val isReminderEnabled: Boolean,
    val ctaLabel: String
)
