package com.raylabs.laundryhub.backend.db.repository

import com.raylabs.laundryhub.backend.db.schema.DeviceTokensTable
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeviceTokenRepositoryTest {

    private lateinit var repository: DeviceTokenRepository

    @BeforeTest
    fun setup() {
        Database.connect("jdbc:h2:mem:test_device_tokens;MODE=MySQL;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.create(DeviceTokensTable)
        }
        repository = DeviceTokenRepository()
    }

    @AfterTest
    fun tearDown() {
        transaction {
            SchemaUtils.drop(DeviceTokensTable)
        }
    }

    @Test
    fun `registerToken trims and stores one token row`() = runBlocking {
        assertTrue(repository.registerToken("  fcm-token-1  "))

        assertEquals(listOf("fcm-token-1"), repository.getAllTokens())
    }

    @Test
    fun `registerToken updates existing token without duplicating it`() = runBlocking {
        assertTrue(repository.registerToken("fcm-token-1"))
        assertTrue(repository.registerToken("fcm-token-1"))

        assertEquals(listOf("fcm-token-1"), repository.getAllTokens())
    }

    @Test
    fun `registerToken rejects blank token`() = runBlocking {
        assertFalse(repository.registerToken("   "))

        assertEquals(emptyList<String>(), repository.getAllTokens())
    }
}
