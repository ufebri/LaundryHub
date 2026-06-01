package com.raylabs.laundryhub.backend.db.repository

import com.raylabs.laundryhub.backend.db.schema.SummaryTable
import com.raylabs.laundryhub.core.domain.model.sheets.SpreadsheetData
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SummaryRepositoryTest {

    private lateinit var repository: SummaryRepository

    @BeforeTest
    fun setUp() {
        Database.connect("jdbc:h2:mem:test_summary;MODE=MySQL;DB_CLOSE_DELAY=-1;", "org.h2.Driver")
        transaction {
            SchemaUtils.create(SummaryTable)
        }
        repository = SummaryRepository()
    }

    @AfterTest
    fun tearDown() {
        transaction {
            SchemaUtils.drop(SummaryTable)
        }
    }

    @Test
    fun testCRUDOperations() = runBlocking {
        val summary = SpreadsheetData("key_1", "value_1")
        
        // Insert
        assertTrue(repository.insert(summary))
        
        // Get all
        val list = repository.getAll()
        assertEquals(1, list.size)
        assertEquals("key_1", list[0].key)

        // Get Unsynced
        val unsynced = repository.getUnsyncedSummaries()
        assertEquals(1, unsynced.size)

        // Update
        val updated = summary.copy(value = "value_2")
        assertTrue(repository.update("key_1", updated))
        assertEquals("value_2", repository.getAll()[0].value)

        // Mark as Synced
        assertTrue(repository.markAsSynced(listOf("key_1")))
        assertEquals(0, repository.getUnsyncedSummaries().size)

        // Delete
        assertTrue(repository.delete("key_1"))
        assertEquals(0, repository.getAll().size)
    }

    @Test
    fun testUpsertAndInsertAll() = runBlocking {
        val summary = SpreadsheetData("key_2", "value_2")
        
        // Upsert when doesn't exist (inserts)
        assertTrue(repository.upsert(summary))
        assertEquals(1, repository.getAll().size)

        // Upsert when exists (updates)
        val updated = summary.copy(value = "value_3")
        assertTrue(repository.upsert(updated))
        assertEquals("value_3", repository.getAll()[0].value)

        // InsertAll
        val list = listOf(
            SpreadsheetData("key_3", "value_3"),
            SpreadsheetData("key_4", "value_4")
        )
        assertEquals(2, repository.insertAll(list))
        assertEquals(3, repository.getAll().size)
    }
}
