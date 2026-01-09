package com.raylabs.laundryhub.ui.home.state

import androidx.compose.ui.graphics.Color
import com.raylabs.laundryhub.core.domain.model.sheets.SpreadsheetData

data class SummaryItem(
    var title: String,
    var body: String,
    var footer: String,
    var backgroundColor: Color,
    var textColor: Color = Color.White,
    val isInteractive: Boolean = false
)

const val CASH_ON_HAND = "Total Cash di mama"
const val READY_TO_PICKUP = "ReadyToPickup Status"
const val NEW_MEMBER = "New Member last 7 days"

val DUMMY_SUMMARY_ITEM = listOf(
    SummaryItem(title = "Ready To Pick", body = "32 Orders", footer = "Send message →", backgroundColor = Color(0xFFFEF7FF), textColor = Color.Black),
    SummaryItem(title = "New Member", body = "2 Members", footer = "Last 7 days", backgroundColor = Color(0xFFF9DEDC), textColor = Color.Black),
    SummaryItem(title = "Gross Income", body = "Rp 3.944.000", footer = "158 order", backgroundColor = Color(0xFFE6F4EA), textColor = Color.Black, isInteractive = true),
    SummaryItem(title = "Cash On Hand", body = "Rp 200.002", footer = "", backgroundColor = Color(0xFF6750A4)),
)

fun List<SpreadsheetData>.toUI(grossItem: GrossItem? = null): List<SummaryItem> =
    listOfNotNull(
        find { it.key == READY_TO_PICKUP }?.toReadyToPick(),
        find { it.key == NEW_MEMBER }?.toNewMember(),
        grossItem?.toSummaryItem(),
        find { it.key == CASH_ON_HAND }?.toCashOnHand()
    )

fun SpreadsheetData.toReadyToPick(): SummaryItem {
    return SummaryItem(
        title = "Ready To Pick",
        body = "$value Orders",
        footer = if (value == "0") "" else "Send message →",
        backgroundColor = Color(0xFFFEF7FF),
        textColor = Color.Black
    )
}

fun SpreadsheetData.toNewMember(): SummaryItem {
    return SummaryItem(
        title = "New Member",
        body = if (this.value == "0") "Good Days" else "${this.value} Members",
        footer = if (this.value == "0") "" else "Last 7 days",
        backgroundColor = Color(0xFFF9DEDC),
        textColor = Color.Black
    )
}

fun SpreadsheetData.toCashOnHand(): SummaryItem = SummaryItem(
    title = "Cash On Hand",
    body = this.value,
    footer = "",
    backgroundColor = Color(0xFF6750A4)
)
