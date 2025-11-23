package com.raylabs.laundryhub.ui.common.dummy.history

import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.history.state.HistoryUiState
import com.raylabs.laundryhub.ui.outcome.state.DateListItemUI
import com.raylabs.laundryhub.ui.outcome.state.EntryItem
import com.raylabs.laundryhub.ui.outcome.state.TypeCard

val dummyHistoryUiState = HistoryUiState(
    history = SectionState(
        data = listOf(
            DateListItemUI.Header(date = "1 Juni 2025"),
            DateListItemUI.Entry(
                item = EntryItem(
                    id = "ORD-001",
                    name = "Ny Emy",
                    paymentStatus = "Lunas",
                    price = "Rp50.000",
                    remark = "Express - 24H",
                    date = "2 Juni 2025",
                    typeCard = TypeCard.INCOME
                )
            ),
            DateListItemUI.Entry(
                item = EntryItem(
                    id = "ORD-002",
                    name = "Ny Emy",
                    paymentStatus = "Lunas",
                    price = "Rp50.000",
                    remark = "Express - 24H",
                    date = "2 Juni 2025",
                    typeCard = TypeCard.INCOME
                )
            )
        )
    )
)