package com.raylabs.laundryhub.core.domain.usecase.sheets.income

import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.repository.LaundryRepository
import com.raylabs.laundryhub.core.domain.usecase.UseCaseErrorHandling
import com.raylabs.laundryhub.shared.util.Resource
import com.raylabs.laundryhub.ui.common.util.retry
import javax.inject.Inject

class GetOrderUseCase @Inject constructor(
    private val repository: LaundryRepository
) {
    suspend operator fun invoke(
        onRetry: ((Int) -> Unit)? = null,
        orderID: String
    ): Resource<TransactionData> {
        val result = retry(onRetry = onRetry) {
            repository.getOrderById(orderID)
        }
        return result ?: UseCaseErrorHandling.handleFailRetry
    }
}