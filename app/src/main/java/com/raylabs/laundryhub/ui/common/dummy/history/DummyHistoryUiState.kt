package com.raylabs.laundryhub.ui.common.dummy.history

import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.history.state.HistoryItem
import com.raylabs.laundryhub.ui.history.state.HistoryUiItem
import com.raylabs.laundryhub.ui.history.state.HistoryUiState

val dummyHistoryUiState = HistoryUiState(
    history = SectionState(
        data = listOf(
            HistoryUiItem.Header(date = "1 Juni 2025"),
            HistoryUiItem.Entry(
                item = HistoryItem(
                    orderId = "ORD-001",
                    name = "Ny Emy",
                    paymentStatus = "Lunas",
                    totalPrice = "Rp50.000",
                    packageType = "Express - 24H",
                    formattedDate = "2 Juni 2025",
                    isPaid = true
                )
            ),
            HistoryUiItem.Entry(
                item = HistoryItem(
                    orderId = "ORD-002",
                    name = "Ny Emy",
                    paymentStatus = "Lunas",
                    totalPrice = "Rp50.000",
                    packageType = "Express - 24H",
                    formattedDate = "2 Juni 2025",
                    isPaid = true
                )
            )
        )
    )
)