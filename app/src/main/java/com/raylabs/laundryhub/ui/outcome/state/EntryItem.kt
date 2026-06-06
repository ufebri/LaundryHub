package com.raylabs.laundryhub.ui.outcome.state

import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.core.domain.model.sheets.paidDescription
import com.raylabs.laundryhub.ui.common.util.DateUtil
import com.raylabs.laundryhub.ui.common.util.TextUtil.toRupiahFormat

import com.raylabs.laundryhub.ui.home.state.SyncStatus

data class EntryItem(
    val id: String,
    val name: String,
    val date: String,
    val price: String,
    val remark: String,
    val paymentStatus: String,
    val typeCard: TypeCard,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val rawPayload: OutcomeData? = null
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
    price = price.toRupiahFormat(),
    remark = remark,
    paymentStatus = paidDescription(),
    typeCard = TypeCard.OUTCOME,
    rawPayload = this
)

fun List<OutcomeData>.toDateListUiItems(): List<DateListItemUI> {
    return this
        .sortedWith(
            compareByDescending<OutcomeData> { DateUtil.parseSupportedAppDate(it.date)?.time ?: Long.MIN_VALUE }
                .thenByDescending { it.id.toIntOrNull() ?: Int.MIN_VALUE }
        )
        .groupBy { it.date }
        .flatMap { (date, items) ->
            listOf(DateListItemUI.Header(date)) +
                    items.map { DateListItemUI.Entry(it.toEntryItemUI()) }
                        .sortedByDescending { it.item.id }
        }
}
