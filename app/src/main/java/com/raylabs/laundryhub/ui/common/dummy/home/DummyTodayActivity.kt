package com.raylabs.laundryhub.ui.common.dummy.home

import com.raylabs.laundryhub.ui.home.state.TransactionItem
import com.raylabs.laundryhub.ui.theme.PurpleLaundryHub

val dummyTodayActivity = TransactionItem(
    id = "1",
    name = "Customer A",
    totalPrice = "Rp 105.000,-",
    status = "Paid by Cash",
    statusColor = PurpleLaundryHub,
    packageDuration = "Express - 24H"
)