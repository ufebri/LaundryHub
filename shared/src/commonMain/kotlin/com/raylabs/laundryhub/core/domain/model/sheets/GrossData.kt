package com.raylabs.laundryhub.core.domain.model.sheets

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
