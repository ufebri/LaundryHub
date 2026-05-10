package com.raylabs.laundryhub.ui.home.state

import androidx.compose.ui.graphics.Color
import com.raylabs.laundryhub.core.domain.model.sheets.PAID
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.model.sheets.paidDescription
import com.raylabs.laundryhub.ui.common.util.TextUtil.toRupiahFormat
import com.raylabs.laundryhub.ui.theme.PurpleLaundryHub
import com.raylabs.laundryhub.ui.theme.RedLaundryHub

data class TransactionItem(
    val id: String,
    val name: String,
    val totalPrice: String,
    val status: String,
    val statusColor: Color,
    val packageDuration: String
)

fun List<TransactionData>.toUI(): List<TransactionItem> {
    return this.map {
        TransactionItem(
            id = it.orderID,
            name = it.name,
            totalPrice = it.totalPrice.toRupiahFormat(),
            status = it.paidDescription(),
            statusColor = it.paymentStatus.toColor(),
            packageDuration = it.packageType
        )
    }.toList()
}

fun String.toColor(): Color {
    return when (this.lowercase()) {
        "unpaid", "belum", "" -> RedLaundryHub
        "paid", "lunas", PAID.lowercase() -> PurpleLaundryHub
        else -> if (this.contains("Paid", ignoreCase = true)) PurpleLaundryHub else Color.Black
    }
}



