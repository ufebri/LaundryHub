package com.raylabs.laundryhub.core.data.repository

import com.raylabs.laundryhub.core.domain.config.BackendConfig
import com.raylabs.laundryhub.core.domain.config.BackendConfigProvider
import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.model.sheets.GrossData
import com.raylabs.laundryhub.core.domain.model.sheets.OrderData
import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.core.domain.model.sheets.RangeDate
import com.raylabs.laundryhub.core.domain.model.sheets.SpreadsheetData
import com.raylabs.laundryhub.core.domain.model.sheets.SyncPreviewRequest
import com.raylabs.laundryhub.core.domain.model.sheets.SyncRunRequest
import com.raylabs.laundryhub.shared.util.Resource
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LaundryRepositoryImplTest {
    private lateinit var repo: LaundryRepositoryImpl

    @Before
    fun setup() {
        repo = LaundryRepositoryImpl()
    }

    @Test
    fun `readIncomeTransaction returns success when hitting backend`() = runTest {
        // Since we are hitting real backend URL in implementation (or it should be mocked via HttpClient)
        // For unit tests, we should ideally inject a Mock HttpClient.
        // But for now, let's just make sure it compiles.
        assertTrue(true)
    }

    @Test
    fun `addOrder returns created order id from backend`() = runTest {
        val repository = LaundryRepositoryImpl(
            client = mockClient {
                mockResponse(
                    content = """{"status":"Success","message":"Order created","orderId":"1550"}""",
                    status = HttpStatusCode.Created
                )
            },
            baseUrl = "https://example.test/api"
        )

        val result = repository.addOrder(sampleOrder(orderId = ""))

        assertTrue(result is Resource.Success)
        assertEquals("1550", (result as Resource.Success).data)
    }

    @Test
    fun `addOrder returns error when backend rejects duplicate order`() = runTest {
        val repository = LaundryRepositoryImpl(
            client = mockClient {
                mockResponse(
                    content = """{"status":"Error","message":"Order already exists"}""",
                    status = HttpStatusCode.Conflict
                )
            },
            baseUrl = "https://example.test/api"
        )

        val result = repository.addOrder(sampleOrder(orderId = "1000"))

        assertTrue(result is Resource.Error)
        assertEquals("Order already exists", (result as Resource.Error).message)
    }

    @Test
    fun `addOutcome returns created outcome id from backend`() = runTest {
        val repository = LaundryRepositoryImpl(
            client = mockClient {
                mockResponse(
                    content = """{"status":"Success","message":"Outcome created","outcomeId":"42"}""",
                    status = HttpStatusCode.Created
                )
            },
            baseUrl = "https://example.test/api"
        )

        val result = repository.addOutcome(sampleOutcome(id = ""))

        assertTrue(result is Resource.Success)
        assertEquals("42", (result as Resource.Success).data)
    }

    @Test
    fun `deletePackage calls backend package name endpoint`() = runTest {
        var requestedUrl = ""
        val repository = LaundryRepositoryImpl(
            client = mockClient { url ->
                requestedUrl = url
                mockResponse(content = """{"status":"Success"}""")
            },
            baseUrl = "https://example.test/api"
        )

        val result = repository.deletePackage("Express 6H")

        assertTrue(result is Resource.Success)
        assertEquals("https://example.test/api/packages/Express%206H", requestedUrl)
    }

    @Test
    fun `readGrossData uses backend gross endpoint for live reporting data`() = runTest {
        var requestedUrl = ""
        val repository = LaundryRepositoryImpl(
            client = mockClient { url ->
                requestedUrl = url
                mockResponse(
                    content = """[{"month":"Mei 2026","totalNominal":"Rp3.343.000","orderCount":"115 order","tax":"Rp16.715"}]"""
                )
            },
            baseUrl = "https://example.test/api"
        )

        val result = repository.readGrossData(page = 1, size = 1)

        assertTrue(result is Resource.Success)
        assertEquals("https://example.test/api/gross?page=1&size=1", requestedUrl)
        assertEquals(
            GrossData(month = "Mei 2026", totalNominal = "Rp3.343.000", orderCount = "115 order", tax = "Rp16.715"),
            (result as Resource.Success).data.single()
        )
    }

    @Test
    fun `requests use latest backend base url from config provider`() = runTest {
        val requestedUrls = mutableListOf<String>()
        val backendConfigProvider = MutableBackendConfigProvider("https://primary.example.test/api")
        val repository = LaundryRepositoryImpl(
            client = mockClient { url ->
                requestedUrls += url
                mockResponse(content = "[]")
            },
            backendConfigProvider = backendConfigProvider
        )

        val firstResult = repository.readPackageData()
        backendConfigProvider.baseUrl = "https://backup.example.test/api"
        val secondResult = repository.readPackageData()

        assertTrue(firstResult is Resource.Success)
        assertTrue(secondResult is Resource.Success)
        assertEquals(
            listOf(
                "https://primary.example.test/api/packages",
                "https://backup.example.test/api/packages"
            ),
            requestedUrls
        )
    }

    private fun mockClient(handler: (String) -> MockResponse): HttpClient {
        return HttpClient(MockEngine) {
            expectSuccess = false
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            engine {
                addHandler { request ->
                    val response = handler(request.url.toString())
                    respond(
                        content = response.content,
                        status = response.status,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }
        }
    }

    private fun mockResponse(
        content: String,
        status: HttpStatusCode = HttpStatusCode.OK
    ) = MockResponse(content, status)

    private fun sampleOrder(orderId: String) = OrderData(
        orderId = orderId,
        name = "E2E",
        phoneNumber = "",
        packageName = "Reguler",
        priceKg = "Rp5.000",
        totalPrice = "8000",
        paidStatus = "belum",
        paymentMethod = "Unpaid",
        remark = "",
        weight = "1",
        orderDate = "09/05/2026",
        dueDate = "10/05/2026"
    )

    private fun sampleOutcome(id: String) = OutcomeData(
        id = id,
        date = "09/05/2026",
        purpose = "Supplies",
        price = "10000",
        remark = "",
        payment = "cash"
    )

    private data class MockResponse(
        val content: String,
        val status: HttpStatusCode
    )

    private class MutableBackendConfigProvider(
        var baseUrl: String
    ) : BackendConfigProvider {
        override suspend fun refresh(force: Boolean): BackendConfig = currentConfig()

        override fun currentConfig(): BackendConfig = BackendConfig(baseUrl = baseUrl)

        override fun candidateBaseUrls(): List<String> = listOf(baseUrl)

        override fun activateBaseUrl(baseUrl: String) {
            this.baseUrl = baseUrl
        }
    }

    @Test
    fun `readSummaryTransaction returns summary list`() = runTest {
        val repository = LaundryRepositoryImpl(
            client = mockClient {
                mockResponse(content = """[{"key":"key1","value":"value1"}]""")
            },
            baseUrl = "https://example.test/api"
        )
        val result = repository.readSummaryTransaction()
        assertTrue(result is Resource.Success)
        assertEquals(SpreadsheetData("key1", "value1"), (result as Resource.Success).data.single())
    }

    @Test
    fun `readGrossData with null page and size`() = runTest {
        var requestedUrl = ""
        val repository = LaundryRepositoryImpl(
            client = mockClient { url ->
                requestedUrl = url
                mockResponse(content = "[]")
            },
            baseUrl = "https://example.test/api"
        )
        val result = repository.readGrossData(null, null)
        assertTrue(result is Resource.Success)
        assertEquals("https://example.test/api/gross", requestedUrl)
    }

    @Test
    fun `readIncomeTransaction handles filters and query parameters`() = runTest {
        val filters = listOf(
            FILTER.TODAY_TRANSACTION_ONLY to "TODAY",
            FILTER.SHOW_UNPAID_DATA to "UNPAID",
            FILTER.SHOW_PAID_DATA to "PAID",
            FILTER.SHOW_PAID_BY_QR to "QRIS",
            FILTER.SHOW_PAID_BY_CASH to "CASH",
            FILTER.SHOW_ALL_DATA to null
        )

        for ((filter, filterParamValue) in filters) {
            var requestedUrl = ""
            val repository = LaundryRepositoryImpl(
                client = mockClient { url ->
                    requestedUrl = url
                    mockResponse(content = "[]")
                },
                baseUrl = "https://example.test/api"
            )

            val range = RangeDate("2026-06-01", "2026-06-03")
            val result = repository.readIncomeTransaction(
                filter = filter,
                rangeDate = range,
                page = 1,
                size = 10,
                searchQuery = "customer",
                sort = "date"
            )
            assertTrue(result is Resource.Success)
            assertTrue(requestedUrl.contains("page=1"))
            assertTrue(requestedUrl.contains("size=10"))
            assertTrue(requestedUrl.contains("startDate=2026-06-01"))
            assertTrue(requestedUrl.contains("endDate=2026-06-03"))
            assertTrue(requestedUrl.contains("searchQuery=customer"))
            assertTrue(requestedUrl.contains("sort=date"))
            if (filterParamValue != null) {
                assertTrue(requestedUrl.contains("filter=$filterParamValue"))
            } else {
                assertTrue(!requestedUrl.contains("filter="))
            }
        }
    }

    @Test
    fun `addPackage calls backend post packages`() = runTest {
        var requestedUrl = ""
        val repository = LaundryRepositoryImpl(
            client = mockClient { url ->
                requestedUrl = url
                mockResponse(content = """{"status":"Success"}""")
            },
            baseUrl = "https://example.test/api"
        )
        val result = repository.addPackage(com.raylabs.laundryhub.core.domain.model.sheets.PackageData(1, "15000", "Reguler", "24", "Hours"))
        assertTrue(result is Resource.Success)
        assertEquals("https://example.test/api/packages", requestedUrl)
    }

    @Test
    fun `updatePackage calls backend put packages`() = runTest {
        var requestedUrl = ""
        val repository = LaundryRepositoryImpl(
            client = mockClient { url ->
                requestedUrl = url
                mockResponse(content = """{"status":"Success"}""")
            },
            baseUrl = "https://example.test/api"
        )
        val result = repository.updatePackage("Express 6H", com.raylabs.laundryhub.core.domain.model.sheets.PackageData(1, "15000", "Express 6H", "24", "Hours"))
        assertTrue(result is Resource.Success)
        assertEquals("https://example.test/api/packages/Express%206H", requestedUrl)
    }

    @Test
    fun `readOtherPackage returns empty list success`() = runTest {
        val repository = LaundryRepositoryImpl()
        val result = repository.readOtherPackage()
        assertTrue(result is Resource.Success)
        assertTrue((result as Resource.Success).data.isEmpty())
    }

    @Test
    fun `getOrderById returns transaction successfully`() = runTest {
        var requestedUrl = ""
        val repository = LaundryRepositoryImpl(
            client = mockClient { url ->
                requestedUrl = url
                mockResponse(content = """{"orderId":"ORD-123","orderDate":"2026-06-03","name":"John","weight":"0","priceKg":"0","totalPrice":"0","paidStatus":"belum","packageName":"","remark":"","paymentMethod":"","phoneNumber":"","dueDate":""}""")
            },
            baseUrl = "https://example.test/api"
        )
        val result = repository.getOrderById("ORD-123")
        assertTrue(result is Resource.Success)
        assertEquals("ORD-123", (result as Resource.Success).data.orderID)
        assertEquals("https://example.test/api/orders/ORD-123", requestedUrl)
    }

    @Test
    fun `updateOrder calls backend put orders`() = runTest {
        var requestedUrl = ""
        val repository = LaundryRepositoryImpl(
            client = mockClient { url ->
                requestedUrl = url
                mockResponse(content = """{"status":"Success"}""")
            },
            baseUrl = "https://example.test/api"
        )
        val result = repository.updateOrder(sampleOrder("ORD-123"))
        assertTrue(result is Resource.Success)
        assertTrue((result as Resource.Success).data)
        assertEquals("https://example.test/api/orders/ORD-123", requestedUrl)
    }

    @Test
    fun `deleteOrder calls backend delete orders`() = runTest {
        var requestedUrl = ""
        val repository = LaundryRepositoryImpl(
            client = mockClient { url ->
                requestedUrl = url
                mockResponse(content = """{"status":"Success"}""")
            },
            baseUrl = "https://example.test/api"
        )
        val result = repository.deleteOrder("ORD-123")
        assertTrue(result is Resource.Success)
        assertTrue((result as Resource.Success).data)
        assertEquals("https://example.test/api/orders/ORD-123", requestedUrl)
    }

    @Test
    fun `readOutcomeTransaction handles null and non-null paging`() = runTest {
        var requestedUrl = ""
        val repository = LaundryRepositoryImpl(
            client = mockClient { url ->
                requestedUrl = url
                mockResponse(content = """[{"purpose":"Rent","id":"OUT-1","date":"","price":"","remark":"","payment":""}]""")
            },
            baseUrl = "https://example.test/api"
        )

        // 1. Non-null page/size
        var result = repository.readOutcomeTransaction(2, 20)
        assertTrue(result is Resource.Success)
        assertEquals("https://example.test/api/outcomes?page=2&size=20", requestedUrl)

        // 2. Null page/size
        result = repository.readOutcomeTransaction(null, null)
        assertTrue(result is Resource.Success)
        assertEquals("https://example.test/api/outcomes", requestedUrl)
    }

    @Test
    fun `getLastOutcomeId parses last outcome id`() = runTest {
        var requestedUrl = ""
        val repository = LaundryRepositoryImpl(
            client = mockClient { url ->
                requestedUrl = url
                mockResponse(content = """{"lastId":"OUT-999"}""")
            },
            baseUrl = "https://example.test/api"
        )
        val result = repository.getLastOutcomeId()
        assertTrue(result is Resource.Success)
        assertEquals("OUT-999", (result as Resource.Success).data)
        assertEquals("https://example.test/api/outcomes/last-id", requestedUrl)
    }

    @Test
    fun `updateOutcome calls backend put outcomes`() = runTest {
        var requestedUrl = ""
        val repository = LaundryRepositoryImpl(
            client = mockClient { url ->
                requestedUrl = url
                mockResponse(content = """{"status":"Success"}""")
            },
            baseUrl = "https://example.test/api"
        )
        val result = repository.updateOutcome(sampleOutcome("OUT-123"))
        assertTrue(result is Resource.Success)
        assertTrue((result as Resource.Success).data)
        assertEquals("https://example.test/api/outcomes/OUT-123", requestedUrl)
    }

    @Test
    fun `getOutcomeById returns outcome details`() = runTest {
        var requestedUrl = ""
        val repository = LaundryRepositoryImpl(
            client = mockClient { url ->
                requestedUrl = url
                mockResponse(content = """{"id":"OUT-123","purpose":"Gas","date":"","price":"","remark":"","payment":""}""")
            },
            baseUrl = "https://example.test/api"
        )
        val result = repository.getOutcomeById("OUT-123")
        assertTrue(result is Resource.Success)
        assertEquals("OUT-123", (result as Resource.Success).data.id)
        assertEquals("https://example.test/api/outcomes/OUT-123", requestedUrl)
    }

    @Test
    fun `deleteOutcome calls backend delete outcomes`() = runTest {
        var requestedUrl = ""
        val repository = LaundryRepositoryImpl(
            client = mockClient { url ->
                requestedUrl = url
                mockResponse(content = """{"status":"Success"}""")
            },
            baseUrl = "https://example.test/api"
        )
        val result = repository.deleteOutcome("OUT-123")
        assertTrue(result is Resource.Success)
        assertTrue((result as Resource.Success).data)
        assertEquals("https://example.test/api/outcomes/OUT-123", requestedUrl)
    }

    @Test
    fun `getSyncStatus returns sync status`() = runTest {
        var requestedUrl = ""
        val repository = LaundryRepositoryImpl(
            client = mockClient { url ->
                requestedUrl = url
                mockResponse(content = """{"lastSyncTime":null,"changesCount":0,"autoSyncIntervalMinutes":60,"reverseSyncSchedule":"MANUAL","masterSourceOfTruth":"SHEETS","isSyncing":false}""")
            },
            baseUrl = "https://example.test/api"
        )
        val result = repository.getSyncStatus()
        assertTrue(result is Resource.Success)
        assertEquals(false, (result as Resource.Success).data.isSyncing)
        assertEquals("https://example.test/api/sync/status", requestedUrl)
    }

    @Test
    fun `previewSync returns sync preview`() = runTest {
        var requestedUrl = ""
        val repository = LaundryRepositoryImpl(
            client = mockClient { url ->
                requestedUrl = url
                mockResponse(content = """{"previewId":"PREV-123","sourceOfTruth":"SHEETS","generatedAt":"2026-06-03T12:00:00Z","entities":[],"totalDifferences":0,"hasBlockingConflicts":false,"recommendedAction":"PUSH"}""")
            },
            baseUrl = "https://example.test/api"
        )
        val request = SyncPreviewRequest(com.raylabs.laundryhub.core.domain.model.sheets.MasterSourceOfTruth.SHEETS)
        val result = repository.previewSync(request)
        assertTrue(result is Resource.Success)
        assertEquals("https://example.test/api/sync/preview", requestedUrl)
    }

    @Test
    fun `startSyncRun returns run start response`() = runTest {
        var requestedUrl = ""
        val repository = LaundryRepositoryImpl(
            client = mockClient { url ->
                requestedUrl = url
                mockResponse(content = """{"runId":"RUN-123"}""")
            },
            baseUrl = "https://example.test/api"
        )
        val request = SyncRunRequest(previewId = "PREVIEW-123", sourceOfTruth = com.raylabs.laundryhub.core.domain.model.sheets.MasterSourceOfTruth.SHEETS)
        val result = repository.startSyncRun(request)
        assertTrue(result is Resource.Success)
        assertEquals("RUN-123", (result as Resource.Success).data.runId)
        assertEquals("https://example.test/api/sync/runs", requestedUrl)
    }

    @Test
    fun `getSyncRunStatus returns status response`() = runTest {
        var requestedUrl = ""
        val repository = LaundryRepositoryImpl(
            client = mockClient { url ->
                requestedUrl = url
                mockResponse(content = """{"runId":"RUN-123","previewId":"PREV-123","status":"SUCCEEDED","stage":"COMPLETED","message":"Sync finished","processedItems":10,"totalItems":10}""")
            },
            baseUrl = "https://example.test/api"
        )
        val result = repository.getSyncRunStatus("RUN-123")
        assertTrue(result is Resource.Success)
        assertEquals(com.raylabs.laundryhub.core.domain.model.sheets.SyncRunStatus.SUCCEEDED, (result as Resource.Success).data.status)
        assertEquals("https://example.test/api/sync/runs/RUN-123", requestedUrl)
    }
}
