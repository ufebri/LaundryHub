package com.raylabs.laundryhub.core.domain.model.sheets

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable


@Serializable
data class GrossData(
    val id: Int = 0,
    val month: String,
    val totalNominal: String,
    val orderCount: String,
    val tax: String
)

const val GROSS_MONTH = "bulan"
const val GROSS_TOTAL_NOMINAL = "total nominal"
const val GROSS_ORDER_COUNT = "# nota laundry"
const val GROSS_TAX = "pajak"

fun Map<String, String>.toGrossData(): GrossData {
    return GrossData(
        month = this[GROSS_MONTH].orEmpty(),
        totalNominal = this[GROSS_TOTAL_NOMINAL].orEmpty(),
        orderCount = this[GROSS_ORDER_COUNT].orEmpty(),
        tax = this[GROSS_TAX].orEmpty()
    )
}

fun GrossData.toSheetValues(): List<List<String>> {
    return listOf(
        listOf(
            month,
            totalNominal,
            orderCount,
            tax
        )
    )
}

fun List<GrossData>.selectCurrentOrLatestGross(
    currentYear: Int = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year,
    currentMonth: Int = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).monthNumber
): GrossData? {
    val currentKey = grossMonthKey(currentYear, currentMonth)
    return firstOrNull { it.grossMonthKey() == currentKey }
        ?: mapNotNull { row -> row.grossMonthKey()?.let { it to row } }
            .maxByOrNull { it.first }
            ?.second
        ?: lastOrNull { it.month.isNotBlank() }
}

fun List<GrossData>.sortedByGrossMonthDescending(): List<GrossData> {
    return sortedWith(
        compareByDescending<GrossData> { it.grossMonthKey() ?: Int.MIN_VALUE }
            .thenByDescending { it.month }
    )
}

fun GrossData.grossMonthKey(): Int? = parseGrossMonthKey(month)

fun parseGrossMonthKey(monthText: String): Int? {
    val parts = Regex("""[\p{L}]+|\d+""")
        .findAll(monthText.lowercase())
        .map { it.value }
        .toList()
    val year = parts.firstNotNullOfOrNull { part ->
        part.toIntOrNull()?.let { value ->
            when (value) {
                in 1900..2999 -> value
                in 0..99 -> 2000 + value
                else -> null
            }
        }
    } ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
    val month = parts.firstNotNullOfOrNull { MONTH_NAMES[it] }
        ?: return null
    return grossMonthKey(year, month)
}

private fun grossMonthKey(year: Int, month: Int): Int = year * 100 + month

private val MONTH_NAMES = mapOf(
    "januari" to 1,
    "january" to 1,
    "jan" to 1,
    "februari" to 2,
    "february" to 2,
    "feb" to 2,
    "maret" to 3,
    "march" to 3,
    "mar" to 3,
    "april" to 4,
    "apr" to 4,
    "mei" to 5,
    "may" to 5,
    "juni" to 6,
    "june" to 6,
    "jun" to 6,
    "juli" to 7,
    "july" to 7,
    "jul" to 7,
    "agustus" to 8,
    "august" to 8,
    "aug" to 8,
    "september" to 9,
    "sep" to 9,
    "oktober" to 10,
    "october" to 10,
    "oct" to 10,
    "november" to 11,
    "nov" to 11,
    "desember" to 12,
    "december" to 12,
    "dec" to 12
)
