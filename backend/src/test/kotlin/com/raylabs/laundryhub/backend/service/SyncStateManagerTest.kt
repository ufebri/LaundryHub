package com.raylabs.laundryhub.backend.service

import com.raylabs.laundryhub.core.domain.model.sheets.MasterSourceOfTruth
import com.raylabs.laundryhub.core.domain.model.sheets.ReverseSyncSchedule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SyncStateManagerTest {

    @Test
    fun `default config keeps sheets as temporary master with manual pull`() {
        val manager = SyncStateManager()
        val config = manager.config.value

        assertEquals(5, config.intervalMinutes)
        assertEquals(ReverseSyncSchedule.MANUAL, config.reverseSyncSchedule)
        assertEquals(MasterSourceOfTruth.SHEETS, config.masterSourceOfTruth)
    }

    @Test
    fun `recordSyncFailure exposes last error until next success`() {
        val manager = SyncStateManager()

        manager.recordSyncFailure("quota exceeded")

        assertEquals("FAILED", manager.lastSyncStatus)
        assertEquals("quota exceeded", manager.lastSyncError)

        manager.recordSync(changesCount = 2)

        assertEquals("SUCCESS", manager.lastSyncStatus)
        assertNull(manager.lastSyncError)
    }
}
