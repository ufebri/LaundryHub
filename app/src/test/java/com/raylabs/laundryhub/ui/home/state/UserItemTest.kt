package com.raylabs.laundryhub.ui.home.state

import com.raylabs.laundryhub.core.domain.model.auth.User
import org.junit.Assert.assertEquals
import org.junit.Test

class UserItemTest {

    @Test
    fun `toUI maps displayName when present`() {
        val user = User(uid = "1", displayName = "Alice", email = null, urlPhoto = null)

        val result = user.toUI()

        assertEquals("Alice", result.displayName)
    }

    @Test
    fun `toUI returns Guest when displayName is null`() {
        val user = User(uid = "1", displayName = null, email = null, urlPhoto = null)

        val result = user.toUI()

        assertEquals("Guest", result.displayName)
    }
}
