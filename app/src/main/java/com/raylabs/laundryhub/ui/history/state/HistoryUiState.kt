package com.raylabs.laundryhub.ui.history.state

import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.outcome.state.DateListItemUI

data class HistoryUiState(
    val history: SectionState<List<DateListItemUI>> = SectionState()
)
