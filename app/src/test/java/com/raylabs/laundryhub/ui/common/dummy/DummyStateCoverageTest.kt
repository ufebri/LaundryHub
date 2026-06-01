package com.raylabs.laundryhub.ui.common.dummy

import com.raylabs.laundryhub.ui.common.dummy.history.dummyHistoryItem
import com.raylabs.laundryhub.ui.common.dummy.inventory.dummyInventoryUiState
import com.raylabs.laundryhub.ui.common.dummy.profile.dummyProfileUiState
import org.junit.Assert.assertNotNull
import org.junit.Test

class DummyStateCoverageTest {

    @Test
    fun `verify dummyHistoryItem variables are initialized`() {
        assertNotNull(dummyHistoryItem)
    }

    @Test
    fun `verify dummyInventoryUiState variables are initialized`() {
        assertNotNull(dummyInventoryUiState)
    }

    @Test
    fun `verify dummyProfileUiState variables are initialized`() {
        assertNotNull(dummyProfileUiState)
    }
}
