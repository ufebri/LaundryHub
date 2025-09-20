package com.raylabs.laundryhub.ui.common.dummy.home

import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.home.state.DUMMY_SUMMARY_ITEM
import com.raylabs.laundryhub.ui.home.state.HomeUiState
import com.raylabs.laundryhub.ui.home.state.SortOption
import com.raylabs.laundryhub.ui.home.state.TransactionItem
import com.raylabs.laundryhub.ui.home.state.UnpaidOrderItem
import com.raylabs.laundryhub.ui.home.state.UserItem
import com.raylabs.laundryhub.ui.theme.PurpleLaundryHub

// Extracted UnpaidOrderItems for reusability and clarity
val DUMMY_UNPAID_ORDER_ITEM_EMY: UnpaidOrderItem = UnpaidOrderItem(
    orderID = "1",
    customerName = "Ny Emy",
    packageType = "Express - 6H",
    nowStatus = "Unpaid",
    dueDate = "17 Sep 25, 16.40 PM",
    orderDate = "15 Sep 25, 10.00 AM"
)

val DUMMY_UNPAID_ORDER_ITEM_GABRIEL: UnpaidOrderItem = UnpaidOrderItem(
    orderID = "2",
    customerName = "Gabriel",
    packageType = "Express - 24H",
    nowStatus = "Unpaid",
    dueDate = "17 Sep 25, 16.40 PM",
    orderDate = "16 Sep 25, 11.30 AM"
)

val DUMMY_UNPAID_ORDER_ITEM_ARIFIN: UnpaidOrderItem = UnpaidOrderItem(
    orderID = "3",
    customerName = "Arifin",
    packageType = "Regular",
    nowStatus = "Unpaid",
    dueDate = "17 Sep 25, 16.40 PM",
    orderDate = "14 Sep 25, 09.15 AM"
)

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
    unpaidOrder = SectionState(
        data = listOf(
            DUMMY_UNPAID_ORDER_ITEM_EMY,
            DUMMY_UNPAID_ORDER_ITEM_GABRIEL,
            DUMMY_UNPAID_ORDER_ITEM_ARIFIN
        )
    ),
    currentSortOption = SortOption.ORDER_DATE_DESC
)
