package com.raylabs.laundryhub.core.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.raylabs.laundryhub.shared.util.Resource

abstract class BasePagingSource<Value : Any> : PagingSource<Int, Value>() {

    abstract suspend fun fetchData(page: Int, size: Int): Resource<List<Value>>

    override fun getRefreshKey(state: PagingState<Int, Value>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Value> {
        val page = params.key ?: 1
        val size = params.loadSize

        return try {
            when (val result = fetchData(page, size)) {
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
