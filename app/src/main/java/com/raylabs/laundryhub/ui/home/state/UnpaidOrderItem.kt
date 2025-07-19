package com.raylabs.laundryhub.ui.home.state

import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData

data class UnpaidOrderItem(
    val orderID: String,
    val customerName: String,
    val packageType: String,
    val nowStatus: String,
    val dueDate: String
)

fun List<TransactionData>.toUi(): List<UnpaidOrderItem> {
    return this.map {
        UnpaidOrderItem(
            orderID = it.orderID,
            customerName = it.name,
            packageType = it.packageType,
            nowStatus = it.paymentStatus,
            dueDate = it.dueDate
        )
    }
}