package com.raylabs.laundryhub.ui.order.state

import com.raylabs.laundryhub.ui.profile.inventory.state.PackageItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderUiStateTest {

    @Test
    fun `toOrderData maps fields and computes due date when missing`() {
        val pkg = PackageItem(name = "Regular", price = "Rp5000", work = "3d")
        val state = OrderUiState(
            name = "Alice",
            phone = "8123",
            selectedPackage = pkg,
            price = "15000",
            paymentMethod = "Unpaid",
            orderDate = "01/01/2024",
            dueDate = ""
        )

        val orderData = state.toOrderData(orderId = "1")

        assertEquals("Alice", orderData.name)
        assertEquals("8123", orderData.phoneNumber)
        assertEquals("Regular", orderData.packageName)
        assertEquals("Rp5000", orderData.priceKg)
        assertEquals("15000", orderData.totalPrice)
        assertEquals("Unpaid", orderData.paidStatus)
        assertEquals("01/01/2024", orderData.orderDate)
        // With 3d duration, due date should be 3 days after orderDate
        assertEquals("04/01/2024", orderData.dueDate)
    }

    @Test
    fun `toOrderData uses provided due date when present`() {
        val state = OrderUiState(
            name = "Alice",
            phone = "8123",
            selectedPackage = null,
            price = "15000",
            paymentMethod = "Unpaid",
            orderDate = "",
            dueDate = "10/10/2025"
        )

        val orderData = state.toOrderData(orderId = "2")

        assertEquals("10/10/2025", orderData.dueDate)
        // orderDate normalized to today when blank
        assertEquals(
            com.raylabs.laundryhub.ui.common.util.DateUtil.getTodayDate("dd/MM/yyyy"),
            orderData.orderDate
        )
    }

    @Test
    fun `isSubmitEnabled true when required fields filled`() {
        val state = OrderUiState(
            name = "Bob",
            selectedPackage = PackageItem("Pkg", "Rp5", "1d"),
            price = "100",
            paymentMethod = "Paid by Cash"
        )

        assertTrue(state.isSubmitEnabled)
    }

    @Test
    fun `isSubmitEnabled false when missing required field`() {
        val state = OrderUiState(
            name = "",
            selectedPackage = PackageItem("Pkg", "Rp5", "1d"),
            price = "100",
            paymentMethod = "Paid by Cash"
        )

        assertFalse(state.isSubmitEnabled)
    }

    @Test
    fun `isUpdateEnabled true when all fields and id filled`() {
        val state = OrderUiState(
            orderID = "123",
            name = "Bob",
            selectedPackage = PackageItem("Pkg", "Rp5", "1d"),
            price = "100",
            paymentMethod = "Paid by Cash"
        )

        assertTrue(state.isUpdateEnabled)
    }

    @Test
    fun `isUpdateEnabled false when id missing`() {
        val state = OrderUiState(
            orderID = "",
            name = "Bob",
            selectedPackage = PackageItem("Pkg", "Rp5", "1d"),
            price = "100",
            paymentMethod = "Paid by Cash"
        )

        assertFalse(state.isUpdateEnabled)
    }
}
