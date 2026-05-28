package com.raylabs.laundryhub.core.domain.model.sheets

import kotlinx.serialization.Serializable

@Serializable
enum class ReverseSyncSchedule(val hours: List<Int>) {
    DEFAULT_23(listOf(23)),
    TWICE_DAILY(listOf(12, 23)),
    MANUAL(emptyList())
}

@Serializable
enum class MasterSourceOfTruth {
    SHEETS, SUPABASE, BOTH
}

@Serializable
enum class SyncRunStatus {
    PENDING,
    RUNNING,
    SUCCEEDED,
    FAILED,
    PARTIAL
}

@Serializable
enum class SyncRunStage {
    IDLE,
    CHECKING,
    PLANNING,
    APPLYING_ORDERS,
    APPLYING_OUTCOMES,
    APPLYING_PACKAGES,
    APPLYING_GROSS,
    APPLYING_SUMMARY,
    CLEANING_DELETES,
    VERIFYING,
    COMPLETED
}

@Serializable
data class SyncStatusResponse(
    val lastSyncTime: String?,
    val changesCount: Int,
    val autoSyncIntervalMinutes: Int,
    val reverseSyncSchedule: ReverseSyncSchedule,
    val masterSourceOfTruth: MasterSourceOfTruth = MasterSourceOfTruth.SHEETS,
    val isSyncing: Boolean = false,
    val lastSyncStatus: String = "UNKNOWN",
    val lastSyncError: String? = null,
    val pendingPushCount: Int = 0,
    val pendingDeleteCount: Int = 0,
    val nextScheduledPushTime: String? = null,
    val dataDifferenceCount: Int = 0,
    val hasDataDifferences: Boolean = false,
    val syncQueueState: SyncQueueState = SyncQueueState.IDLE
)

@Serializable
enum class SyncQueueState {
    IDLE,
    PENDING_PUSH,
    DATA_DIFFERENCES,
    PENDING_PUSH_AND_DATA_DIFFERENCES,
    UNAVAILABLE
}

@Serializable
data class SyncConfigUpdateRequest(
    val autoSyncIntervalMinutes: Int? = null,
    val reverseSyncSchedule: ReverseSyncSchedule? = null,
    val masterSourceOfTruth: MasterSourceOfTruth? = null
)

@Serializable
data class SyncPreviewRequest(
    val sourceOfTruth: MasterSourceOfTruth? = null
)

@Serializable
data class SyncEntityPreview(
    val entity: String,
    val onlyInSheets: Int,
    val onlyInDatabase: Int,
    val changedRows: Int,
    val duplicateKeys: Int,
    val pendingDeletes: Int,
    val suspiciousRows: Int = 0,
    val unresolvedConflicts: Int = 0
) {
    val totalDifferences: Int
        get() = onlyInSheets + onlyInDatabase + changedRows + duplicateKeys + pendingDeletes + unresolvedConflicts
}

@Serializable
data class SyncPreviewResponse(
    val previewId: String,
    val sourceOfTruth: MasterSourceOfTruth,
    val generatedAt: String,
    val entities: List<SyncEntityPreview>,
    val totalDifferences: Int,
    val hasBlockingConflicts: Boolean,
    val recommendedAction: String
)

@Serializable
data class SyncRunRequest(
    val previewId: String,
    val sourceOfTruth: MasterSourceOfTruth? = null
)

@Serializable
data class SyncRunStartResponse(
    val runId: String
)

@Serializable
data class SyncRunStatusResponse(
    val runId: String,
    val previewId: String,
    val status: SyncRunStatus,
    val stage: SyncRunStage,
    val message: String,
    val processedItems: Int,
    val totalItems: Int,
    val currentEntity: String? = null,
    val lastError: String? = null,
    val finalDifferenceCount: Int? = null
)
