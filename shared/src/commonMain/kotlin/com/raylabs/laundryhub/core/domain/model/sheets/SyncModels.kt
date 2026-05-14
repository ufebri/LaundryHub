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
data class SyncStatusResponse(
    val lastSyncTime: String?,
    val changesCount: Int,
    val autoSyncIntervalMinutes: Int,
    val reverseSyncSchedule: ReverseSyncSchedule,
    val masterSourceOfTruth: MasterSourceOfTruth = MasterSourceOfTruth.SHEETS,
    val isSyncing: Boolean = false,
    val lastSyncStatus: String = "UNKNOWN"
)

@Serializable
data class SyncConfigUpdateRequest(
    val autoSyncIntervalMinutes: Int? = null,
    val reverseSyncSchedule: ReverseSyncSchedule? = null,
    val masterSourceOfTruth: MasterSourceOfTruth? = null
)

@Serializable
data class SyncTriggerResponse(
    val success: Boolean,
    val message: String,
    val itemsPushed: Int,
    val itemsPulled: Int
)
