package com.raylabs.laundryhub.core.data.paging

import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.core.domain.repository.LaundryRepository
import com.raylabs.laundryhub.shared.util.Resource

class OutcomePagingSource(
    private val repository: LaundryRepository
) : BasePagingSource<OutcomeData>() {

    override suspend fun fetchData(page: Int, size: Int): Resource<List<OutcomeData>> {
        return repository.readOutcomeTransaction(
            page = page,
            size = size
        )
    }
}
