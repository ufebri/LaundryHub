package com.raylabs.laundryhub.core.domain.usecase.sheets.income

import com.raylabs.laundryhub.core.domain.repository.LaundryRepository
import com.raylabs.laundryhub.core.domain.usecase.UseCaseErrorHandling
import com.raylabs.laundryhub.shared.util.Resource
import com.raylabs.laundryhub.ui.common.util.retry
import javax.inject.Inject

open class GetLastOrderIdUseCase @Inject constructor(
    private val repository: LaundryRepository
) {
    open suspend operator fun invoke(
        onRetry: ((Int) -> Unit)? = null
    ): Resource<String> {
        return retry(onRetry = onRetry) {
            repository.getLastOrderId()
        } ?: UseCaseErrorHandling.handleNotFoundID
    }
}