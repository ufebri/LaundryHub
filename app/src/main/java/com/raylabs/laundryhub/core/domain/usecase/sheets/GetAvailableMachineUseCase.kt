package com.raylabs.laundryhub.core.domain.usecase.sheets

import com.raylabs.laundryhub.core.domain.model.sheets.InventoryData
import com.raylabs.laundryhub.core.domain.repository.GoogleSheetRepository
import com.raylabs.laundryhub.ui.common.util.Resource
import com.raylabs.laundryhub.ui.common.util.retry
import javax.inject.Inject

class GetAvailableMachineUseCase @Inject constructor(
    private val repository: GoogleSheetRepository
) {
    suspend operator fun invoke(
        onRetry: ((Int) -> Unit)? = null,
        stationType: String
    ): Resource<List<InventoryData>> {
        val result = retry(onRetry = onRetry) {
            repository.getAvailableMachineByStation(stationType)
        }
        return result ?: Resource.Error("Failed after 3 attempts.")
    }
}