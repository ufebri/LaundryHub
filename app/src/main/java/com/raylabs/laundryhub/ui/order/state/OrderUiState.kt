package com.raylabs.laundryhub.ui.order.state

import com.raylabs.laundryhub.core.domain.model.sheets.OrderData
import com.raylabs.laundryhub.core.domain.model.sheets.paymentMethodList
import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.inventory.state.PackageItem

data class OrderUiState(
    val lastOrderId: String? = null,
    val submit: SectionState<Boolean> = SectionState(),
    val packageNameList: SectionState<List<PackageItem>> = SectionState(),
    val paymentOption: List<String> = paymentMethodList,

    val name: String = "",
    val phone: String = "",
    val selectedPackage: PackageItem? = null,
    val rawPrice: String = "",
    val price: String = "",
    val weight: String = "",
    val paymentMethod: String = "",
    val note: String = "",
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
        dueDate = selectedPackage?.work ?: ""
    )
}

val OrderUiState.isSubmitEnabled: Boolean
    get() = name.isNotBlank()
            && phone.isNotBlank()
            && selectedPackage != null
            && price.isNotBlank()
            && paymentMethod.isNotBlank()