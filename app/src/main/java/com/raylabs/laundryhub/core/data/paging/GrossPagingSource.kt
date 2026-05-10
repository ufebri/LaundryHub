package com.raylabs.laundryhub.core.data.paging

import com.raylabs.laundryhub.core.domain.model.sheets.GrossData
import com.raylabs.laundryhub.core.domain.repository.LaundryRepository
import com.raylabs.laundryhub.shared.util.Resource

class GrossPagingSource(
    private val repository: LaundryRepository
) : BasePagingSource<GrossData>() {

    override suspend fun fetchData(page: Int, size: Int): Resource<List<GrossData>> {
        return repository.readGrossData(
            page = page,
            size = size
        )
    }
}
