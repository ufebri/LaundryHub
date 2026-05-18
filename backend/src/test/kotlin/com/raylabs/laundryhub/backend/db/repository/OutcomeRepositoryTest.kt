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
            id = "OUT-001",
            date = "10/05/2026",
            purpose = "Soap",
            price = "50000",
            remark = "Beli sabun",
            payment = "Cash"
        )

        val inserted = repository.insert(outcome)
        assertTrue(inserted)

        val retrieved = repository.getById("OUT-001")
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
}
