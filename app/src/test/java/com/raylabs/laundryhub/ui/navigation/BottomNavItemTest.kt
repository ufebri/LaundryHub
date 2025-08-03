package com.raylabs.laundryhub.ui.navigation

import com.raylabs.laundryhub.R
import org.junit.Assert.assertEquals
import org.junit.Test

class BottomNavItemTest {

    @Test
    fun `Home nav item has correct values`() {
        assertEquals("Home", BottomNavItem.Home.title)
        assertEquals(R.drawable.ic_home, BottomNavItem.Home.icon)
        assertEquals("home", BottomNavItem.Home.screenRoute)
    }

    @Test
    fun `History nav item has correct values`() {
        assertEquals("History", BottomNavItem.History.title)
        assertEquals(R.drawable.ic_history, BottomNavItem.History.icon)
        assertEquals("history", BottomNavItem.History.screenRoute)
    }

    @Test
    fun `Order nav item has correct values`() {
        assertEquals("Order", BottomNavItem.Order.title)
        assertEquals(R.drawable.ic_order, BottomNavItem.Order.icon)
        assertEquals("order", BottomNavItem.Order.screenRoute)
    }

    @Test
    fun `Inventory nav item has correct values`() {
        assertEquals("Inventory", BottomNavItem.Inventory.title)
        assertEquals(R.drawable.ic_assignment, BottomNavItem.Inventory.icon)
        assertEquals("inventory", BottomNavItem.Inventory.screenRoute)
    }

    @Test
    fun `Profile nav item has correct values`() {
        assertEquals("Profile", BottomNavItem.Profile.title)
        assertEquals(R.drawable.ic_admin, BottomNavItem.Profile.icon)
        assertEquals("profile", BottomNavItem.Profile.screenRoute)
    }
}