package com.raylabs.laundryhub.ui.home.state

import androidx.compose.ui.graphics.Color
import com.raylabs.laundryhub.core.domain.model.sheets.SpreadsheetData

data class SummaryItem(
    var title: String,
    var body: String,
    var footer: String,
    var backgroundColor: Color,
    var textColor: Color = Color.White
)

const val CASH_ON_HAND = "Total Cash di mama"
const val READY_TO_PICKUP = "ReadyToPickup Status"
const val NEW_MEMBER = "New Member last 7 days"
const val MONTHLY_TARGET_PERCENTAGE = "Monthly Target Percentage"
const val MONTHLY_TARGET_STATUS = "Monthly Target Status"

val DUMMY_SUMMARY_ITEM = listOf(
    SummaryItem(title = "Ready To Pick", body = "32 Orders", footer = "Send message →", backgroundColor = Color(0xFFFEF7FF), textColor = Color.Black),
    SummaryItem(title = "New Member", body = "2 Members", footer = "Last 7 days", backgroundColor = Color(0xFFF9DEDC), textColor = Color.Black),
    SummaryItem(title = "Pending", body = "2 Orders", footer = "Avg: 1d 2h in last 7d", backgroundColor = Color(0xFFB3261E)),
    SummaryItem(title = "Cash On Hand", body = "Rp 200.002", footer = "", backgroundColor = Color(0xFF6750A4)),
)

fun List<SpreadsheetData>.toUI(): List<SummaryItem> =
    listOfNotNull(
        find { it.key == READY_TO_PICKUP }?.toReadyToPick(),
        find { it.key == NEW_MEMBER }?.toNewMember(),
        toMonthlyTarget(),
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

fun List<SpreadsheetData>.toMonthlyTarget(): SummaryItem =
    SummaryItem(
        title = "Monthly Target",
        body = "${this.firstOrNull { it.key == MONTHLY_TARGET_PERCENTAGE }?.value}",
        footer = "${this.firstOrNull { it.key == MONTHLY_TARGET_STATUS }?.value}",
        backgroundColor = Color(0xFFB3261E)
    )

fun SpreadsheetData.toCashOnHand(): SummaryItem = SummaryItem(
    title = "Cash On Hand",
    body = this.value,
    footer = "",
    backgroundColor = Color(0xFF6750A4)
)