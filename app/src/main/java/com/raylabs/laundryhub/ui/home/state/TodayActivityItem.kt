package com.raylabs.laundryhub.ui.home.state

import androidx.compose.ui.graphics.Color
import com.raylabs.laundryhub.core.domain.model.sheets.PAID
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.model.sheets.paidDescription
import com.raylabs.laundryhub.ui.theme.PurpleLaundryHub
import com.raylabs.laundryhub.ui.theme.RedLaundryHub

data class TodayActivityItem(
    val id: String,
    val name: String,
    val totalPrice: String,
    val status: String,
    val statusColor: Color,
    val packageDuration: String
)

fun List<TransactionData>.toUI(): List<TodayActivityItem> {
    return this.map {
        TodayActivityItem(
            id = it.orderID,
            name = it.name,
            totalPrice = "${it.totalPrice.ifEmpty { "Rp 0" }},-",
            status = it.paidDescription(),
            statusColor = getColor(it.paymentStatus),
            packageDuration = it.packageType
        )
    }.toList()
}

private fun getColor(paymentStatus: String): Color {
    return when (paymentStatus) {
        "" -> RedLaundryHub
        PAID -> PurpleLaundryHub
        else -> Color.Black
    }
}



