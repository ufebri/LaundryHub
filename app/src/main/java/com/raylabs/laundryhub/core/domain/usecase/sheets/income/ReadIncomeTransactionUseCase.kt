package com.raylabs.laundryhub.core.domain.usecase.sheets.income

import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.model.sheets.RangeDate
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.repository.LaundryRepository
import com.raylabs.laundryhub.core.domain.usecase.UseCaseErrorHandling
import com.raylabs.laundryhub.shared.util.Resource
import com.raylabs.laundryhub.ui.common.util.retry
import javax.inject.Inject

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.raylabs.laundryhub.core.data.paging.OrderPagingSource
import kotlinx.coroutines.flow.Flow

class ReadIncomeTransactionUseCase @Inject constructor(private val repository: LaundryRepository) {
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

    fun getPagingData(
        filter: FILTER = FILTER.SHOW_ALL_DATA,
        rangeDate: RangeDate? = null
    ): Flow<PagingData<TransactionData>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { OrderPagingSource(repository, filter, rangeDate) }
        ).flow
    }
}
