package com.raylabs.laundryhub.ui.order.state

import com.raylabs.laundryhub.core.domain.model.sheets.OrderData
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.model.sheets.paymentMethodList
import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.inventory.state.PackageItem

data class OrderUiState(
    val lastOrderId: String? = null,
    val isEditMode: Boolean = false,
    val submitNewOrder: SectionState<Boolean> = SectionState(),
    val submitHistoryOrder: SectionState<Boolean> = SectionState(),
    val packageNameList: SectionState<List<PackageItem>> = SectionState(),
    val paymentOption: List<String> = paymentMethodList,
    val updateOrder: SectionState<Boolean> = SectionState(),

    //For Edit
    val orderID: String = "",
    val tempSelectedPackageName: String? = null,
    val editOrder: SectionState<OrderData> = SectionState(),

    val name: String = "",
    val phone: String = "",
    val selectedPackage: PackageItem? = null,
    val rawPrice: String = "",
    val price: String = "",
    val weight: String = "",
    val paymentMethod: String = "",
    val note: String = "",
    val dueDate: String? = "",
    val isSubmitting: Boolean = false,
)

fun OrderUiState.toOrderData(orderId: String): OrderData {
    return OrderData(
        orderId = orderId,
        name = name,
        phoneNumber = phone,
        packageName = selectedPackage?.name ?: "",
        priceKg = selectedPackage?.price ?: "",
        paymentMethod = paymentMethod,
        remark = note,
        totalPrice = price,
        paidStatus = paymentMethod,
        weight = weight,
        dueDate = dueDate ?: selectedPackage?.work.orEmpty()
    )
}

fun TransactionData.toUI(): OrderData {
    return OrderData(
        orderId = orderID,
        name = name,
        phoneNumber = phoneNumber,
        packageName = packageType,
        priceKg = pricePerKg,
        totalPrice = totalPrice,
        paidStatus = paymentStatus,
        paymentMethod = paymentMethod,
        remark = remark,
        weight = weight,
        dueDate = dueDate
    )
}

val OrderUiState.isSubmitEnabled: Boolean
    get() = name.isNotBlank()
            && selectedPackage != null
            && price.isNotBlank()
            && paymentMethod.isNotBlank()

val OrderUiState.isUpdateEnabled: Boolean
    get() = orderID.isNotBlank()
            && name.isNotBlank()
            && selectedPackage != null
            && price.isNotBlank()
            && paymentMethod.isNotBlank()