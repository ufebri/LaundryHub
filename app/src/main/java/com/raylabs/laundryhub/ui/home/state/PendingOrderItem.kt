package com.raylabs.laundryhub.ui.home.state

import com.raylabs.laundryhub.core.domain.model.sheets.HistoryData
import com.raylabs.laundryhub.core.domain.model.sheets.STATUS_ORDER_PENDING

data class PendingOrderItem(
    val orderID: String,
    val customerName: String,
    val packageType: String,
    val nowStatus: String,
    val stationName: String,
    val dueDate: String
)

fun List<HistoryData>.toUI(): List<PendingOrderItem> {
    return this.map {
        PendingOrderItem(
            orderID = it.orderId,
            customerName = it.customerName,
            packageType = it.packageType,
            nowStatus = it.status.orEmpty(),
            stationName = it.getStationNow(),
            dueDate = it.dueDate.orEmpty()
        )
    }
}

fun HistoryData.getStationNow(): String =
    when (this.status) {
        STATUS_ORDER_PENDING -> "Lets Start"
        "Washing" -> washingMachine.orEmpty()
        "Drying" -> dryingMachine.orEmpty()
        "Ironing" -> ironingMachine.orEmpty()
        "Folding" -> foldingStation.orEmpty()
        "Packing" -> packingStation.orEmpty()
        else -> ""
    }.toString()