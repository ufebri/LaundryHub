package com.raylabs.laundryhub.backend.service

import com.raylabs.laundryhub.core.domain.model.sheets.MasterSourceOfTruth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SheetsPushSchedulerTest {

    @Test
    fun `requestPush coalesces writes during debounce window`() = runTest {
        val pushJob = FakeSheetsPushJob(result = 2)
        val syncStateManager = SyncStateManager()
        val scheduler = SheetsPushScheduler(
            batchSyncJob = pushJob,
            syncStateManager = syncStateManager,
            debounceMillis = 1_000,
            scope = this
        )

        scheduler.requestPush("order created")
        scheduler.requestPush("outcome created")

        assertEquals(0, pushJob.flushCount)
        assertNotNull(scheduler.nextScheduledPushTime)

        advanceTimeBy(999)
        runCurrent()
        assertEquals(0, pushJob.flushCount)

        advanceTimeBy(1)
        runCurrent()

        assertEquals(1, pushJob.flushCount)
        assertEquals("SUCCESS", syncStateManager.lastSyncStatus)
        assertEquals(2, syncStateManager.lastChangesCount)
        assertNull(scheduler.nextScheduledPushTime)
    }

    @Test
    fun `requestPush skips when app database is not master source`() = runTest {
        val pushJob = FakeSheetsPushJob(result = 1)
        val syncStateManager = SyncStateManager()
        syncStateManager.updateMasterSourceOfTruth(MasterSourceOfTruth.SHEETS)
        val scheduler = SheetsPushScheduler(
            batchSyncJob = pushJob,
            syncStateManager = syncStateManager,
            debounceMillis = 1,
            scope = this
        )

        scheduler.requestPush("package created")
        advanceTimeBy(1)
        runCurrent()

        assertEquals(0, pushJob.flushCount)
        assertNull(scheduler.nextScheduledPushTime)
        assertEquals("UNKNOWN", syncStateManager.lastSyncStatus)
    }

    @Test
    fun `requestPush records failure when pending push cannot verify rows`() = runTest {
        val pushJob = FakeSheetsPushJob(errorMessage = "Order sync wrote no verified rows for 1 pending orders.")
        val syncStateManager = SyncStateManager()
        val scheduler = SheetsPushScheduler(
            batchSyncJob = pushJob,
            syncStateManager = syncStateManager,
            debounceMillis = 1,
            scope = this
        )

        scheduler.requestPush("order updated")
        advanceTimeBy(1)
        runCurrent()

        assertEquals(1, pushJob.flushCount)
        assertEquals("FAILED", syncStateManager.lastSyncStatus)
        assertEquals(0, syncStateManager.lastChangesCount)
        assertTrue(syncStateManager.lastSyncError.orEmpty().contains("no verified rows"))
    }

    @Test
    fun `parseDebounceMillis uses configured non-negative value or default`() {
        assertEquals(2_500, SheetsPushScheduler.parseDebounceMillis("2500"))
        assertEquals(0, SheetsPushScheduler.parseDebounceMillis("0"))
        assertEquals(SheetsPushScheduler.DEFAULT_DEBOUNCE_MILLIS, SheetsPushScheduler.parseDebounceMillis("-1"))
        assertEquals(SheetsPushScheduler.DEFAULT_DEBOUNCE_MILLIS, SheetsPushScheduler.parseDebounceMillis("nope"))
        assertEquals(SheetsPushScheduler.DEFAULT_DEBOUNCE_MILLIS, SheetsPushScheduler.parseDebounceMillis(null))
    }

    private class FakeSheetsPushJob(
        private val result: Int = 0,
        private val errorMessage: String? = null
    ) : SheetsPushJob {
        var flushCount = 0

        override suspend fun processUnsyncedAll(): Int {
            flushCount++
            errorMessage?.let { error(it) }
            return result
        }
    }
}
