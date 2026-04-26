package com.raylabs.laundryhub.macrobenchmark

import android.os.SystemClock
import android.util.Log
import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class AddOrderFlowBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun addOrderFlow_updatesPendingOrder() {
        val openSheetDurations = mutableListOf<Long>()
        val submitToSuccessDurations = mutableListOf<Long>()
        val successToPendingDurations = mutableListOf<Long>()
        val totalFlowDurations = mutableListOf<Long>()
        var iteration = 0

        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(
                baselineProfileMode = BaselineProfileMode.Disable,
                warmupIterations = 1
            ),
            iterations = 1,
            setupBlock = {
                pressHome()
                launchAppFromShell()
                ensureHomeReady()
            }
        ) {
            iteration += 1

            val orderName = "bench${SystemClock.elapsedRealtime() % 100000}"
            val totalStart = SystemClock.elapsedRealtime()

            val openSheetMs = openOrderSheet()
            selectFirstPackage()
            fillRequiredFields(orderName = orderName, price = ORDER_PRICE)

            val submitStart = SystemClock.elapsedRealtime()
            tapObjectCenter(
                selector = By.desc(ORDER_SUBMIT_BUTTON_DESCRIPTION),
                debugLabel = ORDER_SUBMIT_BUTTON_DESCRIPTION,
                timeoutMs = FORM_READY_TIMEOUT_MS
            )

            val successMessage = waitForObject(
                By.textContains(ORDER_SUBMIT_SUCCESS_TEXT),
                SUBMIT_TIMEOUT_MS
            ).text.orEmpty()
            val submitToSuccessMs = SystemClock.elapsedRealtime() - submitStart
            val submittedOrderId = extractSubmittedOrderId(successMessage)

            val pendingWaitStart = SystemClock.elapsedRealtime()
            waitForPendingOrderVisibility(
                orderId = submittedOrderId,
                orderName = orderName
            )
            val successToPendingMs = SystemClock.elapsedRealtime() - pendingWaitStart

            val totalFlowMs = SystemClock.elapsedRealtime() - totalStart

            openSheetDurations += openSheetMs
            submitToSuccessDurations += submitToSuccessMs
            successToPendingDurations += successToPendingMs
            totalFlowDurations += totalFlowMs

            Log.i(
                LOG_TAG,
                "BENCHMARK_ITERATION iteration=$iteration " +
                    "open_add_order_ms=$openSheetMs " +
                    "submit_to_success_ms=$submitToSuccessMs " +
                    "success_to_pending_ms=$successToPendingMs " +
                    "total_flow_ms=$totalFlowMs " +
                    "order_name=$orderName " +
                    "order_id=$submittedOrderId"
            )
        }

        Log.i(
            LOG_TAG,
            "BENCHMARK_SUMMARY " +
                "open_add_order_ms_median=${openSheetDurations.median()} " +
                "submit_to_success_ms_median=${submitToSuccessDurations.median()} " +
                "success_to_pending_ms_median=${successToPendingDurations.median()} " +
                "total_flow_ms_median=${totalFlowDurations.median()}"
        )
    }

    private fun MacrobenchmarkScope.openOrderSheet(): Long {
        val start = SystemClock.elapsedRealtime()
        tapObjectCenter(
            selector = By.desc(ORDER_NAV_DESCRIPTION),
            debugLabel = ORDER_NAV_DESCRIPTION,
            timeoutMs = HOME_LOAD_TIMEOUT_MS
        )
        waitForObject(By.desc(ORDER_SHEET_DESCRIPTION), SHEET_LOAD_TIMEOUT_MS)
        return SystemClock.elapsedRealtime() - start
    }

    private fun MacrobenchmarkScope.launchAppFromShell() {
        device.executeShellCommand(
            "am start -W -a android.intent.action.MAIN " +
                "-c android.intent.category.LAUNCHER " +
                "$TARGET_PACKAGE/.ui.MainActivity"
        )
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.ensureHomeReady() {
        when (waitForStartupState()) {
            StartupState.HOME -> return

            StartupState.SPREADSHEET_SETUP -> {
                Log.i(LOG_TAG, "Spreadsheet setup screen detected before benchmark flow")

                val connectSheetsButton = waitForOptionalObject(
                    By.text(CONNECT_GOOGLE_SHEETS_TEXT),
                    SHORT_WAIT_TIMEOUT_MS
                )
                check(connectSheetsButton == null) {
                    "Benchmark device still needs Google Sheets access. Open the app once and grant Sheets access before rerunning Macrobenchmark."
                }

                val validateButton = waitForOptionalObject(
                    By.text(VALIDATE_AND_CONTINUE_TEXT),
                    SHORT_WAIT_TIMEOUT_MS
                )
                if (validateButton != null && validateButton.isEnabled) {
                    validateButton.click()
                    device.waitForIdle()
                }

                waitForHomeReady()
            }

            StartupState.ONBOARDING -> error(
                "Benchmark app reached onboarding. Sign in once on this benchmark build/device before rerunning Macrobenchmark."
            )

            StartupState.UNKNOWN -> {
                val windowHierarchy = dumpWindowHierarchy()
                error(
                    "App did not reach Home, Spreadsheet Setup, or onboarding. " +
                        "Window markers=${summarizeWindowMarkers(windowHierarchy)}"
                )
            }
        }
    }

    private fun MacrobenchmarkScope.waitForStartupState(): StartupState {
        val deadline = SystemClock.elapsedRealtime() + STARTUP_STATE_TIMEOUT_MS
        var lastObservedState = StartupState.UNKNOWN
        while (SystemClock.elapsedRealtime() < deadline) {
            if (isHomeVisible()) {
                return StartupState.HOME
            }
            if (device.hasObject(By.text(SPREADSHEET_SETUP_TITLE))) {
                lastObservedState = StartupState.SPREADSHEET_SETUP
            }
            if (device.hasObject(By.text(LOGIN_WITH_GOOGLE_TEXT))) {
                lastObservedState = StartupState.ONBOARDING
            }

            device.waitForIdle()
            SystemClock.sleep(500)
        }

        return lastObservedState
    }

    private fun MacrobenchmarkScope.dumpWindowHierarchy(): String {
        return device.executeShellCommand(
            "uiautomator dump /sdcard/laundryhub-benchmark-window.xml >/dev/null; " +
                "cat /sdcard/laundryhub-benchmark-window.xml"
        )
    }

    private fun summarizeWindowMarkers(windowHierarchy: String): String {
        val markers = buildList {
            if (windowHierarchy.contains(TODAY_ACTIVITY_TITLE) || windowHierarchy.contains(PENDING_ORDERS_TITLE)) {
                add("home")
            }
            if (windowHierarchy.contains("content-desc=\"$ORDER_NAV_DESCRIPTION\"")) {
                add("app_shell")
            }
            if (windowHierarchy.contains(SPREADSHEET_SETUP_TITLE)) add("spreadsheet_setup")
            if (windowHierarchy.contains(LOGIN_WITH_GOOGLE_TEXT)) add("onboarding")
            if (windowHierarchy.contains(VALIDATE_AND_CONTINUE_TEXT)) add("validate_button")
            if (windowHierarchy.contains(CONNECT_GOOGLE_SHEETS_TEXT)) add("connect_sheets")
        }
        return if (markers.isEmpty()) "none" else markers.joinToString()
    }

    private fun MacrobenchmarkScope.waitForHomeReady() {
        val deadline = SystemClock.elapsedRealtime() + HOME_LOAD_TIMEOUT_MS
        while (SystemClock.elapsedRealtime() < deadline) {
            if (isHomeVisible()) return
            device.waitForIdle()
            SystemClock.sleep(250)
        }

        error("Timed out waiting for Home markers to become visible")
    }

    private fun MacrobenchmarkScope.isHomeVisible(): Boolean {
        return device.hasObject(By.text(TODAY_ACTIVITY_TITLE)) ||
            device.hasObject(By.text(PENDING_ORDERS_TITLE)) ||
            device.hasObject(By.desc(ORDER_NAV_DESCRIPTION))
    }

    private fun MacrobenchmarkScope.fillRequiredFields(orderName: String, price: String) {
        focusFieldAndInputText(
            selectors = listOf(
                By.desc(ORDER_NAME_FIELD_DESCRIPTION),
                By.text(NAME_LABEL)
            ),
            value = orderName,
            debugLabel = ORDER_NAME_FIELD_DESCRIPTION
        )
        device.pressBack()
        device.waitForIdle()
        SystemClock.sleep(300)
        ensureFieldVisible(
            selectors = listOf(
                By.desc(ORDER_PRICE_FIELD_DESCRIPTION),
                By.text(PRICE_LABEL)
            ),
            debugLabel = ORDER_PRICE_FIELD_DESCRIPTION
        )
        focusFieldAndInputText(
            selectors = listOf(
                By.desc(ORDER_PRICE_FIELD_DESCRIPTION),
                By.text(PRICE_LABEL)
            ),
            value = price,
            debugLabel = ORDER_PRICE_FIELD_DESCRIPTION
        )
        device.pressBack()
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.selectFirstPackage() {
        tapObjectCenter(
            selector = firstPackageSelector(),
            debugLabel = "first package option",
            timeoutMs = FORM_READY_TIMEOUT_MS
        )
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.tapObjectCenter(
        selector: BySelector,
        debugLabel: String,
        timeoutMs: Long
    ) {
        val current = waitForObject(selector, timeoutMs)
        val bounds = current.visibleBounds
        check(device.click(bounds.centerX(), bounds.centerY())) {
            "Failed to tap object center for $debugLabel at $bounds"
        }
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.focusFieldAndInputText(
        selectors: List<BySelector>,
        value: String,
        debugLabel: String
    ) {
        val field = waitForAnyObject(selectors, FORM_READY_TIMEOUT_MS)
        inputTextIntoObject(field, value, debugLabel)
    }

    private fun MacrobenchmarkScope.inputTextIntoObject(
        field: UiObject2,
        value: String,
        debugLabel: String
    ) {
        val bounds = field.visibleBounds
        check(device.click(bounds.centerX(), bounds.centerY())) {
            "Failed to tap field for $debugLabel at $bounds"
        }
        device.executeShellCommand("input text ${escapeForShellInput(value)}")
        device.waitForIdle()
        SystemClock.sleep(300)
    }

    private fun MacrobenchmarkScope.ensureFieldVisible(
        selectors: List<BySelector>,
        debugLabel: String
    ) {
        val deadline = SystemClock.elapsedRealtime() + FORM_READY_TIMEOUT_MS
        while (SystemClock.elapsedRealtime() < deadline) {
            val field = waitForOptionalAnyObject(selectors, SHORT_WAIT_TIMEOUT_MS)
            if (field != null && !field.visibleBounds.isEmpty) {
                return
            }

            device.swipe(
                (device.displayWidth * 0.88f).toInt(),
                (device.displayHeight * 0.82f).toInt(),
                (device.displayWidth * 0.88f).toInt(),
                (device.displayHeight * 0.42f).toInt(),
                24
            )
            device.waitForIdle()
            SystemClock.sleep(400)
        }

        error("Timed out waiting for field to become visible: $debugLabel")
    }

    private fun MacrobenchmarkScope.waitForPendingOrderVisibility(
        orderId: String,
        orderName: String
    ) {
        ensurePendingOrdersVisible()

        if (filterPendingOrdersByName(orderName)) {
            val displayName = orderName.replaceFirstChar { firstChar ->
                firstChar.uppercase()
            }
            waitForSingleFilteredPendingResult(displayName)
            return
        }

        waitForPendingOrderLabel(orderId)
    }

    private fun MacrobenchmarkScope.filterPendingOrdersByName(orderName: String): Boolean {
        if (!openPendingOrderSearch()) {
            return false
        }

        val searchField = waitForOptionalAnyObject(
            selectors = listOf(
                By.desc(PENDING_ORDER_SEARCH_FIELD_DESCRIPTION),
                By.clazz("android.widget.EditText"),
                By.text(SEARCH_CUSTOMER_PLACEHOLDER)
            ),
            timeoutMs = SHORT_WAIT_TIMEOUT_MS
        )
        if (searchField == null || searchField.visibleBounds.isEmpty) {
            Log.i(LOG_TAG, "Pending search field did not appear; falling back to order id scan")
            device.pressBack()
            device.waitForIdle()
            return false
        }

        inputTextIntoObject(
            field = searchField,
            value = orderName,
            debugLabel = PENDING_ORDER_SEARCH_LABEL
        )
        device.pressBack()
        device.waitForIdle()
        return true
    }

    private fun MacrobenchmarkScope.waitForPendingOrderLabel(orderId: String) {
        val orderLabel = "Order #$orderId"
        val deadline = SystemClock.elapsedRealtime() + PENDING_UPDATE_TIMEOUT_MS
        while (SystemClock.elapsedRealtime() < deadline) {
            if (device.hasObject(By.text(orderLabel))) {
                return
            }

            device.swipe(
                device.displayWidth / 2,
                (device.displayHeight * 0.78f).toInt(),
                device.displayWidth / 2,
                (device.displayHeight * 0.34f).toInt(),
                24
            )
            device.waitForIdle()
            SystemClock.sleep(400)
        }

        error("Timed out waiting for pending order card to show $orderLabel")
    }

    private fun MacrobenchmarkScope.openPendingOrderSearch(): Boolean {
        repeat(6) {
            val searchButton = waitForOptionalObject(
                selector = By.desc(OPEN_SEARCH_DESCRIPTION),
                timeoutMs = SHORT_WAIT_TIMEOUT_MS
            )
            if (searchButton != null) {
                val bounds = searchButton.visibleBounds
                check(device.click(bounds.centerX(), bounds.centerY())) {
                    "Failed to tap Pending Order search button at $bounds"
                }
                device.waitForIdle()
                return true
            }

            device.swipe(
                device.displayWidth / 2,
                (device.displayHeight * 0.34f).toInt(),
                device.displayWidth / 2,
                (device.displayHeight * 0.74f).toInt(),
                24
            )
            device.waitForIdle()
            SystemClock.sleep(400)
        }

        return false
    }

    private fun MacrobenchmarkScope.waitForSingleFilteredPendingResult(displayName: String) {
        val deadline = SystemClock.elapsedRealtime() + PENDING_UPDATE_TIMEOUT_MS
        while (SystemClock.elapsedRealtime() < deadline) {
            val visibleOrderLabels = device.findObjects(By.textStartsWith(ORDER_LABEL_PREFIX))
                .count { !it.visibleBounds.isEmpty }
            val hasDisplayName = device.hasObject(By.text(displayName))

            if (hasDisplayName && visibleOrderLabels == 1) {
                return
            }

            device.waitForIdle()
            SystemClock.sleep(300)
        }

        error("Timed out waiting for exactly one filtered pending result for $displayName")
    }

    private fun MacrobenchmarkScope.ensurePendingOrdersVisible() {
        val deadline = SystemClock.elapsedRealtime() + PENDING_UPDATE_TIMEOUT_MS
        while (SystemClock.elapsedRealtime() < deadline) {
            val hasPendingHeader = device.hasObject(By.text(PENDING_ORDERS_TITLE)) ||
                device.hasObject(By.desc(OPEN_SEARCH_DESCRIPTION))
            if (hasPendingHeader) {
                return
            }

            device.swipe(
                device.displayWidth / 2,
                (device.displayHeight * 0.82f).toInt(),
                device.displayWidth / 2,
                (device.displayHeight * 0.42f).toInt(),
                24
            )
            device.waitForIdle()
            SystemClock.sleep(400)
        }

        error("Timed out waiting for Pending Orders section to become visible")
    }

    private fun MacrobenchmarkScope.waitForObject(
        selector: BySelector,
        timeoutMs: Long
    ): UiObject2 {
        device.wait(Until.hasObject(selector), timeoutMs)
        return requireNotNull(device.findObject(selector)) {
            "Timed out waiting for UI object: $selector"
        }
    }

    private fun MacrobenchmarkScope.waitForOptionalObject(
        selector: BySelector,
        timeoutMs: Long
    ): UiObject2? {
        device.wait(Until.hasObject(selector), timeoutMs)
        return device.findObject(selector)
    }

    private fun MacrobenchmarkScope.waitForAnyObject(
        selectors: List<BySelector>,
        timeoutMs: Long
    ): UiObject2 {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < deadline) {
            selectors.firstNotNullOfOrNull { selector -> device.findObject(selector) }?.let { return it }

            device.waitForIdle()
            SystemClock.sleep(250)
        }

        error("Timed out waiting for any UI object in selectors: $selectors")
    }

    private fun MacrobenchmarkScope.waitForOptionalAnyObject(
        selectors: List<BySelector>,
        timeoutMs: Long
    ): UiObject2? {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < deadline) {
            selectors.firstNotNullOfOrNull { selector -> device.findObject(selector) }?.let { return it }

            device.waitForIdle()
            SystemClock.sleep(250)
        }

        return null
    }

    private fun List<Long>.median(): Long {
        if (isEmpty()) return 0L
        val sorted = sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[middle - 1] + sorted[middle]) / 2
        } else {
            sorted[middle]
        }
    }

    companion object {
        private const val TARGET_PACKAGE = "com.raylabs.laundryhub"
        private const val LOG_TAG = "AddOrderFlowBenchmark"
        private const val ORDER_PRICE = "8000"

        private const val ORDER_NAV_DESCRIPTION = "Order"
        private const val ORDER_SHEET_DESCRIPTION = "Order sheet"
        private const val ORDER_NAME_FIELD_DESCRIPTION = "Order name field"
        private const val ORDER_PACKAGE_OPTION_DESCRIPTION_PREFIX = "Package option "
        private const val ORDER_PRICE_FIELD_DESCRIPTION = "Order price field"
        private const val ORDER_SUBMIT_BUTTON_DESCRIPTION = "Submit order"
        private const val TODAY_ACTIVITY_TITLE = "Today Activity"
        private const val PENDING_ORDERS_TITLE = "Pending Orders"
        private const val ORDER_LABEL_PREFIX = "Order #"
        private const val OPEN_SEARCH_DESCRIPTION = "Open Search"
        private const val PENDING_ORDER_SEARCH_LABEL = "Pending order search"
        private const val PENDING_ORDER_SEARCH_FIELD_DESCRIPTION = "Pending order search field"
        private const val ORDER_SUBMIT_SUCCESS_TEXT = "submitted successfully."
        private const val NAME_LABEL = "Name"
        private const val PRICE_LABEL = "Price"
        private const val SEARCH_CUSTOMER_PLACEHOLDER = "Search Customer"
        private const val SPREADSHEET_SETUP_TITLE = "Set Up Your Spreadsheet"
        private const val VALIDATE_AND_CONTINUE_TEXT = "Validate & Continue"
        private const val CONNECT_GOOGLE_SHEETS_TEXT = "Connect Google Sheets"
        private const val LOGIN_WITH_GOOGLE_TEXT = "Login with Google"

        private const val HOME_LOAD_TIMEOUT_MS = 15_000L
        private const val STARTUP_STATE_TIMEOUT_MS = 30_000L
        private const val SHEET_LOAD_TIMEOUT_MS = 10_000L
        private const val FORM_READY_TIMEOUT_MS = 20_000L
        private const val SUBMIT_TIMEOUT_MS = 45_000L
        private const val PENDING_UPDATE_TIMEOUT_MS = 45_000L
        private const val SHORT_WAIT_TIMEOUT_MS = 2_500L

        private fun firstPackageSelector(): BySelector = By.descStartsWith(
            ORDER_PACKAGE_OPTION_DESCRIPTION_PREFIX
        )

        private fun extractSubmittedOrderId(successMessage: String): String {
            val match = Regex("""Order #(\d+)""").find(successMessage)
            return requireNotNull(match?.groupValues?.getOrNull(1)) {
                "Unable to parse submitted order id from success message: $successMessage"
            }
        }

        private fun escapeForShellInput(value: String): String = buildString {
            value.forEach { char ->
                when {
                    char.isLetterOrDigit() -> append(char)
                    char == ' ' -> append("%s")
                    else -> append("\\").append(char)
                }
            }
        }
    }

    private enum class StartupState {
        HOME,
        SPREADSHEET_SETUP,
        ONBOARDING,
        UNKNOWN
    }
}
