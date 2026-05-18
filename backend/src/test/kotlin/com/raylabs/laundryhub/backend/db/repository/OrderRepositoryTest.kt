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

    @Test
    fun `unpaid filter includes only normalized unpaid statuses`() = runBlocking {
        listOf(
            sampleOrder(orderId = "1", paidStatus = "Unpaid", orderDate = "15/05/2026"),
            sampleOrder(orderId = "2", paidStatus = "belum", orderDate = "14/05/2026"),
            sampleOrder(orderId = "3", paidStatus = "", orderDate = "13/05/2026"),
            sampleOrder(orderId = "4", paidStatus = "Paid", orderDate = "15/05/2026"),
            sampleOrder(orderId = "5", paidStatus = "lunas", orderDate = "14/05/2026"),
            sampleOrder(orderId = "6", paidStatus = "Paid by Cash", orderDate = "13/05/2026")
        ).forEach { repository.insert(it) }

        val unpaidIds = repository.getAll(filter = "UNPAID", page = 1, size = 10)
            .map { it.orderId }
            .toSet()
        val paidIds = repository.getAll(filter = "PAID", page = 1, size = 10)
            .map { it.orderId }
            .toSet()

        assertEquals(setOf("1", "2", "3"), unpaidIds)
        assertEquals(setOf("4", "5", "6"), paidIds)
    }

    @Test
    fun `order date sort accepts display month names from history data`() = runBlocking {
        listOf(
            sampleOrder(orderId = "1", paidStatus = "Unpaid", orderDate = "13/05/2026"),
            sampleOrder(orderId = "2", paidStatus = "Unpaid", orderDate = "14 May 2026"),
            sampleOrder(orderId = "3", paidStatus = "Unpaid", orderDate = "15 Mei 2026")
        ).forEach { repository.insert(it) }

        val sortedIds = repository.getAll(
            filter = "UNPAID",
            sort = "ORDER_DATE_DESC",
            page = 1,
            size = 10
        ).map { it.orderId }

        assertEquals(listOf("3", "2", "1"), sortedIds)
    }

    private fun sampleOrder(
        orderId: String,
        paidStatus: String,
        orderDate: String
    ) = OrderData(
        orderId = orderId,
        name = "User $orderId",
        phoneNumber = orderId,
        packageName = "Regular",
        priceKg = "7000",
        totalPrice = "14000",
        paidStatus = paidStatus,
        paymentMethod = "cash",
        remark = "",
        weight = "2",
        orderDate = orderDate,
        dueDate = orderDate
    )
}
