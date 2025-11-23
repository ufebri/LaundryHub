package com.raylabs.laundryhub.ui.navigation

import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.ui.common.navigation.BottomNavItem
import org.junit.Assert.assertEquals
import org.junit.Test

class BottomNavItemTest {

    @Test
    fun `Home nav item has correct values`() {
        assertEquals(R.string.home, BottomNavItem.Home.title)
        assertEquals(R.drawable.ic_home, BottomNavItem.Home.icon)
        assertEquals("home", BottomNavItem.Home.screenRoute)
    }

    @Test
    fun `History nav item has correct values`() {
        assertEquals(R.string.history, BottomNavItem.History.title)
        assertEquals(R.drawable.ic_history, BottomNavItem.History.icon)
        assertEquals("history", BottomNavItem.History.screenRoute)
    }

    @Test
    fun `Order nav item has correct values`() {
        assertEquals(R.string.order, BottomNavItem.Order.title)
        assertEquals(R.drawable.ic_order, BottomNavItem.Order.icon)
        assertEquals("order", BottomNavItem.Order.screenRoute)
    }

    @Test
    fun `Outcome nav item has correct values`() {
        assertEquals(R.string.outcome, BottomNavItem.Outcome.title)
        assertEquals(R.drawable.ic_assignment, BottomNavItem.Outcome.icon)
        assertEquals("outcome", BottomNavItem.Outcome.screenRoute)
    }

    @Test
    fun `Profile nav item has correct values`() {
        assertEquals(R.string.profile, BottomNavItem.Profile.title)
        assertEquals(R.drawable.ic_admin, BottomNavItem.Profile.icon)
        assertEquals("profile", BottomNavItem.Profile.screenRoute)
    }
}