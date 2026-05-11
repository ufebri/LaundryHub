package com.raylabs.laundryhub.core.domain.model.sheets

import kotlinx.serialization.Serializable

@Serializable
enum class ReverseSyncSchedule(val hours: List<Int>) {
    DEFAULT_23(listOf(23)),
    TWICE_DAILY(listOf(12, 23)),
    MANUAL(emptyList())
}

@Serializable
data class SyncStatusResponse(
    val lastSyncTime: String?,
    val changesCount: Int,
    val autoSyncIntervalMinutes: Int,
    val reverseSyncSchedule: ReverseSyncSchedule
)

@Serializable
data class SyncConfigUpdateRequest(
    val autoSyncIntervalMinutes: Int? = null,
    val reverseSyncSchedule: ReverseSyncSchedule? = null
)

@Serializable
data class SyncTriggerResponse(
    val success: Boolean,
    val message: String,
    val itemsPushed: Int,
    val itemsPulled: Int
)
