package com.raylabs.laundryhub.core.data.paging

import com.raylabs.laundryhub.core.domain.model.sheets.GrossData
import com.raylabs.laundryhub.core.domain.repository.LaundryRepository
import com.raylabs.laundryhub.shared.util.Resource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class GrossPagingSourceTest {

    private lateinit var repository: LaundryRepository
    private lateinit var pagingSource: GrossPagingSource

    @Before
    fun setUp() {
        repository = mock()
        pagingSource = GrossPagingSource(repository)
    }

    @Test
    fun `fetchData delegates to repository readGrossData`() = runTest {
        val mockData = listOf(mock<GrossData>())
        whenever(repository.readGrossData(1, 10)).thenReturn(Resource.Success(mockData))

        val result = pagingSource.fetchData(1, 10)

        assertEquals(Resource.Success(mockData), result)
        verify(repository).readGrossData(1, 10)
    }
}
