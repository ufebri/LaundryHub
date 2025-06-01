package com.raylabs.laundryhub.ui.history.state

import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.model.sheets.isPaidData
import com.raylabs.laundryhub.core.domain.model.sheets.paidDescription

data class HistoryItem(
    val orderId: String,
    val name: String,
    val formattedDate: String,
    val totalPrice: String,
    val packageType: String,
    val paymentStatus: String,
    val isPaid: Boolean
)

sealed interface HistoryUiItem {
    data class Header(val date: String) : HistoryUiItem
    data class Entry(val item: HistoryItem) : HistoryUiItem
}

fun TransactionData.toUiItem(): HistoryItem {
    return HistoryItem(
        orderId = this.orderID,
        name = this.name,
        formattedDate = this.date, // bisa diformat nanti
        totalPrice = this.totalPrice,
        packageType = this.packageType,
        paymentStatus = this.paidDescription(),
        isPaid = this.isPaidData()
    )
}

data class HistoryGroupedItem(
    val date: String, // "1 June 2025"
    val items: List<HistoryUiItem>
)

fun List<TransactionData>.toUiItems(): List<HistoryUiItem> {
    return this
        .groupBy { it.date }
        .flatMap { (date, items) ->
            listOf(HistoryUiItem.Header(date)) +
                    items.map { HistoryUiItem.Entry(it.toUiItem()) }
        }
}