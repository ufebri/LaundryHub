package com.raylabs.laundryhub.ui.home.state

import com.raylabs.laundryhub.core.domain.model.auth.User
import org.junit.Assert.assertEquals
import org.junit.Test

class UserItemTest {

    @Test
    fun `toUI maps User to UserItem correctly with displayName`() {
        val user = User(
            uid = "user-1",
            displayName = "Ray Febri",
            email = "ray@example.com",
            urlPhoto = "https://photo.jpg"
        )
        val ui = user.toUI()

        assertEquals("Ray Febri", ui.displayName)
        assertEquals("user-1", ui.uid)
        assertEquals("ray@example.com", ui.email)
    }

    @Test
    fun `toUI uses fallback Guest for null displayName`() {
        val user = User(
            uid = "user-2",
            displayName = null,
            email = null,
            urlPhoto = null
        )
        val ui = user.toUI()

        assertEquals("Guest", ui.displayName)
        assertEquals("user-2", ui.uid)
        assertEquals("", ui.email)
    }
}
