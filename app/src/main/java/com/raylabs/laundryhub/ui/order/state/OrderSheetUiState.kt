package com.raylabs.laundryhub.ui.order.state

data class OrderSheetUiState(
    val showNewOrderSheet: Boolean = false,
    val showEditOrderSheet: Boolean = false
) {
    val isSheetVisible: Boolean
        get() = showNewOrderSheet || showEditOrderSheet
}

fun OrderSheetUiState.openNewSheet(): OrderSheetUiState =
    copy(showNewOrderSheet = true, showEditOrderSheet = false)

fun OrderSheetUiState.openEditSheet(): OrderSheetUiState =
    copy(showNewOrderSheet = false, showEditOrderSheet = true)

fun OrderSheetUiState.dismissSheet(): OrderSheetUiState =
    copy(showNewOrderSheet = false, showEditOrderSheet = false)
