package com.raylabs.laundryhub.ui.home.state

import com.raylabs.laundryhub.ui.common.util.SectionState

data class HomeUiState(
    val user: SectionState<UserItem> = SectionState(),
    val todayIncome: SectionState<List<TodayActivityItem>> = SectionState(),
    val summary: SectionState<List<SummaryItem>> = SectionState(),
    val orderStatus: SectionState<List<PendingOrderItem>> = SectionState()
)