package com.raylabs.laundryhub.ui.profile.state

import com.raylabs.laundryhub.core.domain.model.auth.User
import org.junit.Assert.assertEquals
import org.junit.Test

class UserItemTest {
    @Test
    fun `toUI maps all fields correctly`() {
        val domainUser = User(
            uid = "123",
            displayName = "Raihan",
            email = "raihan@example.com",
            urlPhoto = "http://img.com/p.jpg"
        )
        val ui = domainUser.toUI()
        assertEquals("Raihan", ui.displayName)
        assertEquals("raihan@example.com", ui.email)
        assertEquals("http://img.com/p.jpg", ui.photoUrl)
    }
}