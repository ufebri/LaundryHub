package com.raylabs.laundryhub.core.domain.model.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class UserTest {

    @Test
    fun `create user with correct values`() {
        val user = User(
            uid = "123",
            displayName = "Uray",
            email = "uray@example.com",
            urlPhoto = "https://example.com/photo.jpg"
        )

        assertEquals("123", user.uid)
        assertEquals("Uray", user.displayName)
        assertEquals("uray@example.com", user.email)
        assertEquals("https://example.com/photo.jpg", user.urlPhoto)
    }

    @Test
    fun `users with same content should be equal`() {
        val user1 = User("123", "Uray", "uray@example.com", "photo.jpg")
        val user2 = User("123", "Uray", "uray@example.com", "photo.jpg")

        assertEquals(user1, user2)
    }

    @Test
    fun `copy user and change one field`() {
        val original = User("123", "Uray", "uray@example.com", "photo.jpg")
        val copied = original.copy(displayName = "Febri")

        assertEquals("Febri", copied.displayName)
        assertEquals("123", copied.uid) // uid tetap
        assertNotEquals(original, copied)
    }
}