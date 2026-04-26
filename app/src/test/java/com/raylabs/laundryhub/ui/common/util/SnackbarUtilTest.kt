package com.raylabs.laundryhub.ui.common.util

import androidx.compose.material.SnackbarHostState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class SnackbarUtilTest {

    @Test
    fun `showQuickSnackbar calls showSnackbar on host state`() = runTest {
        val mockHostState: SnackbarHostState = mock()
        
        // We can't easily test the timeout part in a unit test without 
        // complex dispatcher manipulation, but we can verify it initiates the call.
        mockHostState.showQuickSnackbar("Hello")
        
        verify(mockHostState).showSnackbar(
            message = org.mockito.kotlin.eq("Hello"),
            actionLabel = org.mockito.kotlin.anyOrNull(),
            duration = org.mockito.kotlin.any()
        )
    }
}
