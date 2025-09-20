package com.raylabs.laundryhub.ui.home.state

import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.model.sheets.paidDescription

data class UnpaidOrderItem(
    val orderID: String,
    val customerName: String,
    val packageType: String,
    val nowStatus: String,
    val dueDate: String,
    val orderDate: String // Added orderDate
)

fun List<TransactionData>.toUi(): List<UnpaidOrderItem> {
    return this.map {
        UnpaidOrderItem(
            orderID = it.orderID,
            customerName = it.name,
            packageType = it.packageType,
            nowStatus = it.paidDescription(),
            dueDate = it.dueDate,
            orderDate = it.date // Mapped from TransactionData.date
        )
    }
}