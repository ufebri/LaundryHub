package com.raylabs.laundryhub.ui.navigation

import com.raylabs.laundryhub.R


sealed class BottomNavItem(var title: String, var icon: Int, var screenRoute: String) {

    object Home : BottomNavItem("Home", R.drawable.ic_home, "home")
    object History : BottomNavItem("History", R.drawable.ic_history, "history")
    object Order : BottomNavItem("Order", R.drawable.ic_order, "order")
    object Inventory : BottomNavItem("Inventory", R.drawable.ic_assignment, "inventory")
    object Profile : BottomNavItem("Profile", R.drawable.ic_admin, "profile")
}
