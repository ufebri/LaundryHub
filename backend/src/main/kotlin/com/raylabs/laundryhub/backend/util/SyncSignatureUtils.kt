package com.raylabs.laundryhub.backend.util

import com.raylabs.laundryhub.core.domain.model.sheets.GrossData
import com.raylabs.laundryhub.core.domain.model.sheets.OrderData
import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.core.domain.model.sheets.PackageData
import com.raylabs.laundryhub.core.domain.model.sheets.SpreadsheetData

fun OrderData.syncVerificationSignature(): String = listOf(
    orderId.trim(),
    orderDate.normalizedText(),
    name.normalizedText(),
    weight.normalizedNumberText(),
    priceKg.normalizedNumberText(),
    totalPrice.normalizedNumberText(),
    paidStatus.normalizedStatusText(),
    packageName.normalizedText(),
    remark.normalizedText(),
    paymentMethod.normalizedText(),
    phoneNumber.normalizedPhoneText(),
    dueDate.normalizedText()
).joinToString(VERIFICATION_SEPARATOR)

fun OutcomeData.syncVerificationSignature(): String = listOf(
    id.trim(),
    date.normalizedText(),
    purpose.normalizedText(),
    price.normalizedNumberText(),
    remark.normalizedText(),
    payment.normalizedText()
).joinToString(VERIFICATION_SEPARATOR)

fun PackageData.syncVerificationSignature(): String = listOf(
    name.normalizedText(),
    price.normalizedNumberText(),
    duration.normalizedNumberText(),
    unit.normalizedText()
).joinToString(VERIFICATION_SEPARATOR)

fun GrossData.syncVerificationSignature(): String = listOf(
    month.normalizedText(),
    totalNominal.normalizedNumberText(),
    orderCount.normalizedNumberText(),
    tax.normalizedNumberText()
).joinToString(VERIFICATION_SEPARATOR)

fun SpreadsheetData.syncVerificationSignature(): String = listOf(
    key.normalizedText(),
    value.normalizedNumberText() // Most summary values are numeric or formatted currency
).joinToString(VERIFICATION_SEPARATOR)

private fun String.normalizedText(): String = trim().lowercase()

private fun String.normalizedNumberText(): String {
    val trimmed = trim()
    val digits = trimmed.filter { it.isDigit() }
    return digits.ifBlank { trimmed.lowercase() }
}

private fun String.normalizedPhoneText(): String {
    val trimmed = trim()
    val digits = trimmed.filter { it.isDigit() }
    return digits.ifBlank { trimmed.lowercase() }
}

private fun String.normalizedStatusText(): String {
    return when (trim().lowercase()) {
        "lunas", "paid" -> "paid"
        "belum", "unpaid", "" -> "unpaid"
        else -> trim().lowercase()
    }
}

private const val VERIFICATION_SEPARATOR = "\u001F"
