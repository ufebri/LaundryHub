package com.raylabs.laundryhub.core.domain.model.sheets

data class OutcomeData(
    val id: String,
    val date: String,
    val purpose: String,
    val price: String,
    val remark: String,
    val payment: String
)

fun Map<String, String>.toOutcomeData(): OutcomeData {
    fun String.normalizeKey() = trim().lowercase()
    val normalized = entries.associate { (key, value) ->
        key.normalizeKey() to value
    }
    return OutcomeData(
        id = normalized["id"].orEmpty(),
        date = normalized["date"].orEmpty(),
        purpose = normalized["keperluan"].orEmpty(),
        price = normalized["price"].orEmpty(),
        remark = normalized["remark"].orEmpty(),
        payment = normalized["payment"].orEmpty()
    )
}
