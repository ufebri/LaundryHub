package com.raylabs.laundryhub.backend.service

import com.raylabs.laundryhub.backend.util.syncVerificationSignature
import com.raylabs.laundryhub.core.domain.model.sheets.OrderData
import kotlin.test.Test
import kotlin.test.assertEquals

class SyncPreviewServiceTest {

    @Test
    fun `buildEntityPreview counts missing changed duplicate and pending rows`() {
        val preview = buildEntityPreview(
            entity = "Orders",
            sheetRows = listOf(
                TestRow("1", "same"),
                TestRow("2", "sheet"),
                TestRow("2", "duplicate"),
                TestRow("3", "sheet-only")
            ),
            databaseRows = listOf(
                TestRow("1", "same"),
                TestRow("2", "database"),
                TestRow("4", "database-only")
            ),
            pendingDeletes = 2,
            keySelector = TestRow::id,
            signatureSelector = TestRow::signature
        )

        assertEquals(1, preview.onlyInSheets)
        assertEquals(1, preview.onlyInDatabase)
        assertEquals(1, preview.changedRows)
        assertEquals(1, preview.duplicateKeys)
        assertEquals(2, preview.pendingDeletes)
        assertEquals(6, preview.totalDifferences)
        assertEquals(listOf("3"), preview.onlyInSheetKeys)
        assertEquals(listOf("4"), preview.onlyInDatabaseKeys)
        assertEquals(listOf("2"), preview.changedRowKeys)
        assertEquals(listOf("2"), preview.duplicateKeyValues)
        assertEquals(
            listOf("3", "4", "2"),
            preview.rowDifferences.map { it.key }
        )
    }

    @Test
    fun `buildEntityPreview flags header rows as suspicious differences`() {
        val preview = buildEntityPreview(
            entity = "Orders",
            sheetRows = listOf(TestRow("1", "same")),
            databaseRows = listOf(
                TestRow("1", "same"),
                TestRow("orderID", "header")
            ),
            pendingDeletes = 0,
            keySelector = TestRow::id,
            signatureSelector = TestRow::signature,
            suspiciousKeySelector = ::isOrderHeaderKey
        )

        assertEquals(1, preview.onlyInDatabase)
        assertEquals(1, preview.suspiciousRows)
        assertEquals(1, preview.totalDifferences)
    }

    @Test
    fun `order verification tolerates sheet formatting differences`() {
        val databaseOrder = testOrder(
            priceKg = "Rp10.000",
            totalPrice = "50000",
            paidStatus = "lunas",
            paymentMethod = "Cash"
        )
        val sheetOrder = testOrder(
            priceKg = "10.000",
            totalPrice = "50.000",
            paidStatus = "Paid",
            paymentMethod = "cash"
        )

        assertEquals(databaseOrder.syncVerificationSignature(), sheetOrder.syncVerificationSignature())
    }

    @Test
    fun `order preview returns changed field details with sheet and database values`() {
        val preview = buildEntityPreview(
            entity = "Orders",
            sheetRows = listOf(
                testOrder(
                    orderId = "1674",
                    priceKg = "Rp10.000",
                    totalPrice = "50.000",
                    paidStatus = "belum",
                    paymentMethod = "Unpaid"
                )
            ),
            databaseRows = listOf(
                testOrder(
                    orderId = "1674",
                    priceKg = "Rp10.000",
                    totalPrice = "50000",
                    paidStatus = "lunas",
                    paymentMethod = "cash"
                )
            ),
            pendingDeletes = 0,
            keySelector = OrderData::orderId,
            signatureSelector = OrderData::syncVerificationSignature,
            fieldDifferenceSelector = ::orderFieldDifferences,
            suspiciousKeySelector = ::isOrderHeaderKey
        )

        assertEquals(listOf("1674"), preview.changedRowKeys)
        assertEquals(1, preview.rowDifferences.size)
        assertEquals(listOf("paidStatus", "paymentMethod"), preview.rowDifferences.single().fieldDifferences.map { it.fieldName })
        assertEquals("belum", preview.rowDifferences.single().fieldDifferences.first().sheetValue)
        assertEquals("lunas", preview.rowDifferences.single().fieldDifferences.first().databaseValue)
    }

    @Test
    fun `order verification accepts common sheet number and paid status variants`() {
        val databaseOrder = testOrder(
            priceKg = "10000",
            totalPrice = "50000",
            paidStatus = "lunas",
            paymentMethod = "QRIS"
        )
        val equivalentSheetOrders = listOf(
            testOrder(priceKg = "10.000", totalPrice = "50.000", paidStatus = "Paid", paymentMethod = "qris"),
            testOrder(priceKg = "Rp10.000", totalPrice = "Rp50.000", paidStatus = "paid", paymentMethod = "Qris"),
            testOrder(priceKg = "10,000", totalPrice = "50,000", paidStatus = "LUNAS", paymentMethod = "QRIS")
        )

        equivalentSheetOrders.forEach { sheetOrder ->
            assertEquals(databaseOrder.syncVerificationSignature(), sheetOrder.syncVerificationSignature())
        }
    }

    @Test
    fun `order preview ignores sheet header rows inside data range`() {
        val preview = buildEntityPreview(
            entity = "Orders",
            sheetRows = listOf(
                testOrder(
                    orderId = "orderID",
                    priceKg = "priceKg",
                    totalPrice = "totalPrice",
                    paidStatus = "paidStatus",
                    paymentMethod = "paymentMethod"
                ),
                testOrder(
                    orderId = "1674",
                    priceKg = "Rp10.000",
                    totalPrice = "50000",
                    paidStatus = "lunas",
                    paymentMethod = "qris"
                )
            ).filterNot { isOrderHeaderKey(it.orderId) },
            databaseRows = listOf(
                testOrder(
                    orderId = "1674",
                    priceKg = "Rp10.000",
                    totalPrice = "50000",
                    paidStatus = "lunas",
                    paymentMethod = "qris"
                )
            ),
            pendingDeletes = 0,
            keySelector = OrderData::orderId,
            signatureSelector = OrderData::syncVerificationSignature,
            suspiciousKeySelector = ::isOrderHeaderKey
        )

        assertEquals(0, preview.onlyInSheets)
        assertEquals(0, preview.onlyInDatabase)
        assertEquals(0, preview.suspiciousRows)
    }
}

private data class TestRow(
    val id: String,
    val signature: String
)

private fun testOrder(
    orderId: String = "1674",
    priceKg: String,
    totalPrice: String,
    paidStatus: String,
    paymentMethod: String
) = OrderData(
    orderId = orderId,
    orderDate = "29/05/2026",
    name = "test order baru",
    phoneNumber = "",
    packageName = "Express - 6H",
    priceKg = priceKg,
    totalPrice = totalPrice,
    paidStatus = paidStatus,
    paymentMethod = paymentMethod,
    remark = "",
    weight = "5",
    dueDate = "29/05/2026"
)
