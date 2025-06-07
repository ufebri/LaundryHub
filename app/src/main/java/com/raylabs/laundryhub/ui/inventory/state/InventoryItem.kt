package com.raylabs.laundryhub.ui.inventory.state

import androidx.compose.ui.graphics.Color
import com.raylabs.laundryhub.core.domain.model.sheets.InventoryData

data class MachineItem(
    val id: String,
    val name: String,
    val code: String,
    val isAvailable: Boolean
)


data class InventoryCardItemData(
    val title: String,
    val subtitle: String,
    val color: Color
)

data class InventoryGroupItem(
    val stationType: String,
    val machines: List<MachineItem>
) {
    val availableCount: Int
        get() = machines.count { it.isAvailable }
}

fun List<InventoryData>.toGroupedInventoryUi(): List<InventoryGroupItem> {
    return this
        .groupBy { it.stationType }
        .map { (type, machines) ->
            InventoryGroupItem(
                stationType = type,
                machines = machines.map {
                    MachineItem(
                        id = it.id,
                        name = it.stationName,
                        code = it.machineCode,
                        isAvailable = it.isAvailable
                    )
                }
            )
        }
}