package com.raylabs.laundryhub.core.domain.usecase.sheets

import com.raylabs.laundryhub.core.domain.model.sheets.GrossData
import com.raylabs.laundryhub.core.domain.repository.LaundryRepository
import com.raylabs.laundryhub.shared.util.Resource
import com.raylabs.laundryhub.ui.common.util.retry
import javax.inject.Inject

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.raylabs.laundryhub.core.data.paging.GrossPagingSource
import kotlinx.coroutines.flow.Flow

class ReadGrossDataUseCase @Inject constructor(
    private val repository: LaundryRepository
) {
    suspend operator fun invoke(
        onRetry: ((Int) -> Unit)? = null
    ): Resource<List<GrossData>> {
        val result = retry(onRetry = onRetry) { repository.readGrossData() }
        return result ?: Resource.Error("Failed after 3 attempts.")
    }

    fun getPagingData(): Flow<PagingData<GrossData>> {
        return Pager(
            config = PagingConfig(pageSize = 12, enablePlaceholders = false),
            pagingSourceFactory = { GrossPagingSource(repository) }
        ).flow
    }
}
