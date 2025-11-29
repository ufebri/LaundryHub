package com.raylabs.laundryhub.ui.outcome.state

import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.core.domain.model.sheets.paidDescription
import com.raylabs.laundryhub.ui.common.util.DateUtil

data class EntryItem(
    val id: String,
    val name: String,
    val date: String,
    val price: String,
    val remark: String,
    val paymentStatus: String,
    val typeCard: TypeCard
)

sealed interface DateListItemUI {
    data class Header(val date: String) : DateListItemUI
    data class Entry(val item: EntryItem) : DateListItemUI
}

enum class TypeCard {
    INCOME,
    OUTCOME
}

fun OutcomeData.toEntryItemUI(): EntryItem = EntryItem(
    id = id,
    name = purpose,
    date = DateUtil.formatToLongDate(date, inputFormat = DateUtil.STANDARD_DATE_FORMATED),
    price = price,
    remark = remark,
    paymentStatus = paidDescription(),
    typeCard = TypeCard.OUTCOME
)

fun List<OutcomeData>.toDateListUiItems(): List<DateListItemUI> {
    return this
        .groupBy { it.date }
        .flatMap { (date, items) ->
            listOf(DateListItemUI.Header(date)) +
                    items.map { DateListItemUI.Entry(it.toEntryItemUI()) }
                        .sortedByDescending { it.item.id }
        }
}
