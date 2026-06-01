package com.raylabs.laundryhub.core.data.paging

import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.model.sheets.RangeDate
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.repository.LaundryRepository
import com.raylabs.laundryhub.shared.util.Resource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class OrderPagingSourceTest {

    private lateinit var repository: LaundryRepository
    private lateinit var pagingSource: OrderPagingSource
    private val filter = FILTER.SHOW_ALL_DATA
    private val rangeDate = RangeDate("2026-06-01", "2026-06-02")
    private val searchQuery = "customer"
    private val sort = "date"

    @Before
    fun setUp() {
        repository = mock()
        pagingSource = OrderPagingSource(
            repository = repository,
            filter = filter,
            rangeDate = rangeDate,
            searchQuery = searchQuery,
            sort = sort
        )
    }

    @Test
    fun `fetchData delegates to repository readIncomeTransaction`() = runTest {
        val mockData = listOf(mock<TransactionData>())
        whenever(
            repository.readIncomeTransaction(
                filter = filter,
                rangeDate = rangeDate,
                page = 1,
                size = 10,
                searchQuery = searchQuery,
                sort = sort
            )
        ).thenReturn(Resource.Success(mockData))

        val result = pagingSource.fetchData(1, 10)

        assertEquals(Resource.Success(mockData), result)
        verify(repository).readIncomeTransaction(
            filter = filter,
            rangeDate = rangeDate,
            page = 1,
            size = 10,
            searchQuery = searchQuery,
            sort = sort
        )
    }
}
