package com.raylabs.laundryhub.core.data.repository

import com.raylabs.laundryhub.core.domain.model.sheets.*
import com.raylabs.laundryhub.core.domain.repository.LaundryRepository
import com.raylabs.laundryhub.shared.network.HttpClientProvider
import com.raylabs.laundryhub.shared.util.Resource
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LaundryRepositoryImpl @Inject constructor() : LaundryRepository {

    private val client: HttpClient = HttpClientProvider.createClient()
    private val baseUrl = "https://laundryhub.up.railway.app/api"

    override suspend fun readSummaryTransaction(): Resource<List<SpreadsheetData>> = safeApiCall {
        client.get("$baseUrl/summary").body()
    }

    override suspend fun readGrossData(): Resource<List<GrossData>> = safeApiCall {
        client.get("$baseUrl/gross").body()
    }

    override suspend fun readIncomeTransaction(filter: FILTER, rangeDate: RangeDate?): Resource<List<TransactionData>> = safeApiCall {
        val response: List<TransactionData> = client.get("$baseUrl/orders").body()
        // Lokal filter agar kompatibel dengan UI lama
        response.filter { transaction ->
            when (filter) {
                FILTER.SHOW_ALL_DATA -> true
                FILTER.TODAY_TRANSACTION_ONLY -> transaction.getTodayIncomeData()
                FILTER.RANGE_TRANSACTION_DATA -> transaction.filterRangeDateData(rangeDate)
                FILTER.SHOW_PAID_DATA -> transaction.isPaidData()
                FILTER.SHOW_UNPAID_DATA -> transaction.isUnpaidData()
                FILTER.SHOW_PAID_BY_QR -> transaction.isQRISData()
                FILTER.SHOW_PAID_BY_CASH -> transaction.isCashData()
            }
        }
    }

    override suspend fun readPackageData(): Resource<List<PackageData>> = safeApiCall {
        client.get("$baseUrl/packages").body()
    }

    override suspend fun addPackage(packageData: PackageData): Resource<Boolean> = safeApiCall {
        client.post("$baseUrl/packages") {
            contentType(ContentType.Application.Json)
            setBody(packageData)
        }
        true
    }

    override suspend fun updatePackage(packageData: PackageData): Resource<Boolean> = safeApiCall {
        client.put("$baseUrl/packages/${packageData.name}") {
            contentType(ContentType.Application.Json)
            setBody(packageData)
        }
        true
    }

    override suspend fun deletePackage(sheetRowIndex: Int): Resource<Boolean> {
        return Resource.Error("Delete by index not supported. Use backend API.")
    }

    override suspend fun readOtherPackage(): Resource<List<String>> = Resource.Success(emptyList())

    override suspend fun getLastOrderId(): Resource<String> = safeApiCall {
        val orders: List<OrderData> = client.get("$baseUrl/orders").body()
        orders.maxByOrNull { it.orderId.toIntOrNull() ?: 0 }?.orderId ?: "0"
    }

    override suspend fun addOrder(order: OrderData): Resource<Boolean> = safeApiCall {
        client.post("$baseUrl/orders") {
            contentType(ContentType.Application.Json)
            setBody(order)
        }
        true
    }

    override suspend fun getOrderById(orderId: String): Resource<TransactionData> = safeApiCall {
        client.get("$baseUrl/orders/$orderId").body()
    }

    override suspend fun updateOrder(order: OrderData): Resource<Boolean> = safeApiCall {
        client.put("$baseUrl/orders/${order.orderId}") {
            contentType(ContentType.Application.Json)
            setBody(order)
        }
        true
    }

    override suspend fun deleteOrder(orderId: String): Resource<Boolean> = safeApiCall {
        client.delete("$baseUrl/orders/$orderId")
        true
    }

    override suspend fun readOutcomeTransaction(): Resource<List<OutcomeData>> = safeApiCall {
        client.get("$baseUrl/outcomes").body()
    }

    override suspend fun addOutcome(outcome: OutcomeData): Resource<Boolean> = safeApiCall {
        client.post("$baseUrl/outcomes") {
            contentType(ContentType.Application.Json)
            setBody(outcome)
        }
        true
    }

    override suspend fun getLastOutcomeId(): Resource<String> = safeApiCall {
        val outcomes: List<OutcomeData> = client.get("$baseUrl/outcomes").body()
        outcomes.maxByOrNull { it.id.toIntOrNull() ?: 0 }?.id ?: "0"
    }

    override suspend fun updateOutcome(outcome: OutcomeData): Resource<Boolean> = safeApiCall {
        client.put("$baseUrl/outcomes/${outcome.id}") {
            contentType(ContentType.Application.Json)
            setBody(outcome)
        }
        true
    }

    override suspend fun getOutcomeById(outcomeId: String): Resource<OutcomeData> = safeApiCall {
        client.get("$baseUrl/outcomes/$outcomeId").body()
    }

    override suspend fun deleteOutcome(outcomeId: String): Resource<Boolean> = safeApiCall {
        client.delete("$baseUrl/outcomes/$outcomeId")
        true
    }

    private suspend fun <T> safeApiCall(block: suspend () -> T): Resource<T> = withContext(Dispatchers.IO) {
        try {
            Resource.Success(block())
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network Error")
        }
    }
}
