package com.raylabs.laundryhub.core.domain.usecase.sheets

import com.raylabs.laundryhub.core.domain.repository.GoogleSheetRepository
import com.raylabs.laundryhub.ui.common.util.Resource
import com.raylabs.laundryhub.ui.common.util.retry
import javax.inject.Inject

class UpdateMarkStepHistoryUseCase @Inject constructor(
    private val repository: GoogleSheetRepository
) {
    suspend operator fun invoke(
        onRetry: ((Int) -> Unit)? = null,
        orderId: String,
        step: String,
        startedAt: String,
        machineId: String,
        machineName: String
    ): Resource<Boolean> {
        return retry(onRetry = onRetry) {
            val updateResult = repository.updateOrderStep(
                orderId = orderId,
                step = step,
                startedAt = startedAt,
                machineName = machineName
            )
            if (updateResult is Resource.Success && updateResult.data == true) {
                repository.updateMachineAvailability(machineId, false)
            }
            updateResult
        } ?: Resource.Error("Failed to submit data")
    }
}