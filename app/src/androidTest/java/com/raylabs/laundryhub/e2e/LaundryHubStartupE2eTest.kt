package com.raylabs.laundryhub.e2e

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LaundryHubStartupE2eTest {

    private lateinit var robot: LaundryHubAppRobot

    @Before
    fun setUp() {
        robot = LaundryHubAppRobot()
        robot.wakeAndUnlock()
    }

    @Test
    fun launch_reachesKnownEntryPoint() {
        robot.launchFresh()
        robot.assertKnownStartupState()
    }

    @Test
    fun authenticatedShell_navigatesPrimaryTabsAndOrderSheet_withoutMutating() {
        robot.launchFresh()
        val startupState = robot.waitForStartupState()

        assumeTrue(
            "Requires a signed-in app session. Current state was $startupState.",
            startupState == LaundryHubAppRobot.StartupState.HOME
        )

        robot.navigatePrimaryShellWithoutMutation()
    }
}
