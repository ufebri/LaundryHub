package com.raylabs.laundryhub.core.domain.usecase.sheets

import com.raylabs.laundryhub.core.domain.repository.GoogleSheetRepository
import com.raylabs.laundryhub.ui.common.util.Resource
import com.raylabs.laundryhub.ui.common.util.retry
import javax.inject.Inject

open class GetLastOrderIdUseCase @Inject constructor(
    private val repository: GoogleSheetRepository
) {
    open suspend operator fun invoke(
        onRetry: ((Int) -> Unit)? = null
    ): Resource<String> {
        return retry(onRetry = onRetry) {
            repository.getLastOrderId()
        } ?: Resource.Error("Failed to retrieve order ID.")
    }
}