package com.raylabs.laundryhub.backend.db.repository

import com.raylabs.laundryhub.backend.db.schema.OutcomesTable
import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OutcomeRepositoryTest {

    private lateinit var repository: OutcomeRepository

    @BeforeTest
    fun setup() {
        Database.connect("jdbc:h2:mem:test_outcome;MODE=MySQL;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")
        transaction {
            org.jetbrains.exposed.sql.transactions.TransactionManager.current().exec(
                "CREATE ALIAS IF NOT EXISTS pg_advisory_xact_lock AS 'long pgAdvisoryXactLock(long val) { return val; }'"
            )
            SchemaUtils.create(OutcomesTable)
        }
        repository = OutcomeRepository()
    }

    @AfterTest
    fun tearDown() {
        transaction {
            SchemaUtils.drop(OutcomesTable)
        }
    }

    @Test
    fun testInsertAndGetOutcome() = runBlocking {
        val outcome = OutcomeData(
            id = "1",
            date = "10/05/2026",
            purpose = "Soap",
            price = "50000",
            remark = "Beli sabun",
            payment = "Cash"
        )

        val inserted = repository.insert(outcome)
        assertTrue(inserted)

        val retrieved = repository.getById("1")
        assertEquals("Soap", retrieved?.purpose)
        assertEquals("50000", retrieved?.price)
    }

    @Test
    fun `getAll sorts by date descending before id descending`() = runBlocking {
        val outcomes = listOf(
            OutcomeData("10", "08 May 2026", "Old high id", "1000", "", "cash"),
            OutcomeData("2", "15 Mei 2026", "Latest", "2000", "", "cash"),
            OutcomeData("3", "14/05/2026", "Middle", "3000", "", "cash")
        )

        outcomes.forEach { repository.insert(it) }

        val result = repository.getAll()

        assertEquals(listOf("2", "3", "10"), result.map { it.id })
    }

    @Test
    fun testInsertWithNextId() = runBlocking {
        val outcome = OutcomeData(
            id = "",
            date = "10/05/2026",
            purpose = "Soap",
            price = "50000",
            remark = "Beli sabun",
            payment = "Cash"
        )
        val created = repository.insertWithNextId(outcome)
        kotlin.test.assertNotNull(created)
        assertEquals("1", created.id)
        
        val second = repository.insertWithNextId(outcome)
        kotlin.test.assertNotNull(second)
        assertEquals("2", second.id)
    }

    @Test
    fun testUpdateOutcome() = runBlocking {
        val outcome = OutcomeData("1", "10/05/2026", "Soap", "50000", "Beli sabun", "Cash")
        repository.insert(outcome)
        
        val updated = outcome.copy(purpose = "Bleach", price = "60000")
        val success = repository.update("1", updated)
        assertTrue(success)
        
        val retrieved = repository.getById("1")
        assertEquals("Bleach", retrieved?.purpose)
        assertEquals("60000", retrieved?.price)
    }

    @Test
    fun testUpsertOutcome() = runBlocking {
        val outcome = OutcomeData("1", "10/05/2026", "Soap", "50000", "Beli sabun", "Cash")
        
        val successInsert = repository.upsert(outcome)
        assertTrue(successInsert)
        
        val updated = outcome.copy(purpose = "Bleach")
        val successUpdate = repository.upsert(updated)
        assertTrue(successUpdate)
        
        val retrieved = repository.getById("1")
        assertEquals("Bleach", retrieved?.purpose)
    }

    @Test
    fun testDeleteOutcome() = runBlocking {
        val outcome = OutcomeData("1", "10/05/2026", "Soap", "50000", "Beli sabun", "Cash")
        repository.insert(outcome)
        
        val success = repository.delete("1")
        assertTrue(success)
        
        val retrieved = repository.getById("1")
        assertEquals(null, retrieved)
    }

    @Test
    fun testGetLatestIdAndGetNextId() = runBlocking {
        assertEquals("0", repository.getLatestId())
        assertEquals("1", repository.getNextId())
        
        repository.insert(OutcomeData("5", "10/05/2026", "Soap", "50000", "Beli sabun", "Cash"))
        
        assertEquals("5", repository.getLatestId())
        assertEquals("6", repository.getNextId())
    }

    @Test
    fun testInsertAllAndGetUnsyncedAndMarkSynced() = runBlocking {
        val outcomes = listOf(
            OutcomeData("1", "10/05/2026", "Soap", "50000", "Beli sabun", "Cash"),
            OutcomeData("2", "11/05/2026", "Bleach", "30000", "Beli pemutih", "Cash")
        )
        
        val count = repository.insertAll(outcomes)
        assertEquals(2, count)
        
        assertEquals(0, repository.getUnsyncedOutcomes().size)
        
        repository.insert(OutcomeData("3", "12/05/2026", "Hanger", "20000", "", "Cash"))
        val unsynced = repository.getUnsyncedOutcomes()
        assertEquals(1, unsynced.size)
        assertEquals("3", unsynced[0].id)
        
        val marked = repository.markAsSynced(listOf("3"))
        assertTrue(marked)
        assertEquals(0, repository.getUnsyncedOutcomes().size)
    }
}
