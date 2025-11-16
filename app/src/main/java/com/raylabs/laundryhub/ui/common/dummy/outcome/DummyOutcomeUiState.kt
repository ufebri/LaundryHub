package com.raylabs.laundryhub.ui.common.dummy.outcome

import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.outcome.state.OutcomeHistoryItem
import com.raylabs.laundryhub.ui.outcome.state.OutcomeUiItem
import com.raylabs.laundryhub.ui.outcome.state.OutcomeUiState

val dummyOutcomeUiState = OutcomeUiState(
    history = SectionState(
        data = listOf(
            OutcomeUiItem.Header("16 November 2025"),
            OutcomeUiItem.Entry(
                OutcomeHistoryItem(
                    id = "12",
                    purpose = "Gas 3KG",
                    remark = "Kelontong depan",
                    paymentLabel = "Paid by Cash",
                    price = "Rp56.000"
                )
            ),
            OutcomeUiItem.Entry(
                OutcomeHistoryItem(
                    id = "13",
                    purpose = "Gas 3KG",
                    remark = "Kelontong depan",
                    paymentLabel = "Paid by QRIS",
                    price = "Rp56.000"
                )
            )
        )
    )
)
