package com.raylabs.laundryhub.ui.home.state

import androidx.compose.ui.graphics.Color
import com.raylabs.laundryhub.core.domain.model.sheets.GrossData
import com.raylabs.laundryhub.ui.common.util.TextUtil.toRupiahFormat

data class GrossItem(
    val id: Int,
    val month: String,
    val totalNominal: String,
    val orderCount: String,
    val tax: String
)

fun List<GrossData>.toUi(): List<GrossItem> = map { it.toUi() }

fun GrossData.toUi(): GrossItem = GrossItem(
    id = id,
    month = month,
    totalNominal = totalNominal.toRupiahFormat(),
    orderCount = orderCount,
    tax = tax.toRupiahFormat()
)

fun GrossData.stableGrossDetailKey(index: Int): String {
    return "gross_${id}_${month}_${totalNominal}_${orderCount}_${tax}_$index"
}

fun GrossItem.toSummaryItem(): SummaryItem {
    val orderLabel = if (orderCount.isBlank()) "" else "$orderCount order"
    return SummaryItem(
        title = "Gross Income",
        body = totalNominal,
        footer = orderLabel,
        backgroundColor = Color(0xFFE6F4EA),
        textColor = Color.Black,
        isInteractive = true
    )
}
