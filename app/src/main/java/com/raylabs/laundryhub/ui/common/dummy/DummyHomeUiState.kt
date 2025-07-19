package com.raylabs.laundryhub.ui.common.dummy

import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.home.state.DUMMY_SUMMARY_ITEM
import com.raylabs.laundryhub.ui.home.state.HomeUiState
import com.raylabs.laundryhub.ui.home.state.TransactionItem
import com.raylabs.laundryhub.ui.home.state.UnpaidOrderItem
import com.raylabs.laundryhub.ui.home.state.UserItem
import com.raylabs.laundryhub.ui.theme.PurpleLaundryHub

val dummyState = HomeUiState(
    user = SectionState(data = UserItem(displayName = "Jhon Doe")),
    todayIncome = SectionState(
        data = listOf(
            TransactionItem(
                id = "1",
                name = "Customer A",
                totalPrice = "Rp 105.000,-",
                status = "lunas",
                statusColor = PurpleLaundryHub,
                packageDuration = "Express - 24H"
            )
        )
    ),
    summary = SectionState(data = DUMMY_SUMMARY_ITEM),
    orderStatus = SectionState(
        data = listOf(
            UnpaidOrderItem(
                "1",
                "Ny Emy",
                "Express - 6H",
                "Unpaid",
                "17 Sep 25, 16.40 PM"
            ),
            UnpaidOrderItem(
                "2",
                "Gabriel",
                "Express - 24H",
                "Unpaid",
                "17 Sep 25, 16.40 PM"
            ),
            UnpaidOrderItem(
                "3",
                "Arifin",
                "Regular",
                "Unpaid",
                "17 Sep 25, 16.40 PM"
            )
        )
    )
)