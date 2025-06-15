package com.raylabs.laundryhub.core.domain.model.sheets

import com.raylabs.laundryhub.ui.common.util.DateUtil

data class OrderData(
    val orderId: String,
    val name: String,
    val phoneNumber: String,
    val packageName: String,
    val priceKg: String,
    val totalPrice: String,
    val paidStatus: String,
    val paymentMethod: String,
    val remark: String,
    val weight: String,
    val dueDate: String,
) {
    val getSpreadSheetPaymentMethod: String
        get() = getDisplayPaymentMethod(paymentMethod)

    val getSpreadSheetPaidStatus: String
        get() = getDisplayPaidStatus(paidStatus)

    val getSpreadSheetDueDate: String
        get() = DateUtil.getDueDate(dueDate)
}

const val PAID_BY_CASH: String = "Paid by Cash"
const val PAID_BY_QRIS: String = "Paid by QRIS"
const val UNPAID: String = "Unpaid"

const val PAID = "lunas"
const val UNPAID_ID = "belum"
const val QRIS = "qris"
const val CASH = "cash"

fun getDisplayPaymentMethod(paymentMethod: String): String {
    return when (paymentMethod) {
        PAID_BY_CASH -> CASH
        PAID_BY_QRIS -> QRIS
        UNPAID -> UNPAID
        else -> ""
    }
}

fun getDisplayPaidStatus(paidStatus: String): String {
    return when (paidStatus) {
        PAID_BY_CASH, PAID_BY_QRIS -> PAID
        UNPAID -> UNPAID_ID
        else -> ""
    }
}

val paymentMethodList = listOf(PAID_BY_CASH, PAID_BY_QRIS, UNPAID)