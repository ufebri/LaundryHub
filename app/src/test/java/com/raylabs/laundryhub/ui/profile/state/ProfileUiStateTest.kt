package com.raylabs.laundryhub.ui.profile.state

import com.raylabs.laundryhub.ui.common.util.SectionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileUiStateTest {

    @Test
    fun `default ProfileUiState is empty SectionState`() {
        val state = ProfileUiState()
        assertFalse(state.user.isLoading)
        assertNull(state.user.errorMessage)
        assertNull(state.user.data)
        assertFalse(state.logout.isLoading)
        assertNull(state.logout.errorMessage)
        assertNull(state.logout.data)
        assertTrue(state.showWhatsAppOption)
    }

    @Test
    fun `can set user and logout state with full user item`() {
        val userItem = UserItem(
            displayName = "Raihan",
            email = "raihan@example.com",
            photoUrl = "http://img.com/p.jpg"
        )
        val userSection = SectionState(data = userItem)
        val logoutSection = SectionState(data = true)
        val state = ProfileUiState(user = userSection, logout = logoutSection, showWhatsAppOption = false)
        assertEquals("Raihan", state.user.data?.displayName)
        assertEquals("raihan@example.com", state.user.data?.email)
        assertEquals("http://img.com/p.jpg", state.user.data?.photoUrl)
        assertTrue(state.logout.data == true)
        assertFalse(state.showWhatsAppOption)
    }

    @Test
    fun `error and loading states handled for user and logout`() {
        val userSection =
            SectionState<UserItem>(isLoading = true, errorMessage = "fail", data = null)
        val logoutSection =
            SectionState<Boolean>(isLoading = true, errorMessage = "logout error", data = null)
        val state = ProfileUiState(user = userSection, logout = logoutSection)
        assertTrue(state.user.isLoading)
        assertEquals("fail", state.user.errorMessage)
        assertNull(state.user.data)
        assertTrue(state.logout.isLoading)
        assertEquals("logout error", state.logout.errorMessage)
    }
}
