package com.raylabs.laundryhub.ui.order

import com.raylabs.laundryhub.core.domain.model.sheets.OrderData
import com.raylabs.laundryhub.core.domain.model.sheets.PackageData
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadPackageUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.income.GetLastOrderIdUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.income.GetOrderUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.income.SubmitOrderUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.income.UpdateOrderUseCase
import com.raylabs.laundryhub.ui.common.util.DateUtil
import com.raylabs.laundryhub.ui.common.util.Resource
import com.raylabs.laundryhub.ui.profile.inventory.state.PackageItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class OrderViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mockGetLastOrderIdUseCase: GetLastOrderIdUseCase = mock()
    private val mockSubmitOrderUseCase: SubmitOrderUseCase = mock()
    private val mockPackageListUseCase: ReadPackageUseCase = mock()
    private val mockGetOrderByIdUseCase: GetOrderUseCase = mock()
    private val mockUpdateOrderUseCase: UpdateOrderUseCase = mock()

    private lateinit var vm: OrderViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init triggers fetchLastOrderId and getPackageList`() = runTest {
        whenever(mockGetLastOrderIdUseCase.invoke()).thenReturn(Resource.Success("ORD-123"))
        whenever(mockPackageListUseCase.invoke()).thenReturn(
            Resource.Success(
                listOf(
                    PackageData(
                        "Reguler", "5000", "3d", unit = "kg"
                    )
                )
            )
        )
        vm = OrderViewModel(
            mockGetLastOrderIdUseCase,
            mockSubmitOrderUseCase,
            mockPackageListUseCase,
            mockGetOrderByIdUseCase,
            mockUpdateOrderUseCase
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("ORD-123", state.lastOrderId)
        assertFalse(state.packageNameList.isLoading)
        assertEquals(1, state.packageNameList.data?.size)
    }

    @Test
    fun `onOrderEditClick success loads order and triggers onSuccess`() = runTest {
        val transaction = TransactionData(
            orderID = "ORD-1",
            name = "Budi",
            phoneNumber = "0812",
            totalPrice = "5000",
            packageType = "Reguler",
            date = "2025-08-04",
            weight = "2",
            paymentMethod = "Cash",
            pricePerKg = "2500",
            paymentStatus = "Paid",
            remark = "Catatan",
            dueDate = "2025-08-05"
        )
        whenever(mockGetOrderByIdUseCase.invoke(orderID = "ORD-1")).thenReturn(
            Resource.Success(
                transaction
            )
        )
        whenever(mockPackageListUseCase.invoke()).thenReturn(Resource.Success(listOf()))
        vm = OrderViewModel(
            mockGetLastOrderIdUseCase,
            mockSubmitOrderUseCase,
            mockPackageListUseCase,
            mockGetOrderByIdUseCase,
            mockUpdateOrderUseCase
        )
        var successCalled = false
        vm.onOrderEditClick("ORD-1") { successCalled = true }
        testDispatcher.scheduler.advanceUntilIdle()
        val state = vm.uiState.value
        assertTrue(state.isEditMode)
        assertEquals("Budi", state.name)
        assertTrue(successCalled)
    }

    @Test
    fun `onOrderEditClick error sets error state`() = runTest {
        whenever(mockGetOrderByIdUseCase.invoke(orderID = "ORD-1")).thenReturn(Resource.Error("not found"))
        vm = OrderViewModel(
            mockGetLastOrderIdUseCase,
            mockSubmitOrderUseCase,
            mockPackageListUseCase,
            mockGetOrderByIdUseCase,
            mockUpdateOrderUseCase
        )
        vm.onOrderEditClick("ORD-1") { }
        testDispatcher.scheduler.advanceUntilIdle()
        val state = vm.uiState.value
        assertFalse(state.isEditMode)
        assertEquals("not found", state.editOrder.errorMessage)
    }

    @Test
    fun `updateOrder success triggers onComplete and updates state`() = runTest {
        val order = OrderData(
            orderId = "ORD2",
            name = "Ayu",
            phoneNumber = "0821",
            packageName = "Express",
            priceKg = "10000",
            totalPrice = "20000",
            paidStatus = "Paid",
            paymentMethod = "QR",
            remark = "Ok",
            weight = "2",
            orderDate = "2025-08-01",
            dueDate = "2025-08-05"
        )
        whenever(mockUpdateOrderUseCase.invoke(order = order)).thenReturn(Resource.Success(true))
        vm = OrderViewModel(
            mockGetLastOrderIdUseCase,
            mockSubmitOrderUseCase,
            mockPackageListUseCase,
            mockGetOrderByIdUseCase,
            mockUpdateOrderUseCase
        )
        var onCompleteCalled = false
        vm.updateOrder(order) { onCompleteCalled = true }
        testDispatcher.scheduler.advanceUntilIdle()
        val state = vm.uiState.value
        assertFalse(state.isSubmitting)
        assertTrue(onCompleteCalled)
        assertTrue(state.updateOrder.data == true)
    }

    @Test
    fun `updateField updates fields correctly`() {
        vm = OrderViewModel(
            mockGetLastOrderIdUseCase,
            mockSubmitOrderUseCase,
            mockPackageListUseCase,
            mockGetOrderByIdUseCase,
            mockUpdateOrderUseCase
        )
        vm.updateField("name", "Budi")
        assertEquals("Budi", vm.uiState.value.name)
        vm.updateField("paymentMethod", "QRIS")
        assertEquals("QRIS", vm.uiState.value.paymentMethod)
        vm.updateField("note", "Catatan A")
        assertEquals("Catatan A", vm.uiState.value.note)
    }

    @Test
    fun `onPhoneChanged trims leading zero`() {
        vm = OrderViewModel(
            mockGetLastOrderIdUseCase,
            mockSubmitOrderUseCase,
            mockPackageListUseCase,
            mockGetOrderByIdUseCase,
            mockUpdateOrderUseCase
        )
        vm.onPhoneChanged("08123")
        assertEquals("8123", vm.uiState.value.phone)
        vm.onPhoneChanged("628123")
        assertEquals("628123", vm.uiState.value.phone)
    }

    @Test
    fun `onPackageSelected updates selectedPackage and weight`() {
        vm = OrderViewModel(
            mockGetLastOrderIdUseCase,
            mockSubmitOrderUseCase,
            mockPackageListUseCase,
            mockGetOrderByIdUseCase,
            mockUpdateOrderUseCase
        )
        vm.onPriceChanged("10000")
        val packageItem = PackageItem("Express", "10000", "6h")
        vm.onPackageSelected(packageItem)
        assertEquals(packageItem, vm.uiState.value.selectedPackage)
        assertEquals("1", vm.uiState.value.weight)
    }

    @Test
    fun `onPriceChanged updates price and recalculates weight`() {
        vm = OrderViewModel(
            mockGetLastOrderIdUseCase,
            mockSubmitOrderUseCase,
            mockPackageListUseCase,
            mockGetOrderByIdUseCase,
            mockUpdateOrderUseCase
        )
        val packageItem = PackageItem("Express", "10000", "6h")
        vm.onPackageSelected(packageItem)
        vm.onPriceChanged("20000")
        assertEquals("20000", vm.uiState.value.price)
        assertEquals("2", vm.uiState.value.weight)
    }

    @Test
    fun `resetForm resets state`() {
        vm = OrderViewModel(
            mockGetLastOrderIdUseCase,
            mockSubmitOrderUseCase,
            mockPackageListUseCase,
            mockGetOrderByIdUseCase,
            mockUpdateOrderUseCase
        )
        vm.updateField("name", "Budi")
        vm.resetForm()
        val state = vm.uiState.value
        assertEquals("", state.name)
        assertFalse(state.isEditMode)
    }

    @Test
    fun `fetchLastOrderId error sets fallback text`() = runTest {
        whenever(mockGetLastOrderIdUseCase.invoke()).thenReturn(Resource.Error("x"))
        whenever(mockPackageListUseCase.invoke()).thenReturn(Resource.Success(emptyList()))
        vm = OrderViewModel(
            mockGetLastOrderIdUseCase,
            mockSubmitOrderUseCase,
            mockPackageListUseCase,
            mockGetOrderByIdUseCase,
            mockUpdateOrderUseCase
        )
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(null, vm.uiState.value.lastOrderId)
        assertEquals("x", vm.uiState.value.lastOrderIdError)
    }

    @Test
    fun `resolveLastOrderIdForSubmit returns id and marks submitting`() = runTest {
        whenever(mockGetLastOrderIdUseCase.invoke()).thenReturn(
            Resource.Success("ORD-1"),
            Resource.Success("ORD-2")
        )
        whenever(mockPackageListUseCase.invoke()).thenReturn(Resource.Success(emptyList()))
        vm = OrderViewModel(
            mockGetLastOrderIdUseCase,
            mockSubmitOrderUseCase,
            mockPackageListUseCase,
            mockGetOrderByIdUseCase,
            mockUpdateOrderUseCase
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val result = vm.resolveLastOrderIdForSubmit()

        assertEquals("ORD-2", result)
        assertEquals("ORD-2", vm.uiState.value.lastOrderId)
        assertEquals(null, vm.uiState.value.lastOrderIdError)
        assertTrue(vm.uiState.value.isSubmitting)
    }

    @Test
    fun `resolveLastOrderIdForSubmit error clears id and stops submitting`() = runTest {
        whenever(mockGetLastOrderIdUseCase.invoke()).thenReturn(
            Resource.Success("ORD-1"),
            Resource.Error("fail")
        )
        whenever(mockPackageListUseCase.invoke()).thenReturn(Resource.Success(emptyList()))
        vm = OrderViewModel(
            mockGetLastOrderIdUseCase,
            mockSubmitOrderUseCase,
            mockPackageListUseCase,
            mockGetOrderByIdUseCase,
            mockUpdateOrderUseCase
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val result = vm.resolveLastOrderIdForSubmit()

        assertEquals(null, result)
        assertEquals(null, vm.uiState.value.lastOrderId)
        assertEquals("fail", vm.uiState.value.lastOrderIdError)
        assertFalse(vm.uiState.value.isSubmitting)
    }

    @Test
    fun `getPackageList error sets errorMessage`() = runTest {
        whenever(mockGetLastOrderIdUseCase.invoke()).thenReturn(Resource.Success("ORD-1"))
        whenever(mockPackageListUseCase.invoke()).thenReturn(Resource.Error("pkg err"))
        vm = OrderViewModel(
            mockGetLastOrderIdUseCase,
            mockSubmitOrderUseCase,
            mockPackageListUseCase,
            mockGetOrderByIdUseCase,
            mockUpdateOrderUseCase
        )
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("pkg err", vm.uiState.value.packageNameList.errorMessage)
    }

    @Test
    fun `getPackageList selects matched package in edit mode`() = runTest {
        // masuk edit mode
        val trx = TransactionData(
            orderID = "1",
            name = "A",
            phoneNumber = "08",
            totalPrice = "10,000",
            packageType = "Express",
            date = "",
            weight = "",
            paymentMethod = "",
            pricePerKg = "",
            paymentStatus = "",
            remark = "",
            dueDate = ""
        )
        whenever(mockGetOrderByIdUseCase.invoke(orderID = "1")).thenReturn(Resource.Success(trx))
        whenever(mockPackageListUseCase.invoke()).thenReturn(
            Resource.Success(
                listOf(
                    PackageData(
                        "5000", "Express", "3d", "kg"
                    ),
                    PackageData(
                        "10000", "Express", "6h", "kg"
                    ),
                )
            )
        )
        vm = OrderViewModel(
            mockGetLastOrderIdUseCase,
            mockSubmitOrderUseCase,
            mockPackageListUseCase,
            mockGetOrderByIdUseCase,
            mockUpdateOrderUseCase
        )
        testDispatcher.scheduler.advanceUntilIdle()
        vm.onOrderEditClick("1") {}; testDispatcher.scheduler.advanceUntilIdle()
        val st = vm.uiState.value
        assertTrue(st.isEditMode)
        assertEquals("Express", st.selectedPackage?.name)
        assertEquals("1", st.orderID) // 10,000 / 10,000
    }

    @Test
    fun `onOrderEditClick empty sets specific error`() = runTest {
        whenever(mockGetOrderByIdUseCase.invoke(orderID = "X")).thenReturn(Resource.Empty)
        whenever(mockPackageListUseCase.invoke()).thenReturn(Resource.Success(emptyList()))
        vm = OrderViewModel(
            mockGetLastOrderIdUseCase,
            mockSubmitOrderUseCase,
            mockPackageListUseCase,
            mockGetOrderByIdUseCase,
            mockUpdateOrderUseCase
        )
        vm.onOrderEditClick("X") {}; testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.isEditMode)
        assertEquals("No data found for order ID: X", vm.uiState.value.editOrder.errorMessage)
    }

    @Test
    fun `onOrderEditClick loading keeps editOrder loading`() = runTest {
        whenever(mockGetOrderByIdUseCase.invoke(orderID = "X")).thenReturn(Resource.Loading)
        whenever(mockPackageListUseCase.invoke()).thenReturn(Resource.Success(emptyList()))
        vm = OrderViewModel(
            mockGetLastOrderIdUseCase,
            mockSubmitOrderUseCase,
            mockPackageListUseCase,
            mockGetOrderByIdUseCase,
            mockUpdateOrderUseCase
        )
        vm.onOrderEditClick("X") {} // no advance to keep at loading if needed
        assertTrue(vm.uiState.value.editOrder.isLoading)
        assertFalse(vm.uiState.value.isEditMode)
    }

    @Test
    fun `submitOrder success updates state and calls onComplete`() = runTest {
        val order = OrderData(
            orderId = "1",
            name = "A",
            phoneNumber = "8",
            packageName = "Express",
            priceKg = "10000",
            totalPrice = "10000",
            paidStatus = "Paid",
            paymentMethod = "Cash",
            remark = "-",
            weight = "1",
            orderDate = "21/08/2025",
            dueDate = ""
        )
        whenever(mockSubmitOrderUseCase.invoke(order = order)).thenReturn(Resource.Success(true))
        whenever(mockPackageListUseCase.invoke()).thenReturn(Resource.Success(emptyList()))
        vm = OrderViewModel(
            mockGetLastOrderIdUseCase,
            mockSubmitOrderUseCase,
            mockPackageListUseCase,
            mockGetOrderByIdUseCase,
            mockUpdateOrderUseCase
        )
        var done = false
        vm.submitOrder(order) { done = true }
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.isSubmitting)
        assertTrue(done)
        assertTrue(vm.uiState.value.submitNewOrder.data == true)
    }

    @Test
    fun `submitOrder error sets error without calling onComplete`() = runTest {
        val order = OrderData(
            orderId = "1",
            name = "A",
            phoneNumber = "8",
            packageName = "Express",
            priceKg = "10000",
            totalPrice = "10000",
            paidStatus = "Paid",
            paymentMethod = "Cash",
            remark = "-",
            weight = "1",
            orderDate = "21/08/2025",
            dueDate = ""
        )
        whenever(mockSubmitOrderUseCase.invoke(order = order)).thenReturn(Resource.Error("submit err"))
        whenever(mockPackageListUseCase.invoke()).thenReturn(Resource.Success(emptyList()))
        vm = OrderViewModel(
            mockGetLastOrderIdUseCase,
            mockSubmitOrderUseCase,
            mockPackageListUseCase,
            mockGetOrderByIdUseCase,
            mockUpdateOrderUseCase
        )
        var done = false
        vm.submitOrder(order) { done = true }
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.isSubmitting)
        assertEquals("submit err", vm.uiState.value.submitNewOrder.errorMessage)
        assertFalse(done)
    }

    @Test
    fun `submitOrder else branch clears isSubmitting`() = runTest {
        val order = OrderData(
            orderId = "1",
            name = "A",
            phoneNumber = "8",
            packageName = "Express",
            priceKg = "10000",
            totalPrice = "10000",
            paidStatus = "Paid",
            paymentMethod = "Cash",
            remark = "-",
            weight = "1",
            orderDate = "21/08/2025",
            dueDate = ""
        )
        whenever(mockSubmitOrderUseCase.invoke(order = order)).thenReturn(Resource.Loading)
        whenever(mockPackageListUseCase.invoke()).thenReturn(Resource.Success(emptyList()))
        vm = OrderViewModel(
            mockGetLastOrderIdUseCase,
            mockSubmitOrderUseCase,
            mockPackageListUseCase,
            mockGetOrderByIdUseCase,
            mockUpdateOrderUseCase
        )
        var done = false
        vm.submitOrder(order) { done = true }
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.isSubmitting)
        assertFalse(done)
    }

    @Test
    fun `updateOrder error sets error and stop submitting`() = runTest {
        val order = OrderData(
            orderId = "1",
            name = "A",
            phoneNumber = "8",
            packageName = "Express",
            priceKg = "10000",
            totalPrice = "10000",
            paidStatus = "Paid",
            paymentMethod = "Cash",
            remark = "-",
            weight = "1",
            orderDate = "21/08/2025",
            dueDate = ""
        )
        whenever(mockUpdateOrderUseCase.invoke(order = order)).thenReturn(Resource.Error("upd err"))
        whenever(mockPackageListUseCase.invoke()).thenReturn(Resource.Success(emptyList()))
        vm = OrderViewModel(
            mockGetLastOrderIdUseCase,
            mockSubmitOrderUseCase,
            mockPackageListUseCase,
            mockGetOrderByIdUseCase,
            mockUpdateOrderUseCase
        )
        var done = false
        vm.updateOrder(order) { done = true }; testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.isSubmitting)
        assertEquals("upd err", vm.uiState.value.updateOrder.errorMessage)
        assertFalse(done)
    }

    @Test
    fun `updateField unknown key does not change state`() {
        vm = OrderViewModel(
            mockGetLastOrderIdUseCase,
            mockSubmitOrderUseCase,
            mockPackageListUseCase,
            mockGetOrderByIdUseCase,
            mockUpdateOrderUseCase
        )
        val before = vm.uiState.value
        vm.updateField("unknown", "x")
        assertEquals(before, vm.uiState.value)
    }

    @Test
    fun `recalculateWeight returns empty when minPrice is zero`() {
        vm = OrderViewModel(
            mockGetLastOrderIdUseCase,
            mockSubmitOrderUseCase,
            mockPackageListUseCase,
            mockGetOrderByIdUseCase,
            mockUpdateOrderUseCase
        )
        vm.onPackageSelected(PackageItem("Zero", "0", "6h"))
        vm.onPriceChanged("10000")
        assertEquals("", vm.uiState.value.weight)
    }

    @Test
    fun `onOrderDateSelected updates date and recalculates due date`() = runTest {
        whenever(mockGetLastOrderIdUseCase.invoke()).thenReturn(Resource.Success("ORD-10"))
        whenever(mockPackageListUseCase.invoke()).thenReturn(
            Resource.Success(
                listOf(PackageData(name = "Express", price = "10000", duration = "3d", unit = "kg"))
            )
        )
        vm = OrderViewModel(
            mockGetLastOrderIdUseCase,
            mockSubmitOrderUseCase,
            mockPackageListUseCase,
            mockGetOrderByIdUseCase,
            mockUpdateOrderUseCase
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val packageItem = PackageItem(name = "Express", price = "10000", work = "3d")
        vm.onPackageSelected(packageItem)

        vm.onOrderDateSelected("02/09/2025")
        val state = vm.uiState.value
        assertEquals("02/09/2025", state.orderDate)
        val expectedDue = DateUtil.getDueDate("3d", "02-09-2025 08:00")
        assertEquals(expectedDue, state.dueDate)
    }
}
