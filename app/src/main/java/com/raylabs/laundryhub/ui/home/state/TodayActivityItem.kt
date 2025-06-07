package com.raylabs.laundryhub.ui.home.state

import androidx.compose.ui.graphics.Color
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
    var mList = arrayListOf<TodayActivityItem>()
    this.map {
        val mData = TodayActivityItem(
            id = it.orderID,
            name = it.name,
            totalPrice = "Rp ${it.totalPrice.ifEmpty { "0" }},-",
            status = it.paidDescription(),
            statusColor = getColor(it.paymentStatus),
            packageDuration = it.packageType
        )
        mList.add(mData)
    }
    return mList
}

private fun getColor(paymentStatus: String): Color {
    return when (paymentStatus) {
        "" -> RedLaundryHub
        "lunas" -> PurpleLaundryHub
        else -> Color.Black
    }
}



