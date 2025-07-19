package com.raylabs.laundryhub.core.domain.usecase.sheets

import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.repository.GoogleSheetRepository
import com.raylabs.laundryhub.ui.common.util.Resource
import com.raylabs.laundryhub.ui.common.util.retry
import javax.inject.Inject

class GetOrderUseCase @Inject constructor(
    private val repository: GoogleSheetRepository
) {
    suspend operator fun invoke(
        onRetry: ((Int) -> Unit)? = null,
        orderID: String
    ): Resource<TransactionData> {
        val result = retry(onRetry = onRetry) {
            repository.getOrderById(orderID)
        }
        return result ?: Resource.Error("Failed after 3 attempts.")
    }
}