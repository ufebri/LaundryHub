package com.raylabs.laundryhub.backend.db.repository

import com.raylabs.laundryhub.backend.db.schema.OrdersTable
import com.raylabs.laundryhub.core.domain.model.sheets.OrderData
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrderRepositoryTest {

    private lateinit var repository: OrderRepository

    @BeforeTest
    fun setup() {
        Database.connect("jdbc:h2:mem:test;MODE=MySQL;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.create(OrdersTable)
        }
        repository = OrderRepository()
    }

    @AfterTest
    fun tearDown() {
        transaction {
            SchemaUtils.drop(OrdersTable)
        }
    }

    @Test
    fun testInsertAndGetOrder() = runBlocking {
        val order = OrderData(
            orderId = "ORD-001",
            name = "Test User",
            phoneNumber = "123456",
            packageName = "Reguler",
            priceKg = "3000",
            totalPrice = "9000",
            paidStatus = "Unpaid",
            paymentMethod = "Cash",
            remark = "",
            weight = "3",
            orderDate = "10/05/2026",
            dueDate = "13/05/2026"
        )

        val inserted = repository.insert(order)
        assertTrue(inserted)

        val retrieved = repository.getById("ORD-001")
        assertEquals("Test User", retrieved?.name)
        assertEquals("9000", retrieved?.totalPrice)
    }

    @Test
    fun testGetAllOrders() = runBlocking {
        val order1 = OrderData(orderId = "1", name = "User 1", orderDate = "10/05/2026", packageName = "P1", priceKg = "1", totalPrice = "1", paidStatus = "P", paymentMethod = "C", weight = "1", phoneNumber = "1", remark = "", dueDate = "1")
        val order2 = OrderData(orderId = "2", name = "User 2", orderDate = "10/05/2026", packageName = "P2", priceKg = "2", totalPrice = "2", paidStatus = "P", paymentMethod = "C", weight = "2", phoneNumber = "2", remark = "", dueDate = "2")
        
        repository.insert(order1)
        repository.insert(order2)

        val all = repository.getAll(page = 1, size = 10)
        assertEquals(2, all.size)
    }
}
