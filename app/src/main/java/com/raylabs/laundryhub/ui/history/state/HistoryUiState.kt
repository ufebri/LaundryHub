package com.raylabs.laundryhub.ui.history.state

import com.raylabs.laundryhub.ui.common.util.SectionState

data class HistoryUiState(
    val history: SectionState<List<HistoryUiItem>> = SectionState()
)
