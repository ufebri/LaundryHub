package com.raylabs.laundryhub.backend.db.repository

import com.raylabs.laundryhub.backend.db.schema.PackagesTable
import com.raylabs.laundryhub.core.domain.model.sheets.PackageData
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PackageRepositoryTest {

    private lateinit var repository: PackageRepository

    @BeforeTest
    fun setup() {
        Database.connect("jdbc:h2:mem:test_package;MODE=MySQL;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.create(PackagesTable)
        }
        repository = PackageRepository()
    }

    @AfterTest
    fun tearDown() {
        transaction {
            SchemaUtils.drop(PackagesTable)
        }
    }

    @Test
    fun testInsertAndGetPackage() = runBlocking {
        val pkg = PackageData(
            name = "Super Fast",
            price = "15000",
            duration = "2h",
            unit = "kg"
        )

        val inserted = repository.insert(pkg)
        assertTrue(inserted)

        val all = repository.getAll()
        val retrieved = all.find { it.name == "Super Fast" }
        assertEquals("15000", retrieved?.price)
        assertEquals("2h", retrieved?.duration)
    }
}
