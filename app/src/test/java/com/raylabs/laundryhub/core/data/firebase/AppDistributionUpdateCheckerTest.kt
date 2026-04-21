package com.raylabs.laundryhub.core.data.firebase

import com.google.firebase.appdistribution.FirebaseAppDistribution
import com.google.firebase.appdistribution.UpdateTask
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class AppDistributionUpdateCheckerTest {

    @Test
    fun `checkAndPromptIfNeeded returns true when app distribution succeeds`() = runTest {
        val appDistribution = mock<FirebaseAppDistribution>()
        val updateTask = mock<UpdateTask>()
        doAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            (invocation.arguments[0] as com.google.android.gms.tasks.OnSuccessListener<Void?>)
                .onSuccess(null)
            updateTask
        }.whenever(updateTask).addOnSuccessListener(any())
        whenever(updateTask.addOnFailureListener(any())).thenReturn(updateTask)
        whenever(appDistribution.updateIfNewReleaseAvailable()).thenReturn(updateTask)

        val result = AppDistributionUpdateChecker(appDistribution).checkAndPromptIfNeeded()

        assertTrue(result)
    }

    @Test
    fun `checkAndPromptIfNeeded returns false when app distribution fails`() = runTest {
        val appDistribution = mock<FirebaseAppDistribution>()
        val updateTask = mock<UpdateTask>()
        whenever(updateTask.addOnSuccessListener(any())).thenReturn(updateTask)
        doAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            (invocation.arguments[0] as com.google.android.gms.tasks.OnFailureListener)
                .onFailure(RuntimeException("network error"))
            updateTask
        }.whenever(updateTask).addOnFailureListener(any())
        whenever(appDistribution.updateIfNewReleaseAvailable()).thenReturn(updateTask)

        val result = AppDistributionUpdateChecker(appDistribution).checkAndPromptIfNeeded()

        assertFalse(result)
    }
}
