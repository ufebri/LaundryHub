package com.raylabs.laundryhub.backend.db.repository

import com.raylabs.laundryhub.backend.db.schema.SyncDeleteEventsTable
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

class SyncDeleteEventRepositoryTest {

    private lateinit var repository: SyncDeleteEventRepository

    @BeforeTest
    fun setup() {
        Database.connect("jdbc:h2:mem:test-sync-delete;MODE=MySQL;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.create(SyncDeleteEventsTable)
        }
        repository = SyncDeleteEventRepository()
    }

    @AfterTest
    fun tearDown() {
        transaction {
            SchemaUtils.drop(SyncDeleteEventsTable)
        }
    }

    @Test
    fun `record stores one pending delete event per entity`() = runBlocking {
        assertTrue(repository.record(SyncEntityType.ORDER, "42"))
        assertFalse(repository.record(SyncEntityType.ORDER, "42"))

        val pending = repository.getPending()

        assertEquals(1, pending.size)
        assertEquals(SyncEntityType.ORDER, pending.single().entityType)
        assertEquals("42", pending.single().entityId)
    }

    @Test
    fun `markProcessed removes completed delete events`() = runBlocking {
        repository.record(SyncEntityType.OUTCOME, "9")
        val event = repository.getPending().single()

        assertTrue(repository.markProcessed(listOf(event.id)))

        assertEquals(emptyList(), repository.getPending())
    }
}
