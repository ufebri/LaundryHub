package com.raylabs.laundryhub.ui.home.state

import com.raylabs.laundryhub.core.domain.model.sheets.HistoryData
import com.raylabs.laundryhub.core.domain.model.sheets.groupStatus

data class OrderStatusDetailUiModel(
    val orderId: String,
    val customerName: String,
    val packageType: String,
    val dueDate: String,
    val totalPrice: String,
    val paymentMethod: String,
    val groupStatus: String,
    val steps: List<LaundryStepUiModel>
)

data class LaundryStepUiModel(
    val label: String,           // e.g. "Washing"
    val date: String,            // e.g. "21 Jun, 14:00"
    val selectedMachine: String,        // e.g. "Washer #2"
    val isDone: Boolean,         // based on whether `date` is empty or not
    val isCurrent: Boolean,
    val availableMachines: List<String> = emptyList() // list mesin yang available
)


fun HistoryData.toUi(): OrderStatusDetailUiModel {
    val rawSteps = listOf(
        LaundryStepUiModel("Washing Machine", washingDate.orEmpty(), washingMachine.orEmpty(), washingDate.orEmpty().isNotBlank(), false),
        LaundryStepUiModel("Drying Machine", dryingDate.orEmpty(), dryingMachine.orEmpty(), dryingDate.orEmpty().isNotBlank(), false),
        LaundryStepUiModel("Ironing Machine", ironingDate.orEmpty(), ironingMachine.orEmpty(), ironingDate.orEmpty().isNotBlank(), false),
        LaundryStepUiModel("Folding", foldingDate.orEmpty(), foldingStation.orEmpty(), foldingDate.orEmpty().isNotBlank(), false),
        LaundryStepUiModel("Packing", packingDate.orEmpty(), packingStation.orEmpty(), packingDate.orEmpty().isNotBlank(), false),
        LaundryStepUiModel("Ready", readyDate.orEmpty(), "", readyDate.orEmpty().isNotBlank(), false)
    )
    val currentStepIndex = rawSteps.indexOfFirst { !it.isDone }
    val steps = rawSteps.mapIndexed { index, step ->
        step.copy(isCurrent = index == currentStepIndex)
    }
    return OrderStatusDetailUiModel(
        orderId = orderId,
        customerName = customerName,
        packageType = packageType,
        dueDate = dueDate.orEmpty(),
        totalPrice = totalPrice,
        paymentMethod = paymentMethod,
        groupStatus = groupStatus(),
        steps = steps
    )
}

fun List<LaundryStepUiModel>.withAvailableMachines(availableMachines: List<String>): List<LaundryStepUiModel> {
    val currentStepIndex = indexOfFirst { it.isCurrent && it.selectedMachine.isBlank() }
    if (currentStepIndex == -1) return this
    return mapIndexed { idx, step ->
        if (idx == currentStepIndex) step.copy(availableMachines = availableMachines)
        else step
    }
}
