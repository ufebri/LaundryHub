package com.raylabs.laundryhub.backend.service

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
            keySelector = com.raylabs.laundryhub.core.domain.model.sheets.OrderData::orderId,
            signatureSelector = com.raylabs.laundryhub.core.domain.model.sheets.OrderData::syncSignature,
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
) = com.raylabs.laundryhub.core.domain.model.sheets.OrderData(
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
