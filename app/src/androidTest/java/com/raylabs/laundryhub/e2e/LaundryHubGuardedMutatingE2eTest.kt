package com.raylabs.laundryhub.e2e

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LaundryHubGuardedMutatingE2eTest {

    private lateinit var robot: LaundryHubAppRobot

    @Before
    fun setUp() {
        robot = LaundryHubAppRobot()
        robot.wakeAndUnlock()
        
        assumeTrue(
            LaundryHubE2eConfig.mutatingSandboxMessage,
            LaundryHubE2eConfig.mutatingSandboxEnabled
        )

        robot.launchFresh()
        val startupState = robot.waitForStartupState()
        assumeTrue(
            "Requires a signed-in app session. Current state was $startupState.",
            startupState == LaundryHubAppRobot.StartupState.HOME
        )
    }

    @After
    fun tearDown() {
        if (LaundryHubE2eConfig.mutatingSandboxEnabled) {
            try {
                robot.cleanUpAllE2eTransactions()
            } catch (e: Exception) {
                // Ignore failure in teardown if no matching rows or if UI wasn't ready
            }
        }
    }

    @Test
    fun testFlickerFreeTransition() {
        val baseName = "E2E_stab_${SystemClock.elapsedRealtime() % 100000}"
        robot.validateFlickerFreeSubmission(baseName, "9000")
    }

    @Test
    fun orderFlow_submitsUpdatesAndDeletesOnlyWhenSandboxMutationIsExplicitlyEnabled() {
        val baseName = "E2E_order_${SystemClock.elapsedRealtime() % 100000}"
        
        // Submit
        robot.submitSandboxOrder(orderName = baseName, price = "8000")
        
        // Update
        val updatedName = robot.updateSandboxOrder(baseName, "_mod")
        
        // Delete
        robot.deleteTransactionFromHistory(updatedName, isOutcome = false)
    }

    @Test
    fun outcomeFlow_submitsUpdatesAndDeletesOnlyWhenSandboxMutationIsExplicitlyEnabled() {
        val basePurpose = "E2E_out_${SystemClock.elapsedRealtime() % 100000}"
        
        // Submit
        robot.submitSandboxOutcome(purpose = basePurpose, price = "5000")
        
        // Update
        val updatedPurpose = robot.updateSandboxOutcome(basePurpose, "_mod")
        
        // Delete
        robot.deleteTransactionFromHistory(updatedPurpose, isOutcome = true)
    }
}
