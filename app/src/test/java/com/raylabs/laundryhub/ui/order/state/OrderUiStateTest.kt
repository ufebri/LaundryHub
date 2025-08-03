package com.raylabs.laundryhub.ui.order.state

import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.ui.inventory.state.PackageItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderUiStateTest {

    @Test
    fun `toOrderData maps all fields correctly`() {
        val pkg = PackageItem(name = "Express", price = "10000", work = "6h")
        val state = OrderUiState(
            name = "Nina",
            phone = "0812",
            selectedPackage = pkg,
            price = "20000",
            paymentMethod = "Cash",
            note = "Segera",
            weight = "2",
            dueDate = "10/08/2025"
        )
        val order = state.toOrderData("ORDER99")
        assertEquals("ORDER99", order.orderId)
        assertEquals("Nina", order.name)
        assertEquals("0812", order.phoneNumber)
        assertEquals("Express", order.packageName)
        assertEquals("10000", order.priceKg)
        assertEquals("20000", order.totalPrice)
        assertEquals("Cash", order.paymentMethod)
        assertEquals("Segera", order.remark)
        assertEquals("2", order.weight)
        assertEquals("10/08/2025", order.dueDate)
    }

    @Test
    fun `TransactionData toUI maps to OrderData correctly`() {
        val trx = TransactionData(
            orderID = "ORD111",
            name = "Ray",
            date = "2025-08-01",
            totalPrice = "9000",
            packageType = "Reguler",
            paymentStatus = "Paid",
            paymentMethod = "Cash",
            phoneNumber = "0856",
            remark = "Ok",
            weight = "3",
            pricePerKg = "3000",
            dueDate = "2025-08-05"
        )
        val order = trx.toUI()
        assertEquals("ORD111", order.orderId)
        assertEquals("Ray", order.name)
        assertEquals("0856", order.phoneNumber)
        assertEquals("Reguler", order.packageName)
        assertEquals("3000", order.priceKg)
        assertEquals("9000", order.totalPrice)
        assertEquals("Paid", order.paidStatus)
        assertEquals("Cash", order.paymentMethod)
        assertEquals("Ok", order.remark)
        assertEquals("3", order.weight)
        assertEquals("2025-08-05", order.dueDate)
    }

    @Test
    fun `isSubmitEnabled is true only if all required fields are filled`() {
        val pkg = PackageItem(name = "Express", price = "10000", work = "6h")
        val valid = OrderUiState(
            name = "Sari",
            phone = "0812",
            selectedPackage = pkg,
            price = "10000",
            paymentMethod = "Cash"
        )
        assertTrue(valid.isSubmitEnabled)

        val invalid = valid.copy(phone = "")
        assertFalse(invalid.isSubmitEnabled)
    }

    @Test
    fun `isUpdateEnabled true if all fields including orderID are filled`() {
        val pkg = PackageItem(name = "Express", price = "10000", work = "6h")
        val valid = OrderUiState(
            orderID = "ORD1",
            name = "Sari",
            selectedPackage = pkg,
            price = "10000",
            paymentMethod = "Cash"
        )
        assertTrue(valid.isUpdateEnabled)

        val invalid = valid.copy(orderID = "")
        assertFalse(invalid.isUpdateEnabled)
    }
}