package com.raylabs.laundryhub.core.data.repository

import com.raylabs.laundryhub.BuildConfig
import com.raylabs.laundryhub.core.data.config.StaticBackendConfigProvider
import com.raylabs.laundryhub.core.domain.config.BackendConfigProvider
import com.raylabs.laundryhub.core.domain.model.sheets.CreateOrderResponse
import com.raylabs.laundryhub.core.domain.model.sheets.CreateOutcomeResponse
import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.model.sheets.GrossData
import com.raylabs.laundryhub.core.domain.model.sheets.OrderData
import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.core.domain.model.sheets.PackageData
import com.raylabs.laundryhub.core.domain.model.sheets.RangeDate
import com.raylabs.laundryhub.core.domain.model.sheets.SpreadsheetData
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.repository.LaundryRepository
import com.raylabs.laundryhub.shared.network.HttpClientProvider
import com.raylabs.laundryhub.shared.util.Resource
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class LaundryRepositoryImpl(
    private val backendConfigProvider: BackendConfigProvider =
        StaticBackendConfigProvider(BuildConfig.BASE_URL)
) : LaundryRepository {

    private var client: HttpClient = HttpClientProvider.createClient()

    internal constructor(client: HttpClient, baseUrl: String) : this(
        StaticBackendConfigProvider(baseUrl)
    ) {
        this.client = client
    }

    internal constructor(
        client: HttpClient,
        backendConfigProvider: BackendConfigProvider
    ) : this(backendConfigProvider) {
        this.client = client
    }

    override suspend fun readSummaryTransaction(): Resource<List<SpreadsheetData>> = safeApiCall {
        client.get(endpoint("summary")).body()
    }

    override suspend fun readGrossData(page: Int?, size: Int?): Resource<List<GrossData>> = safeApiCall {
        val url = if (page != null && size != null) {
            endpoint("gross?page=$page&size=$size")
        } else {
            endpoint("gross")
        }
        client.get(url).body()
    }

    override suspend fun readIncomeTransaction(filter: FILTER, rangeDate: RangeDate?, page: Int?, size: Int?, searchQuery: String?, sort: String?): Resource<List<TransactionData>> = safeApiCall {
        val filterParam = when (filter) {
            FILTER.TODAY_TRANSACTION_ONLY -> "TODAY"
            FILTER.SHOW_UNPAID_DATA -> "UNPAID"
            FILTER.SHOW_PAID_DATA -> "PAID"
            FILTER.SHOW_PAID_BY_QR -> "QRIS"
            FILTER.SHOW_PAID_BY_CASH -> "CASH"
            else -> null
        }
        
        client.get(endpoint("orders")) {
            page?.let { parameter("page", it) }
            size?.let { parameter("size", it) }
            filterParam?.let { parameter("filter", it) }
            rangeDate?.startDate?.let { parameter("startDate", it) }
            rangeDate?.endDate?.let { parameter("endDate", it) }
            searchQuery?.takeIf { it.isNotBlank() }?.let { parameter("searchQuery", it) }
            sort?.takeIf { it.isNotBlank() }?.let { parameter("sort", it) }
        }.body()
    }

    override suspend fun readPackageData(): Resource<List<PackageData>> = safeApiCall {
        client.get(endpoint("packages")).body()
    }

    override suspend fun addPackage(packageData: PackageData): Resource<Boolean> = safeApiCall {
        client.post(endpoint("packages")) {
            contentType(ContentType.Application.Json)
            setBody(packageData)
        }.requireSuccessfulResponse()
        true
    }

    override suspend fun updatePackage(packageName: String, packageData: PackageData): Resource<Boolean> = safeApiCall {
        client.put(endpoint("packages/${packageName.encodeURLPathPart()}")) {
            contentType(ContentType.Application.Json)
            setBody(packageData)
        }.requireSuccessfulResponse()
        true
    }

    override suspend fun deletePackage(packageName: String): Resource<Boolean> = safeApiCall {
        client.delete(endpoint("packages/${packageName.encodeURLPathPart()}")).requireSuccessfulResponse()
        true
    }

    override suspend fun readOtherPackage(): Resource<List<String>> = Resource.Success(emptyList())

    override suspend fun addOrder(order: OrderData): Resource<String> = safeApiCall {
        val response = client.post(endpoint("orders")) {
            contentType(ContentType.Application.Json)
            setBody(order)
        }
        response.requireSuccessfulResponse()
        response.body<CreateOrderResponse>().orderId
    }

    override suspend fun getOrderById(orderId: String): Resource<TransactionData> = safeApiCall {
        client.get(endpoint("orders/$orderId")).body()
    }

    override suspend fun updateOrder(order: OrderData): Resource<Boolean> = safeApiCall {
        client.put(endpoint("orders/${order.orderId}")) {
            contentType(ContentType.Application.Json)
            setBody(order)
        }.requireSuccessfulResponse()
        true
    }

    override suspend fun deleteOrder(orderId: String): Resource<Boolean> = safeApiCall {
        client.delete(endpoint("orders/$orderId")).requireSuccessfulResponse()
        true
    }

    override suspend fun readOutcomeTransaction(page: Int?, size: Int?): Resource<List<OutcomeData>> = safeApiCall {
        val url = if (page != null && size != null) {
            endpoint("outcomes?page=$page&size=$size")
        } else {
            endpoint("outcomes")
        }
        client.get(url).body()
    }

    override suspend fun addOutcome(outcome: OutcomeData): Resource<String> = safeApiCall {
        val response = client.post(endpoint("outcomes")) {
            contentType(ContentType.Application.Json)
            setBody(outcome)
        }
        response.requireSuccessfulResponse()
        response.body<CreateOutcomeResponse>().outcomeId
    }

    override suspend fun getLastOutcomeId(): Resource<String> = safeApiCall {
        parseLastId(client.get(endpoint("outcomes/last-id")).bodyAsText())
    }

    override suspend fun updateOutcome(outcome: OutcomeData): Resource<Boolean> = safeApiCall {
        client.put(endpoint("outcomes/${outcome.id}")) {
            contentType(ContentType.Application.Json)
            setBody(outcome)
        }.requireSuccessfulResponse()
        true
    }

    override suspend fun getOutcomeById(outcomeId: String): Resource<OutcomeData> = safeApiCall {
        client.get(endpoint("outcomes/$outcomeId")).body()
    }

    override suspend fun deleteOutcome(outcomeId: String): Resource<Boolean> = safeApiCall {
        client.delete(endpoint("outcomes/$outcomeId")).requireSuccessfulResponse()
        true
    }

    private suspend fun endpoint(path: String): String {
        return "${backendConfigProvider.currentBaseUrl().trimEnd('/')}/${path.trimStart('/')}"
    }

    private suspend fun <T> safeApiCall(block: suspend () -> T): Resource<T> = withContext(Dispatchers.IO) {
        try {
            Resource.Success(block())
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network Error")
        }
    }

}

private val repositoryJson = Json { ignoreUnknownKeys = true }

private suspend fun HttpResponse.requireSuccessfulResponse() {
    if (status.value in 200..299) return

    val responseBody = bodyAsText()
    error(extractApiErrorMessage(responseBody, status))
}

private fun extractApiErrorMessage(responseBody: String, status: HttpStatusCode): String {
    val apiMessage = runCatching {
        repositoryJson
            .parseToJsonElement(responseBody)
            .jsonObject["message"]
            ?.jsonPrimitive
            ?.contentOrNull
    }.getOrNull()

    return apiMessage?.takeIf { it.isNotBlank() }
        ?: "HTTP ${status.value} ${status.description}"
}

private fun parseLastId(responseBody: String): String {
    return repositoryJson
        .parseToJsonElement(responseBody)
        .jsonObject["lastId"]
        ?.jsonPrimitive
        ?.contentOrNull
        ?: error("Missing lastId in response")
}
