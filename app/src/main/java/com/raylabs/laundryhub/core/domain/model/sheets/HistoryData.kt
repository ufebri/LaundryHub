package com.raylabs.laundryhub.core.domain.model.sheets

import com.raylabs.laundryhub.ui.common.util.DateUtil

data class HistoryData(
    val orderId: String,
    val customerName: String,
    val packageType: String,
    val duration: String,
    val orderDate: String? = "",
    val dueDate: String? = "",
    val status: String? = "",

    val washingDate: String? = "",
    val washingMachine: String? = "",

    val dryingDate: String? = "",
    val dryingMachine: String? = "",

    val ironingDate: String? = "",
    val ironingMachine: String? = "",

    val foldingDate: String? = "",
    val foldingStation: String? = "",

    val packingDate: String? = "",
    val packingStation: String? = "",

    val readyDate: String? = "",
    val completedDate: String? = "",

    val paymentMethod: String,
    val paymentStatus: String,
    val totalPrice: String
)

enum class HistoryFilter {
    SHOW_ALL_DATA,
    SHOW_UNDONE_ORDER
}

const val STATUS_ORDER_PENDING: String = "Pending"

fun Map<String, String>.toHistoryData(): HistoryData {
    fun get(key: String) = this[key]?.toString().orEmpty()

    return HistoryData(
        orderId = get("order_id"),
        customerName = get("customer_name"),
        packageType = get("package"),
        duration = get("duration"),
        orderDate = get("order_date"),
        dueDate = get("due_date"),
        status = get("status"),

        washingDate = get("washing_date"),
        washingMachine = get("washing_machine"),

        dryingDate = get("drying_date"),
        dryingMachine = get("drying_machine"),

        ironingDate = get("ironing_date"),
        ironingMachine = get("ironing_machine"),

        foldingDate = get("folding_date"),
        foldingStation = get("folding_station"),

        packingDate = get("packing_date"),
        packingStation = get("packing_station"),

        readyDate = get("ready_date"),
        completedDate = get("completed_date"),

        paymentMethod = get("payment_method"),
        paymentStatus = get("payment_status"),
        totalPrice = get("total_price")
    )
}

fun HistoryData.toSheetRow(): List<String> {
    return listOf(
        orderId,
        customerName,
        packageType,
        duration,
        DateUtil.getTodayDate("dd/MM/yyyy"),
        DateUtil.getDueDate(dueDate.orEmpty()),
        STATUS_ORDER_PENDING,
    ) + List(12) { "" } + listOf(
        getDisplayPaymentMethod(paymentMethod),
        getDisplayPaidStatus(paymentStatus),
        totalPrice
    )
}
