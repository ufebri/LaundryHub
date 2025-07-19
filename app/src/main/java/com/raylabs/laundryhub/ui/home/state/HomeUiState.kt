package com.raylabs.laundryhub.ui.home.state

import com.raylabs.laundryhub.ui.common.util.SectionState

data class HomeUiState(
    val orderUpdateKey: Long = System.currentTimeMillis(),
    val user: SectionState<UserItem> = SectionState(),
    val todayIncome: SectionState<List<TransactionItem>> = SectionState(),
    val summary: SectionState<List<SummaryItem>> = SectionState(),
    val orderStatus: SectionState<List<UnpaidOrderItem>> = SectionState(),
    val historyOrder: SectionState<TransactionItem> = SectionState(),

    val selectedOrderID: String? = null
)