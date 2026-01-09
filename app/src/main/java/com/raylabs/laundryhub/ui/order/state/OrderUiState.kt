package com.raylabs.laundryhub.ui.order.state

import com.raylabs.laundryhub.core.domain.model.sheets.OrderData
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.model.sheets.paymentMethodList
import com.raylabs.laundryhub.ui.common.util.DateUtil
import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.profile.inventory.state.PackageItem

data class OrderUiState(
    val lastOrderId: String? = null,
    val lastOrderIdError: String? = null,
    val showWhatsAppOption: Boolean = true,
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
    val orderDate: String = DateUtil.getTodayDate(DateUtil.STANDARD_DATE_FORMATED),
    val dueDate: String? = "",
    val isSubmitting: Boolean = false,
)

fun OrderUiState.toOrderData(orderId: String): OrderData {
    val normalizedOrderDate = orderDate.ifBlank { DateUtil.getTodayDate("dd/MM/yyyy") }
    val packageDuration = selectedPackage?.work
    val computedDueDate = when {
        !dueDate.isNullOrBlank() -> dueDate
        packageDuration.isNullOrBlank() -> ""
        else -> DateUtil.getDueDate(
            packageDuration,
            "${normalizedOrderDate.replace('/', '-')} 08:00"
        )
    }

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
        orderDate = normalizedOrderDate,
        dueDate = computedDueDate
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
        orderDate = date,
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
