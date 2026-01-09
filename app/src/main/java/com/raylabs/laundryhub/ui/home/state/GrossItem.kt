package com.raylabs.laundryhub.ui.home.state

import androidx.compose.ui.graphics.Color
import com.raylabs.laundryhub.core.domain.model.sheets.GrossData

data class GrossItem(
    val month: String,
    val totalNominal: String,
    val orderCount: String,
    val tax: String
)

fun List<GrossData>.toUi(): List<GrossItem> = map { it.toUi() }

fun GrossData.toUi(): GrossItem = GrossItem(
    month = month,
    totalNominal = totalNominal,
    orderCount = orderCount,
    tax = tax
)

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
