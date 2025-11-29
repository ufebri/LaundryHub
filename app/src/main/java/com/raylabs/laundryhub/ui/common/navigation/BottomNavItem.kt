package com.raylabs.laundryhub.ui.common.navigation

import androidx.annotation.StringRes
import com.raylabs.laundryhub.R


sealed class BottomNavItem(
    @field:StringRes var title: Int,
    var icon: Int,
    var screenRoute: String
) {


    object Home : BottomNavItem(R.string.home, R.drawable.ic_home, "home")
    object History : BottomNavItem(R.string.history, R.drawable.ic_history, "history")
    object Order : BottomNavItem(R.string.order, R.drawable.ic_order, "order")
    object Outcome : BottomNavItem(R.string.outcome, R.drawable.ic_assignment, "outcome")
    object Profile : BottomNavItem(R.string.profile, R.drawable.ic_admin, "profile")
}
