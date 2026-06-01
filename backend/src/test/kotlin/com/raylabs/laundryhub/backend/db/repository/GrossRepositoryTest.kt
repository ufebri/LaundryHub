package com.raylabs.laundryhub.backend.db.repository

import com.raylabs.laundryhub.backend.db.schema.GrossTable
import com.raylabs.laundryhub.core.domain.model.sheets.GrossData
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GrossRepositoryTest {

    private lateinit var repository: GrossRepository

    @BeforeTest
    fun setUp() {
        Database.connect("jdbc:h2:mem:test_gross;MODE=MySQL;DB_CLOSE_DELAY=-1;", "org.h2.Driver")
        transaction {
            SchemaUtils.create(GrossTable)
        }
        repository = GrossRepository()
    }

    @AfterTest
    fun tearDown() {
        transaction {
            SchemaUtils.drop(GrossTable)
        }
    }

    @Test
    fun testCRUDOperations() = runBlocking {
        val gross = GrossData(month = "Mei 2026", totalNominal = "Rp1.000.000", orderCount = "10", tax = "0")
        
        // Insert
        assertTrue(repository.insert(gross))
        
        // Get all
        val list = repository.getAll()
        assertEquals(1, list.size)
        assertEquals("Mei 2026", list[0].month)

        // Get Unsynced
        val unsynced = repository.getUnsyncedGross()
        assertEquals(1, unsynced.size)

        // Update
        val updatedGross = gross.copy(totalNominal = "Rp1.500.000")
        assertTrue(repository.update("Mei 2026", updatedGross))
        assertEquals("Rp1.500.000", repository.getAll()[0].totalNominal)

        // Mark as Synced
        assertTrue(repository.markAsSynced(listOf("Mei 2026")))
        assertEquals(0, repository.getUnsyncedGross().size)

        // Delete
        assertTrue(repository.delete("Mei 2026"))
        assertEquals(0, repository.getAll().size)
    }

    @Test
    fun testUpsertAndInsertAll() = runBlocking {
        val gross = GrossData(month = "Juni 2026", totalNominal = "Rp2.000.000", orderCount = "15", tax = "0")
        
        // Upsert when doesn't exist (inserts)
        assertTrue(repository.upsert(gross))
        assertEquals(1, repository.getAll().size)

        // Upsert when exists (updates)
        val updated = gross.copy(totalNominal = "Rp2.500.000")
        assertTrue(repository.upsert(updated))
        assertEquals("Rp2.500.000", repository.getAll()[0].totalNominal)

        // InsertAll
        val list = listOf(
            GrossData(month = "Juli 2026", totalNominal = "Rp3.000.000", orderCount = "20", tax = "0"),
            GrossData(month = "Agustus 2026", totalNominal = "Rp4.000.000", orderCount = "30", tax = "0")
        )
        assertEquals(2, repository.insertAll(list))
        assertEquals(3, repository.getAll().size)
    }
}
