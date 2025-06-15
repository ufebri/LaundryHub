package com.raylabs.laundryhub.ui.history.state

import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.model.sheets.isPaidData
import com.raylabs.laundryhub.core.domain.model.sheets.paidDescription
import com.raylabs.laundryhub.ui.common.util.DateUtil

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
        orderId = orderID,
        name = name,
        formattedDate = date, // bisa diformat nanti
        totalPrice = totalPrice,
        packageType = packageType,
        paymentStatus = paidDescription(),
        isPaid = isPaidData()
    )
}

fun List<TransactionData>.toUiItems(): List<HistoryUiItem> {
    return this
        .groupBy { it.date }
        .flatMap { (date, items) ->
            listOf(HistoryUiItem.Header(date)) +
                    items.map { HistoryUiItem.Entry(it.toUiItem()) }
        }
}