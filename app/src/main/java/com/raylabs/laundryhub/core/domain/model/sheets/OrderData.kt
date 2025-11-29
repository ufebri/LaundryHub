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
    val orderDate: String,
    val dueDate: String,
) {
    val getSpreadSheetPaymentMethod: String
        get() = getDisplayPaymentMethod(paymentMethod)

    val getSpreadSheetPaidStatus: String
        get() = getDisplayPaidStatus(paidStatus)

    val getSpreadSheetDueDate: String
        get() {
            val normalized = dueDate.trim()
            if (normalized.isBlank()) {
                return normalized
            }

            if (normalized.contains("/") || normalized.contains("-")) {
                return normalized
            }

            val sanitizedOrderDate = orderDate.ifBlank { DateUtil.getTodayDate("dd/MM/yyyy") }
            val startDate = "${sanitizedOrderDate.replace('/', '-')} 08:00"
            return DateUtil.getDueDate(normalized, startDate)
        }
}

fun OrderData.toSheetValues(): List<List<String>> {
    return listOf(
        listOf(
            this.orderId,
            this.orderDate.ifBlank { DateUtil.getTodayDate(DateUtil.STANDARD_DATE_FORMATED) },
            this.name,
            this.weight,
            this.priceKg,
            this.totalPrice,
            this.getSpreadSheetPaidStatus,
            this.packageName,
            this.remark,
            this.getSpreadSheetPaymentMethod,
            this.phoneNumber,
            this.getSpreadSheetDueDate
        )
    )
}

fun OrderData.toUpdateSheetValues(existingDate: String): List<List<String>> {
    val fallbackDate =
        existingDate.takeIf { it.isNotBlank() } ?: DateUtil.getTodayDate(
            DateUtil.STANDARD_DATE_FORMATED
        )
    val updatedDate = this.orderDate.ifBlank { fallbackDate }

    return listOf(
        listOf(
            this.orderId,
            updatedDate,
            this.name,
            this.weight,
            this.priceKg,
            this.totalPrice,
            this.getSpreadSheetPaidStatus,
            this.packageName,
            this.remark,
            this.getSpreadSheetPaymentMethod,
            this.phoneNumber,
            this.getSpreadSheetDueDate
        )
    )
}

const val PAID_BY_CASH: String = "Paid by Cash"
const val PAID_BY_QRIS: String = "Paid by QRIS"
const val PAID_BY_PERSONAL: String = "Paid by Personal"
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

val paymentMethodList = listOf(UNPAID, PAID_BY_CASH, PAID_BY_QRIS)
val paymentMethodOutcomeList = listOf(PAID_BY_CASH, PAID_BY_QRIS, PAID_BY_PERSONAL)


