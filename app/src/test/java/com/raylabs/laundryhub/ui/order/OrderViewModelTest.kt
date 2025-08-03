package com.raylabs.laundryhub.ui.order

import com.raylabs.laundryhub.core.domain.model.sheets.OrderData
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.usecase.sheets.GetLastOrderIdUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.GetOrderUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadPackageUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.SubmitOrderUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.UpdateOrderUseCase
import com.raylabs.laundryhub.ui.common.util.Resource
import com.raylabs.laundryhub.ui.inventory.state.PackageItem
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
                    com.raylabs.laundryhub.core.domain.model.sheets.PackageData(
                        "Reguler",
                        "5000",
                        "3d",
                        unit = "kg"
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
            "ORD2",
            "Ayu",
            "0821",
            "Express",
            "10000",
            "20000",
            "Paid",
            "QR",
            "Ok",
            "2",
            "2025-08-05"
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
}