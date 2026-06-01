package com.raylabs.laundryhub.core.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.raylabs.laundryhub.shared.util.Resource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BasePagingSourceTest {

    // Concrete test implementation of BasePagingSource
    private class TestPagingSource(
        val stubbedResponse: Resource<List<String>>
    ) : BasePagingSource<String>() {
        override suspend fun fetchData(page: Int, size: Int): Resource<List<String>> {
            return stubbedResponse
        }
    }

    private class ExceptionPagingSource : BasePagingSource<String>() {
        override suspend fun fetchData(page: Int, size: Int): Resource<List<String>> {
            throw RuntimeException("Database error")
        }
    }

    @Test
    fun `load returns Page on Resource Success`() = runTest {
        val successData = listOf("Item 1", "Item 2")
        val pagingSource = TestPagingSource(Resource.Success(successData))

        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = 1,
                loadSize = 2,
                placeholdersEnabled = false
            )
        )

        assertTrue(result is PagingSource.LoadResult.Page)
        val pageResult = result as PagingSource.LoadResult.Page
        assertEquals(successData, pageResult.data)
        assertNull(pageResult.prevKey)
        assertEquals(2, pageResult.nextKey)
    }

    @Test
    fun `load returns Page with empty list and null keys on Resource Empty`() = runTest {
        val pagingSource = TestPagingSource(Resource.Empty)

        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = 1,
                loadSize = 2,
                placeholdersEnabled = false
            )
        )

        assertTrue(result is PagingSource.LoadResult.Page)
        val pageResult = result as PagingSource.LoadResult.Page
        assertTrue(pageResult.data.isEmpty())
        assertNull(pageResult.prevKey)
        assertNull(pageResult.nextKey)
    }

    @Test
    fun `load returns Error on Resource Error`() = runTest {
        val pagingSource = TestPagingSource(Resource.Error("API failed"))

        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = 1,
                loadSize = 2,
                placeholdersEnabled = false
            )
        )

        assertTrue(result is PagingSource.LoadResult.Error)
        val errorResult = result as PagingSource.LoadResult.Error
        assertEquals("API failed", errorResult.throwable.message)
    }

    @Test
    fun `load returns Error on exception`() = runTest {
        val pagingSource = ExceptionPagingSource()

        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = 1,
                loadSize = 2,
                placeholdersEnabled = false
            )
        )

        assertTrue(result is PagingSource.LoadResult.Error)
        val errorResult = result as PagingSource.LoadResult.Error
        assertEquals("Database error", errorResult.throwable.message)
    }

    @Test
    fun `getRefreshKey returns null when anchorPosition is null`() {
        val pagingSource = TestPagingSource(Resource.Empty)
        val state = PagingState<Int, String>(
            pages = emptyList(),
            anchorPosition = null,
            config = androidx.paging.PagingConfig(pageSize = 10),
            leadingPlaceholderCount = 0
        )
        assertNull(pagingSource.getRefreshKey(state))
    }
}
