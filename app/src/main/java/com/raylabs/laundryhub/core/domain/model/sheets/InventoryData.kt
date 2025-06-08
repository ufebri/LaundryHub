package com.raylabs.laundryhub.core.domain.model.sheets

data class InventoryData(
    val id: String,
    val stationType: String,
    val stationName: String,
    val machineCode: String,
    val isAvailable: Boolean
)

fun Map<String, String>.toInventoryData(): InventoryData {
    return InventoryData(
        id = this["id"]?.toString().orEmpty(),
        stationType = this["station_type"]?.toString().orEmpty(),
        stationName = this["station_name"]?.toString().orEmpty(),
        machineCode = this["machine_code"]?.toString().orEmpty(),
        isAvailable = this["is_available"]?.toString().equals("TRUE", ignoreCase = true)
    )
}