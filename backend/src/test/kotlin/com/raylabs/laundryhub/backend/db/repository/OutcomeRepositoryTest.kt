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
}
