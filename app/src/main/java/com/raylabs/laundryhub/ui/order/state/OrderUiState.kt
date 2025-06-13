package com.raylabs.laundryhub.ui.order.state

import com.raylabs.laundryhub.core.domain.model.sheets.OrderData
import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.inventory.state.PackageItem

data class OrderUiState(
    val lastOrderId: String? = null,
    val submit: SectionState<Boolean> = SectionState(),
    val packageNameList: SectionState<List<PackageItem>> = SectionState(),
    val paymentOption: List<String> = listOf("Paid by Cash", "Paid by QRIS", "Unpaid"),

    val name: String = "",
    val phone: String = "",
    val selectedPackage: PackageItem? = null,
    val price: String = "",
    val weight: String = "",
    val paymentMethod: String = "",
    val note: String = "",
)

fun OrderUiState.toOrderData(orderId: String): OrderData {
    return OrderData(
        orderId = orderId,
        name = this.name,
        phoneNumber = this.phone,
        packageName = this.selectedPackage?.name ?: "",
        priceKg = this.selectedPackage?.price ?: "",
        paymentMethod = this.paymentMethod,
        remark = this.note,
        totalPrice = this.price,
        paidStatus = this.paymentMethod
    )
}