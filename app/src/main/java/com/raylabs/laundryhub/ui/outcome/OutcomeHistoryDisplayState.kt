package com.raylabs.laundryhub.ui.outcome

import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.outcome.state.OutcomeUiItem

sealed interface OutcomeHistoryDisplayState {
    data object Loading : OutcomeHistoryDisplayState
    data class Error(val message: String) : OutcomeHistoryDisplayState
    data object Empty : OutcomeHistoryDisplayState
    data class Populated(val items: List<OutcomeUiItem>) : OutcomeHistoryDisplayState
}

fun SectionState<List<OutcomeUiItem>>.toDisplayState(): OutcomeHistoryDisplayState {
    if (isLoading) return OutcomeHistoryDisplayState.Loading
    errorMessage?.let { return OutcomeHistoryDisplayState.Error(it) }
    val currentItems = data
    return if (currentItems.isNullOrEmpty()) {
        OutcomeHistoryDisplayState.Empty
    } else {
        OutcomeHistoryDisplayState.Populated(currentItems)
    }
}
