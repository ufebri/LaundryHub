package com.raylabs.laundryhub.e2e

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.raylabs.laundryhub.BuildConfig
import com.raylabs.laundryhub.core.domain.model.sheets.GrossData
import com.raylabs.laundryhub.core.domain.model.sheets.selectCurrentOrLatestGross
import com.raylabs.laundryhub.ui.common.util.TextUtil.toRupiahFormat
import kotlinx.serialization.json.Json
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeNotNull
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.URL

@RunWith(AndroidJUnit4::class)
class LaundryHubGrossE2eTest {

    private lateinit var robot: LaundryHubAppRobot

    @Before
    fun setUp() {
        robot = LaundryHubAppRobot()
        robot.wakeAndUnlock()
    }

    @Test
    fun authenticatedHome_displaysCurrentGrossFromBackend_withoutMutating() {
        val apiBaseUrl = e2eApiBaseUrl()
        assumeFalse("Requires a backend API URL for read-only gross validation.", apiBaseUrl.isBlank())
        val expectedGross = fetchGross(apiBaseUrl).selectCurrentOrLatestGross()
        assumeNotNull(expectedGross)

        robot.launchFresh()
        val startupState = robot.waitForStartupState()
        assumeTrue(
            "Requires a signed-in app session. Current state was $startupState.",
            startupState == LaundryHubAppRobot.StartupState.HOME
        )

        val gross = requireNotNull(expectedGross)
        robot.assertGrossSummary(
            totalNominal = gross.totalNominal.toRupiahFormat(),
            orderCountLabel = gross.orderCount.toOrderCountLabel()
        )
        robot.openGrossDetailAndAssertFirstRow(
            month = gross.month,
            totalNominal = gross.totalNominal.toRupiahFormat(),
            orderCount = gross.orderCount,
            tax = gross.tax.toRupiahFormat()
        )
    }

    private fun e2eApiBaseUrl(): String {
        val args = InstrumentationRegistry.getArguments()
        return args.getString(ARG_API_BASE_URL)?.takeIf { it.isNotBlank() } ?: BuildConfig.BASE_URL
    }

    private fun fetchGross(apiBaseUrl: String): List<GrossData> {
        val connection = URL("${apiBaseUrl.trimEnd('/')}/gross").openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.requestMethod = "GET"
        return connection.inputStream.bufferedReader().use { reader ->
            Json { ignoreUnknownKeys = true }.decodeFromString(reader.readText())
        }
    }

    private fun String.toOrderCountLabel(): String {
        val count = trim()
            .replace(Regex("""\s*orders?\s*$""", RegexOption.IGNORE_CASE), "")
            .trim()
        return if (count.isBlank()) "" else "$count order"
    }

    private companion object {
        const val ARG_API_BASE_URL = "laundryhub.e2e.apiBaseUrl"
    }
}
