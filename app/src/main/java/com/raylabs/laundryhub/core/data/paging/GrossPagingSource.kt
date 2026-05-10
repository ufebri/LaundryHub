package com.raylabs.laundryhub.core.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.raylabs.laundryhub.core.domain.model.sheets.GrossData
import com.raylabs.laundryhub.core.domain.repository.LaundryRepository
import com.raylabs.laundryhub.shared.util.Resource

class GrossPagingSource(
    private val repository: LaundryRepository
) : PagingSource<Int, GrossData>() {

    override fun getRefreshKey(state: PagingState<Int, GrossData>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GrossData> {
        val page = params.key ?: 1
        val size = params.loadSize

        return try {
            val result = repository.readGrossData(
                page = page,
                size = size
            )

            when (result) {
                is Resource.Success -> {
                    val data = result.data
                    LoadResult.Page(
                        data = data,
                        prevKey = if (page == 1) null else page - 1,
                        nextKey = if (data.isEmpty()) null else page + 1
                    )
                }
                is Resource.Empty -> {
                    LoadResult.Page(
                        data = emptyList(),
                        prevKey = null,
                        nextKey = null
                    )
                }
                is Resource.Error -> {
                    LoadResult.Error(Exception(result.message))
                }
                else -> LoadResult.Error(Exception("Unknown Error"))
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
