package com.raylabs.laundryhub.core.domain.model.sheets

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncModelsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testReverseSyncScheduleEnum() {
        assertEquals(listOf(23), ReverseSyncSchedule.DEFAULT_23.hours)
        assertEquals(listOf(12, 23), ReverseSyncSchedule.TWICE_DAILY.hours)
        assertEquals(emptyList(), ReverseSyncSchedule.MANUAL.hours)
        
        // Serialization check
        val serialized = json.encodeToString(ReverseSyncSchedule.DEFAULT_23)
        assertEquals("\"DEFAULT_23\"", serialized)
        val deserialized = json.decodeFromString<ReverseSyncSchedule>(serialized)
        assertEquals(ReverseSyncSchedule.DEFAULT_23, deserialized)
    }

    @Test
    fun testMasterSourceOfTruthEnum() {
        val serialized = json.encodeToString(MasterSourceOfTruth.SHEETS)
        assertEquals("\"SHEETS\"", serialized)
        val deserialized = json.decodeFromString<MasterSourceOfTruth>(serialized)
        assertEquals(MasterSourceOfTruth.SHEETS, deserialized)
    }

    @Test
    fun testSyncRunStatusEnum() {
        val serialized = json.encodeToString(SyncRunStatus.SUCCEEDED)
        assertEquals("\"SUCCEEDED\"", serialized)
        val deserialized = json.decodeFromString<SyncRunStatus>(serialized)
        assertEquals(SyncRunStatus.SUCCEEDED, deserialized)
    }

    @Test
    fun testSyncRunStageEnum() {
        val serialized = json.encodeToString(SyncRunStage.APPLYING_ORDERS)
        assertEquals("\"APPLYING_ORDERS\"", serialized)
        val deserialized = json.decodeFromString<SyncRunStage>(serialized)
        assertEquals(SyncRunStage.APPLYING_ORDERS, deserialized)
    }

    @Test
    fun testSyncQueueStateEnum() {
        val serialized = json.encodeToString(SyncQueueState.DATA_DIFFERENCES)
        assertEquals("\"DATA_DIFFERENCES\"", serialized)
        val deserialized = json.decodeFromString<SyncQueueState>(serialized)
        assertEquals(SyncQueueState.DATA_DIFFERENCES, deserialized)
    }

    @Test
    fun testSyncRowChangeTypeEnum() {
        val serialized = json.encodeToString(SyncRowChangeType.ONLY_IN_DATABASE)
        assertEquals("\"ONLY_IN_DATABASE\"", serialized)
        val deserialized = json.decodeFromString<SyncRowChangeType>(serialized)
        assertEquals(SyncRowChangeType.ONLY_IN_DATABASE, deserialized)
    }

    @Test
    fun testSyncStatusResponse() {
        val response = SyncStatusResponse(
            lastSyncTime = "2026-06-01T12:00:00Z",
            changesCount = 5,
            autoSyncIntervalMinutes = 15,
            reverseSyncSchedule = ReverseSyncSchedule.TWICE_DAILY,
            masterSourceOfTruth = MasterSourceOfTruth.BOTH,
            isSyncing = true,
            lastSyncStatus = "SUCCESS",
            lastSyncError = "No error",
            pendingPushCount = 2,
            pendingDeleteCount = 1,
            nextScheduledPushTime = "2026-06-01T12:15:00Z",
            dataDifferenceCount = 4,
            hasDataDifferences = true,
            reportingDifferenceCount = 3,
            hasReportingDifferences = true,
            syncQueueState = SyncQueueState.PENDING_PUSH_AND_DATA_DIFFERENCES
        )

        val serialized = json.encodeToString(response)
        val deserialized = json.decodeFromString<SyncStatusResponse>(serialized)

        assertEquals(response, deserialized)
        assertEquals("2026-06-01T12:00:00Z", deserialized.lastSyncTime)
        assertEquals(5, deserialized.changesCount)
        assertEquals(15, deserialized.autoSyncIntervalMinutes)
        assertEquals(ReverseSyncSchedule.TWICE_DAILY, deserialized.reverseSyncSchedule)
        assertEquals(MasterSourceOfTruth.BOTH, deserialized.masterSourceOfTruth)
        assertTrue(deserialized.isSyncing)
        assertEquals("SUCCESS", deserialized.lastSyncStatus)
        assertEquals("No error", deserialized.lastSyncError)
        assertEquals(2, deserialized.pendingPushCount)
        assertEquals(1, deserialized.pendingDeleteCount)
        assertEquals("2026-06-01T12:15:00Z", deserialized.nextScheduledPushTime)
        assertEquals(4, deserialized.dataDifferenceCount)
        assertTrue(deserialized.hasDataDifferences)
        assertEquals(3, deserialized.reportingDifferenceCount)
        assertTrue(deserialized.hasReportingDifferences)
        assertEquals(SyncQueueState.PENDING_PUSH_AND_DATA_DIFFERENCES, deserialized.syncQueueState)
    }

    @Test
    fun testSyncConfigUpdateRequest() {
        val request = SyncConfigUpdateRequest(
            autoSyncIntervalMinutes = 30,
            reverseSyncSchedule = ReverseSyncSchedule.MANUAL,
            masterSourceOfTruth = MasterSourceOfTruth.SUPABASE
        )
        val serialized = json.encodeToString(request)
        val deserialized = json.decodeFromString<SyncConfigUpdateRequest>(serialized)
        assertEquals(request, deserialized)
    }

    @Test
    fun testSyncPreviewRequest() {
        val request = SyncPreviewRequest(sourceOfTruth = MasterSourceOfTruth.BOTH)
        val serialized = json.encodeToString(request)
        val deserialized = json.decodeFromString<SyncPreviewRequest>(serialized)
        assertEquals(request, deserialized)
    }

    @Test
    fun testSyncEntityPreview() {
        val fieldDiff = SyncFieldDifference("name", "John", "Doe")
        val rowDiff = SyncRowDifference("key-1", SyncRowChangeType.CHANGED, listOf(fieldDiff))
        val preview = SyncEntityPreview(
            entity = "orders",
            onlyInSheets = 1,
            onlyInDatabase = 2,
            changedRows = 3,
            duplicateKeys = 4,
            pendingDeletes = 5,
            suspiciousRows = 6,
            unresolvedConflicts = 7,
            onlyInSheetKeys = listOf("k1"),
            onlyInDatabaseKeys = listOf("k2"),
            changedRowKeys = listOf("k3"),
            duplicateKeyValues = listOf("k4"),
            suspiciousKeyValues = listOf("k5"),
            rowDifferences = listOf(rowDiff)
        )

        assertEquals(22, preview.totalDifferences)

        val serialized = json.encodeToString(preview)
        val deserialized = json.decodeFromString<SyncEntityPreview>(serialized)
        assertEquals(preview, deserialized)
        assertEquals(rowDiff, deserialized.rowDifferences.first())
        assertEquals(fieldDiff, deserialized.rowDifferences.first().fieldDifferences.first())
    }

    @Test
    fun testSyncPreviewResponse() {
        val preview = SyncEntityPreview(
            entity = "orders",
            onlyInSheets = 1,
            onlyInDatabase = 1,
            changedRows = 1,
            duplicateKeys = 1,
            pendingDeletes = 1,
            unresolvedConflicts = 1
        )
        val response = SyncPreviewResponse(
            previewId = "prev-123",
            sourceOfTruth = MasterSourceOfTruth.BOTH,
            generatedAt = "2026-06-01T12:00:00Z",
            entities = listOf(preview),
            totalDifferences = 6,
            hasBlockingConflicts = false,
            recommendedAction = "SYNC",
            appOwnedDifferenceCount = 2,
            reportingDifferenceCount = 4
        )

        val serialized = json.encodeToString(response)
        val deserialized = json.decodeFromString<SyncPreviewResponse>(serialized)
        assertEquals(response, deserialized)
    }

    @Test
    fun testSyncRunRequest() {
        val request = SyncRunRequest("prev-123", MasterSourceOfTruth.BOTH)
        val serialized = json.encodeToString(request)
        val deserialized = json.decodeFromString<SyncRunRequest>(serialized)
        assertEquals(request, deserialized)
    }

    @Test
    fun testSyncRunStartResponse() {
        val response = SyncRunStartResponse("run-123")
        val serialized = json.encodeToString(response)
        val deserialized = json.decodeFromString<SyncRunStartResponse>(serialized)
        assertEquals(response, deserialized)
    }

    @Test
    fun testSyncRunStatusResponse() {
        val response = SyncRunStatusResponse(
            runId = "run-123",
            previewId = "prev-123",
            status = SyncRunStatus.RUNNING,
            stage = SyncRunStage.APPLYING_GROSS,
            message = "Applying gross data...",
            processedItems = 10,
            totalItems = 20,
            currentEntity = "gross",
            lastError = null,
            finalDifferenceCount = 0
        )

        val serialized = json.encodeToString(response)
        val deserialized = json.decodeFromString<SyncRunStatusResponse>(serialized)
        assertEquals(response, deserialized)
    }
}
