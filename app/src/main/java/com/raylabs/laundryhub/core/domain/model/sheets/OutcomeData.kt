package com.raylabs.laundryhub.core.domain.model.sheets

import com.raylabs.laundryhub.ui.common.util.DateUtil

data class OutcomeData(
    val id: String,
    val date: String,
    val purpose: String,
    val price: String,
    val remark: String,
    val payment: String,
)

fun Map<String, String>.toOutcomeList(): OutcomeData {
    return OutcomeData(
        id = (this["id"] ?: ""),
        date = (this["date"] ?: ""),
        purpose = (this["keperluan"] ?: ""),
        price = (this["price"] ?: ""),
        remark = (this["remark"] ?: ""),
        payment = (this["payment"] ?: ""),
    )
}

fun OutcomeData.toSheetValues(): List<List<String>> {
    return listOf(
        listOf(
            this.id,
            this.date.ifBlank { DateUtil.getTodayDate(DateUtil.STANDARD_DATE_FORMATED) },
            this.purpose,
            this.price,
            this.remark,
            this.payment
        )
    )
}

fun OutcomeData.toUpdateSheetValues(existingDate: String): List<List<String>> {
    val fallbackDate =
        existingDate.takeIf { it.isNotBlank() } ?: DateUtil.getTodayDate(
            DateUtil.STANDARD_DATE_FORMATED
        )
    val updatedDate = this.date.ifBlank { fallbackDate }

    return listOf(
        listOf(
            this.id,
            updatedDate,
            this.purpose,
            this.price,
            this.remark,
            this.payment
        )
    )
}

enum class PaymentType(val value: String, val description: String) {
    QRIS("qris", PAID_BY_QRIS),
    CASH("cash", PAID_BY_CASH),
    PERSONAL("personal", PAID_BY_PERSONAL);


    // Fungsi untuk mendapatkan enum dari nilai string (kebalikannya)
    companion object {
        fun fromValue(value: String): PaymentType? {
            // entries adalah fungsi baru di Kotlin yang lebih efisien dari values()
            return entries.find { it.value == value }
        }

        fun fromDescription(description: String): PaymentType? {
            return entries.find { it.description == description }
        }
    }
}


fun OutcomeData.paidDescription(): String = PaymentType.fromValue(this.payment)?.description ?: ""

fun getPaymentValueFromDescription(description: String): String =
    PaymentType.fromDescription(description)?.value ?: ""