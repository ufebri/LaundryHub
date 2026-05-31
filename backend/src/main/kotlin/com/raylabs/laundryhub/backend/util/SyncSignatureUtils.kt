package com.raylabs.laundryhub.backend.util

import com.raylabs.laundryhub.core.domain.model.sheets.GrossData
import com.raylabs.laundryhub.core.domain.model.sheets.OrderData
import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.core.domain.model.sheets.PackageData
import com.raylabs.laundryhub.core.domain.model.sheets.SpreadsheetData

fun OrderData.syncVerificationSignature(): String = listOf(
    orderId.trim(),
    orderDate.normalizedSyncText(),
    name.normalizedSyncText(),
    weight.normalizedSyncNumberText(),
    priceKg.normalizedSyncNumberText(),
    totalPrice.normalizedSyncNumberText(),
    paidStatus.normalizedSyncStatusText(),
    packageName.normalizedSyncText(),
    remark.normalizedSyncText(),
    paymentMethod.normalizedSyncText(),
    phoneNumber.normalizedSyncPhoneText(),
    dueDate.normalizedSyncText()
).joinToString(VERIFICATION_SEPARATOR)

fun OutcomeData.syncVerificationSignature(): String = listOf(
    id.trim(),
    date.normalizedSyncText(),
    purpose.normalizedSyncText(),
    price.normalizedSyncNumberText(),
    remark.normalizedSyncText(),
    payment.normalizedSyncText()
).joinToString(VERIFICATION_SEPARATOR)

fun PackageData.syncVerificationSignature(): String = listOf(
    name.normalizedSyncText(),
    price.normalizedSyncNumberText(),
    duration.normalizedSyncNumberText(),
    unit.normalizedSyncText()
).joinToString(VERIFICATION_SEPARATOR)

fun GrossData.syncVerificationSignature(): String = listOf(
    month.normalizedSyncText(),
    totalNominal.normalizedSyncNumberText(),
    orderCount.normalizedSyncNumberText(),
    tax.normalizedSyncNumberText()
).joinToString(VERIFICATION_SEPARATOR)

fun SpreadsheetData.syncVerificationSignature(): String = listOf(
    key.normalizedSyncText(),
    value.normalizedSyncNumberText() // Most summary values are numeric or formatted currency
).joinToString(VERIFICATION_SEPARATOR)

internal fun String.normalizedSyncText(): String = trim().lowercase()

internal fun String.normalizedSyncNumberText(): String {
    val trimmed = trim()
    val digits = trimmed.filter { it.isDigit() }
    return digits.ifBlank { trimmed.lowercase() }
}

internal fun String.normalizedSyncPhoneText(): String {
    return this.normalizedSyncNumberText()
}

internal fun String.normalizedSyncStatusText(): String {
    return when (trim().lowercase()) {
        "lunas", "paid" -> "paid"
        "belum", "unpaid", "" -> "unpaid"
        else -> trim().lowercase()
    }
}

private const val VERIFICATION_SEPARATOR = "\u001F"
