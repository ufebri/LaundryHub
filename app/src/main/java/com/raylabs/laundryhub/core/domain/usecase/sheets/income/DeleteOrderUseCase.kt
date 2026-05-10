package com.raylabs.laundryhub.core.domain.usecase.sheets.income

import com.raylabs.laundryhub.core.domain.repository.LaundryRepository
import com.raylabs.laundryhub.core.domain.usecase.UseCaseErrorHandling
import com.raylabs.laundryhub.shared.util.Resource
import com.raylabs.laundryhub.ui.common.util.retry

class DeleteOrderUseCase(
    private val repository: LaundryRepository
) {
    suspend operator fun invoke(
        onRetry: ((Int) -> Unit)? = null,
        orderId: String
    ): Resource<Boolean> {
        return retry(onRetry = onRetry) {
            repository.deleteOrder(orderId)
        } ?: UseCaseErrorHandling.handleFailedSubmit
    }
}
