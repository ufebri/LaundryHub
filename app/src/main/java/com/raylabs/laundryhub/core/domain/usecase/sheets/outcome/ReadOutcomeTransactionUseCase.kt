package com.raylabs.laundryhub.core.domain.usecase.sheets.outcome

import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.core.domain.repository.LaundryRepository
import com.raylabs.laundryhub.core.domain.usecase.UseCaseErrorHandling
import com.raylabs.laundryhub.shared.util.Resource
import com.raylabs.laundryhub.ui.common.util.retry
import javax.inject.Inject

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.raylabs.laundryhub.core.data.paging.OutcomePagingSource
import kotlinx.coroutines.flow.Flow

class ReadOutcomeTransactionUseCase @Inject constructor(private val repository: LaundryRepository) {
    suspend operator fun invoke(
        onRetry: ((Int) -> Unit)? = null
    ): Resource<List<OutcomeData>> {
        val result = retry(onRetry = onRetry) {
            repository.readOutcomeTransaction()
        }
        return result ?: UseCaseErrorHandling.handleFailRetry
    }

    fun getPagingData(): Flow<PagingData<OutcomeData>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { OutcomePagingSource(repository) }
        ).flow
    }
}
