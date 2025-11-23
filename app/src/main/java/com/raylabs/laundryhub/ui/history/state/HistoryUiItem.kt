package com.raylabs.laundryhub.ui.history.state

import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.model.sheets.paidDescription
import com.raylabs.laundryhub.ui.outcome.state.DateListItemUI
import com.raylabs.laundryhub.ui.outcome.state.EntryItem
import com.raylabs.laundryhub.ui.outcome.state.TypeCard

fun TransactionData.toUiItem(): EntryItem {
    return EntryItem(
        id = orderID,
        name = name,
        date = date, // bisa diformat nanti
        price = totalPrice,
        remark = packageType,
        paymentStatus = paidDescription(),
        typeCard = TypeCard.INCOME
    )
}

fun List<TransactionData>.toUiItems(): List<DateListItemUI> {
    return this
        .groupBy { it.date }
        .flatMap { (date, items) ->
            listOf(DateListItemUI.Header(date)) +
                    items.map { DateListItemUI.Entry(it.toUiItem()) }
                        .sortedByDescending { it.item.id }
        }
}