package com.raylabs.laundryhub.core.domain.usecase.sheets

import com.raylabs.laundryhub.core.domain.model.sheets.OrderData
import com.raylabs.laundryhub.core.domain.repository.GoogleSheetRepository
import com.raylabs.laundryhub.ui.common.util.Resource
import com.raylabs.laundryhub.ui.common.util.retry

class UpdateOrderUseCase(
    private val repository: GoogleSheetRepository
) {
    suspend operator fun invoke(
        onRetry: ((Int) -> Unit)? = null,
        order: OrderData
    ): Resource<Boolean> {
        return retry(onRetry = onRetry) {
            repository.updateOrder(order)
        } ?: Resource.Error("Failed to submit data")
    }
}