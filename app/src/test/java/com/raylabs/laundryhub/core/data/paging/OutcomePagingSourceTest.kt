package com.raylabs.laundryhub.core.data.paging

import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.core.domain.repository.LaundryRepository
import com.raylabs.laundryhub.shared.util.Resource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class OutcomePagingSourceTest {

    private lateinit var repository: LaundryRepository
    private lateinit var pagingSource: OutcomePagingSource

    @Before
    fun setUp() {
        repository = mock()
        pagingSource = OutcomePagingSource(repository)
    }

    @Test
    fun `fetchData delegates to repository readOutcomeTransaction`() = runTest {
        val mockData = listOf(mock<OutcomeData>())
        whenever(repository.readOutcomeTransaction(1, 10)).thenReturn(Resource.Success(mockData))

        val result = pagingSource.fetchData(1, 10)

        assertEquals(Resource.Success(mockData), result)
        verify(repository).readOutcomeTransaction(1, 10)
    }
}
