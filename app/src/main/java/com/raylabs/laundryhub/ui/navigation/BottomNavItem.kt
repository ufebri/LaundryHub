package com.raylabs.laundryhub.ui.navigation

import com.raylabs.laundryhub.R


sealed class BottomNavItem(var title: String, var icon: Int, var screen_route: String) {

    object Home : BottomNavItem("Home", R.drawable.ic_home, "home")
    object History : BottomNavItem("History", R.drawable.ic_history, "history")
    object Order : BottomNavItem("Order", R.drawable.ic_order, "order")
    object ToDo : BottomNavItem("ToDo", R.drawable.ic_assignment, "todo")
    object Admin : BottomNavItem("Admin", R.drawable.ic_admin, "admin")
}
