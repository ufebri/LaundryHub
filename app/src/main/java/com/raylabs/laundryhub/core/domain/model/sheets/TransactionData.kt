package com.raylabs.laundryhub.core.domain.model.sheets

import com.raylabs.laundryhub.ui.common.util.DateUtil
import com.raylabs.laundryhub.ui.common.util.DateUtil.parseDate
import com.raylabs.laundryhub.ui.common.util.TextUtil.capitalizeFirstLetter

data class TransactionData(
    val orderID: String,
    val date: String,
    val name: String,
    val weight: String,
    val pricePerKg: String,
    val totalPrice: String,
    val paymentStatus: String,  // "(lunas/belum)"
    val packageType: String,
    val remark: String,
    val paymentMethod: String,
    val phoneNumber: String,
    val dueDate: String
)

fun Map<String, String>.toIncomeList(): TransactionData {
    return TransactionData(
        orderID = (this["orderID"] ?: ""),
        date = (this["Date"] ?: ""),
        name = (this["Name"] ?: ""),
        weight = (this["Weight"] ?: ""),
        pricePerKg = (this["Price/kg"] ?: ""),
        totalPrice = (this["Total Price"] ?: ""),
        paymentStatus = (this["(lunas/belum)"] ?: ""),
        packageType = (this["Package"] ?: ""),
        remark = (this["remark"] ?: ""),
        paymentMethod = (this["payment"] ?: ""),
        phoneNumber = (this["phoneNumber"] ?: ""),
        dueDate = (this["due date"] ?: "")
    )
}

enum class FILTER {
    SHOW_ALL_DATA,
    TODAY_TRANSACTION_ONLY,
    RANGE_TRANSACTION_DATA,
    SHOW_UNPAID_DATA,
    SHOW_PAID_DATA,
    SHOW_PAID_BY_QR,
    SHOW_PAID_BY_CASH
}

data class RangeDate(val startDate: String?, val endDate: String?)

fun TransactionData.getAllIncomeData(): Boolean =
    this.name.isNotEmpty() && this.totalPrice.isNotEmpty()

fun TransactionData.getTodayIncomeData(): Boolean =
    DateUtil.isToday(date = this.date, formatedDate = "dd/MM/yyyy")

fun TransactionData.filterRangeDateData(rangeDate: RangeDate?): Boolean {
    // Filter berdasarkan range tanggal jika ada
    val transactionDate = parseDate(this.date)
    val start = parseDate(rangeDate?.startDate ?: "1900-01-01")
    val end = parseDate(rangeDate?.endDate ?: "2100-01-01")

    return transactionDate != null &&
            start != null &&
            end != null &&
            transactionDate >= start &&
            transactionDate <= end
}

fun TransactionData.isUnpaidData(): Boolean =
    this.paymentStatus.equals(UNPAID_ID, ignoreCase = true) || this.paymentStatus.isEmpty()

fun TransactionData.isPaidData(): Boolean = this.paymentStatus.equals(PAID, ignoreCase = true)

fun TransactionData.isQRISData(): Boolean = this.paymentMethod.equals(QRIS, ignoreCase = true)

fun TransactionData.isCashData(): Boolean = this.paymentMethod.equals(CASH, ignoreCase = true)

fun TransactionData.paidDescription(): String {
    return if (this.isPaidData())
        "Paid by ${this.paymentMethod.capitalizeFirstLetter()}"
    else
        "Unpaid"
}