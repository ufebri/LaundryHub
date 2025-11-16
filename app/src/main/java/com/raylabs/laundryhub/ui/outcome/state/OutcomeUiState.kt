package com.raylabs.laundryhub.ui.outcome.state

import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.ui.common.util.DateUtil
import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.common.util.TextUtil.capitalizeFirstLetter
import com.raylabs.laundryhub.ui.common.util.TextUtil.toRupiahFormat

data class OutcomeUiState(
    val history: SectionState<List<OutcomeUiItem>> = SectionState(),
    val form: OutcomeFormState = OutcomeFormState(),
    val isAddSheetVisible: Boolean = false,
    val snackbarMessage: String? = null
)

data class OutcomeFormState(
    val id: String = "1",
    val date: String = DateUtil.getTodayDate("dd/MM/yyyy"),
    val purpose: String = "",
    val priceRaw: String = "",
    val payment: String = PAYMENT_OPTIONS.first(),
    val remark: String = "",
    val isSubmitting: Boolean = false
) {
    val formattedDate: String
        get() = if (date.isBlank()) "" else DateUtil.formatToLongDate(date, "dd/MM/yyyy")

    val priceDisplay: String
        get() = if (priceRaw.isBlank()) "" else priceRaw.toRupiahFormat()

    val priceForSheet: String
        get() = if (priceRaw.isBlank()) "" else "Rp${priceRaw.toRupiahFormat()}"

    val isValid: Boolean
        get() = purpose.isNotBlank() && priceRaw.isNotBlank() && payment.isNotBlank()
}

data class OutcomeHistoryItem(
    val id: String,
    val purpose: String,
    val remark: String,
    val paymentLabel: String,
    val price: String
)

sealed interface OutcomeUiItem {
    data class Header(val dateLabel: String) : OutcomeUiItem
    data class Entry(val item: OutcomeHistoryItem) : OutcomeUiItem
}

fun OutcomeData.toHistoryItem(): OutcomeHistoryItem {
    val paymentDesc =
        "Paid by ${payment.ifBlank { "Cash" }.capitalizeFirstLetter()}"
    return OutcomeHistoryItem(
        id = id,
        purpose = purpose,
        remark = remark,
        paymentLabel = paymentDesc,
        price = price.ifBlank { "Rp0" }
    )
}

fun List<OutcomeData>.toUiItems(): List<OutcomeUiItem> {
    if (isEmpty()) return emptyList()
    val sorted = sortedWith(
        compareByDescending<OutcomeData> {
            DateUtil.parseDate(it.date, "dd/MM/yyyy")?.time ?: 0L
        }.thenByDescending {
            it.id.toIntOrNull() ?: 0
        }
    )
    val items = mutableListOf<OutcomeUiItem>()
    var lastDate: String? = null
    sorted.forEach { data ->
        if (data.date != lastDate) {
            lastDate = data.date
            items += OutcomeUiItem.Header(
                DateUtil.formatToLongDate(data.date, "dd/MM/yyyy")
            )
        }
        items += OutcomeUiItem.Entry(data.toHistoryItem())
    }
    return items
}

private val PAYMENT_OPTIONS = listOf("Cash", "QRIS", "Personal")

val OutcomeFormState.paymentOptions: List<String>
    get() = PAYMENT_OPTIONS
