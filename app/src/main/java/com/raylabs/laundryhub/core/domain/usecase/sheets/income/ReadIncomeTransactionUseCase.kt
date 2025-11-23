package com.raylabs.laundryhub.core.domain.usecase.sheets.income

import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.model.sheets.RangeDate
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.repository.GoogleSheetRepository
import com.raylabs.laundryhub.core.domain.usecase.UseCaseErrorHandling
import com.raylabs.laundryhub.ui.common.util.Resource
import com.raylabs.laundryhub.ui.common.util.retry
import javax.inject.Inject

class ReadIncomeTransactionUseCase @Inject constructor(private val repository: GoogleSheetRepository) {
    suspend operator fun invoke(
        onRetry: ((Int) -> Unit)? = null,
        filter: FILTER = FILTER.SHOW_ALL_DATA,
        rangeDate: RangeDate? = null
    ): Resource<List<TransactionData>> {
        val result = retry(onRetry = onRetry) {
            repository.readIncomeTransaction(filter, rangeDate)
        }
        return result ?: UseCaseErrorHandling.handleFailRetry
    }
}
