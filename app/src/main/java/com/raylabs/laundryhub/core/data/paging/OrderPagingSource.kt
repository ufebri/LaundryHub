package com.raylabs.laundryhub.core.data.paging

import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.model.sheets.RangeDate
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.repository.LaundryRepository
import com.raylabs.laundryhub.shared.util.Resource

class OrderPagingSource(
    private val repository: LaundryRepository,
    private val filter: FILTER,
    private val rangeDate: RangeDate? = null,
    private val searchQuery: String? = null,
    private val sort: String? = null
) : BasePagingSource<TransactionData>() {

    override suspend fun fetchData(page: Int, size: Int): Resource<List<TransactionData>> {
        return repository.readIncomeTransaction(
            filter = filter,
            rangeDate = rangeDate,
            page = page,
            size = size,
            searchQuery = searchQuery,
            sort = sort
        )
    }
}
