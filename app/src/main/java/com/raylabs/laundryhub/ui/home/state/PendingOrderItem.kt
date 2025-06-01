package com.raylabs.laundryhub.ui.home.state

import com.raylabs.laundryhub.core.domain.model.sheets.HistoryData

data class PendingOrderItem(
    val orderID: String,
    val customerName: String,
    val packageType: String,
    val nowStatus: String,
    val stationName: String,
    val dueDate: String
)

fun List<HistoryData>.toUI(): List<PendingOrderItem> {
    val mList = arrayListOf<PendingOrderItem>()
    this.map {
        val mData = PendingOrderItem(
            orderID = it.orderId,
            customerName = it.customerName,
            packageType = it.packageType,
            nowStatus = it.status,
            stationName = it.getStationNow(),
            dueDate = it.dueDate
        )
        mList.add(mData)
    }
    return mList
}

fun HistoryData.getStationNow(): String =
    when (this.status) {
        "Pending" -> "Lets Start"
        "Washing" -> this.washingMachine
        "Drying" -> this.dryingMachine
        "Ironing" -> this.ironingMachine
        "Folding" -> this.foldingStation
        "Packing" -> this.packingStation
        else -> ""
    }