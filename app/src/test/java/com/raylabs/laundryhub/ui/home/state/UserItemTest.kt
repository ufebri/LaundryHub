package com.raylabs.laundryhub.ui.home.state

import com.raylabs.laundryhub.core.domain.model.auth.User
import org.junit.Assert.assertEquals
import org.junit.Test

class UserItemTest {

    @Test
    fun `toUI maps displayName when not null`() {
        val user = User(
            uid = "123",
            displayName = "Fahmi",
            email = "fahmi@mail.com",
            urlPhoto = "https://example.com/photo.jpg"
        )
        val uiItem = user.toUI()
        assertEquals("Fahmi", uiItem.displayName)
    }

    @Test
    fun `toUI uses Guest when displayName is null`() {
        val user = User(
            uid = "123",
            displayName = null,
            email = "test@mail.com",
            urlPhoto = "https://photo"
        )
        val uiItem = user.toUI()
        assertEquals("Guest", uiItem.displayName)
    }
}