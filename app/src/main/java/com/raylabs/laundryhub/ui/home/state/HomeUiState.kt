package com.raylabs.laundryhub.ui.home.state

import com.raylabs.laundryhub.ui.common.util.SectionState

data class HomeUiState(
    val orderUpdateKey: Long = System.currentTimeMillis(),
    val user: SectionState<UserItem> = SectionState(),
    val todayIncome: SectionState<List<TransactionItem>> = SectionState(),
    val summary: SectionState<List<SummaryItem>> = SectionState(),
    val unpaidOrder: SectionState<List<UnpaidOrderItem>> = SectionState(),
    val detailOrder: SectionState<TransactionItem> = SectionState(),
)