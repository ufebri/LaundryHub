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
            org.jetbrains.exposed.sql.transactions.TransactionManager.current().exec(
                "CREATE ALIAS IF NOT EXISTS pg_advisory_xact_lock AS 'long pgAdvisoryXactLock(long val) { return val; }'"
            )
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

    @Test
    fun `getGrossForMonth aggregates orders for requested month`() = runBlocking {
        repository.insert(sampleOrder("1", "lunas", "01/06/2026").copy(totalPrice = "Rp25.000"))
        repository.insert(sampleOrder("2", "belum", "06/06/2026").copy(totalPrice = "18000"))
        repository.insert(sampleOrder("3", "lunas", "31/05/2026").copy(totalPrice = "999000"))

        val gross = repository.getGrossForMonth(year = 2026, month = 6)

        assertEquals("Juni 2026", gross?.month)
        assertEquals("Rp43.000", gross?.totalNominal)
        assertEquals("2", gross?.orderCount)
        assertEquals("Rp215", gross?.tax)
    }

    @Test
    fun testInsertWithNextId() = runBlocking {
        val order = sampleOrder("0", "Unpaid", "10/05/2026")
        val created = repository.insertWithNextId(order)
        kotlin.test.assertNotNull(created)
        assertEquals("1", created.orderId)
        
        val second = repository.insertWithNextId(order)
        kotlin.test.assertNotNull(second)
        assertEquals("2", second.orderId)
    }

    @Test
    fun testUpdateOrder() = runBlocking {
        val order = sampleOrder("1", "Unpaid", "10/05/2026")
        repository.insert(order)
        
        val updated = order.copy(name = "Updated User")
        val success = repository.update("1", updated)
        assertTrue(success)
        
        val retrieved = repository.getById("1")
        assertEquals("Updated User", retrieved?.name)
    }

    @Test
    fun testUpsertOrder() = runBlocking {
        val order = sampleOrder("1", "Unpaid", "10/05/2026")
        
        val successInsert = repository.upsert(order)
        assertTrue(successInsert)
        
        val updated = order.copy(name = "Updated User")
        val successUpdate = repository.upsert(updated)
        assertTrue(successUpdate)
        
        val retrieved = repository.getById("1")
        assertEquals("Updated User", retrieved?.name)
    }

    @Test
    fun testDeleteOrder() = runBlocking {
        val order = sampleOrder("1", "Unpaid", "10/05/2026")
        repository.insert(order)
        
        val success = repository.delete("1")
        assertTrue(success)
        
        val retrieved = repository.getById("1")
        assertEquals(null, retrieved)
    }

    @Test
    fun testGetUnsyncedAndMarkSynced() = runBlocking {
        val order = sampleOrder("1", "Unpaid", "10/05/2026")
        repository.insert(order)
        
        val unsynced = repository.getUnsyncedOrders()
        assertEquals(1, unsynced.size)
        
        val success = repository.markAsSynced(listOf("1"))
        assertTrue(success)
        
        assertEquals(0, repository.getUnsyncedOrders().size)
    }

    @Test
    fun testGetLatestIdAndNextId() = runBlocking {
        assertEquals("0", repository.getLatestId())
        assertEquals("1", repository.getNextId())
        
        repository.insert(sampleOrder("5", "Unpaid", "10/05/2026"))
        assertEquals("5", repository.getLatestId())
        assertEquals("6", repository.getNextId())
    }

    @Test
    fun testInsertAll() = runBlocking {
        val orders = listOf(
            sampleOrder("1", "Unpaid", "10/05/2026"),
            sampleOrder("2", "Unpaid", "11/05/2026")
        )
        val count = repository.insertAll(orders)
        assertEquals(2, count)
    }

    @Test
    fun testGetAllWithDateRangeAndSort() = runBlocking {
        repository.insert(sampleOrder("1", "Unpaid", "10 May 2026"))
        repository.insert(sampleOrder("2", "Paid", "12/05/2026"))
        
        val inRange = repository.getAll(startDate = "09 May 2026", endDate = "11 May 2026")
        assertEquals(listOf("1"), inRange.map { it.orderId })
        
        val todayOrders = repository.getAll(filter = "TODAY")
        
        val sortDuedate = repository.getAll(sort = "DUE_DATE_ASC", startDate = "01 May 2026")
    }
}
