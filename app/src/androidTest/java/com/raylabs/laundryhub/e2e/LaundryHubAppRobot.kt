package com.raylabs.laundryhub.e2e

import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Assert.fail
import java.io.File

internal class LaundryHubAppRobot(
    private val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
) {

    fun wakeAndUnlock() {
        if (!device.isScreenOn) {
            device.wakeUp()
        }
        device.executeShellCommand("input keyevent KEYCODE_WAKEUP")
        device.executeShellCommand("wm dismiss-keyguard")
        collapseSystemSurfaces()
        device.waitForIdle()
    }

    fun launchFresh() {
        collapseSystemSurfaces()
        device.executeShellCommand("am start -W -n $TARGET_PACKAGE/$MAIN_ACTIVITY_CLASS -f $LAUNCH_FLAGS")
        val launched = device.wait(Until.hasObject(By.pkg(TARGET_PACKAGE)), STARTUP_TIMEOUT_MS)
        collapseSystemSurfaces()
        device.waitForIdle()

        if (!launched && device.currentPackageName != TARGET_PACKAGE) {
            fail("Failed to launch app. Current package=${device.currentPackageName}")
        }
    }

    private fun collapseSystemSurfaces() {
        device.executeShellCommand("cmd statusbar collapse")
        device.waitForIdle()
    }

    fun waitForStartupState(timeoutMs: Long = STARTUP_TIMEOUT_MS): StartupState {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        var lastObservedState = StartupState.UNKNOWN

        while (SystemClock.elapsedRealtime() < deadline) {
            if (isHomeShellVisible()) {
                return StartupState.HOME
            }
            if (device.hasObject(By.text(SPREADSHEET_SETUP_TITLE))) {
                lastObservedState = StartupState.SPREADSHEET_SETUP
            }
            if (device.hasObject(By.text(LOGIN_WITH_GOOGLE_TEXT))) {
                lastObservedState = StartupState.ONBOARDING
            }

            device.waitForIdle()
            SystemClock.sleep(250)
        }

        return lastObservedState
    }

    fun assertKnownStartupState() {
        when (waitForStartupState()) {
            StartupState.HOME -> assertHomeShellVisible()
            StartupState.ONBOARDING -> waitForObject(By.text(LOGIN_WITH_GOOGLE_TEXT), SHORT_TIMEOUT_MS)
            StartupState.SPREADSHEET_SETUP -> waitForObject(By.text(SPREADSHEET_SETUP_TITLE), SHORT_TIMEOUT_MS)
            StartupState.UNKNOWN -> fail("Unknown startup state. Markers=${summarizeWindowMarkers()}")
        }
    }

    fun navigatePrimaryShellWithoutMutation() {
        tapNav(ORDER_NAV_DESCRIPTION)
        waitForObject(By.desc(ORDER_SHEET_DESCRIPTION), FORM_TIMEOUT_MS)
        device.pressBack()
        waitForHomeShellOrRelaunch()

        tapNav(HISTORY_NAV_DESCRIPTION)
        waitForAnyObject(listOf(By.text(HISTORY_NAV_DESCRIPTION), By.text(NO_TRANSACTIONS_TEXT)), SHORT_TIMEOUT_MS)

        tapNav(OUTCOME_NAV_DESCRIPTION)
        waitForObject(By.desc(ADD_OUTCOME_DESCRIPTION), FORM_TIMEOUT_MS)

        tapNav(PROFILE_NAV_DESCRIPTION)
        waitForObject(By.text(PROFILE_INVENTORY_TEXT), FORM_TIMEOUT_MS)

        tapNav(HOME_NAV_DESCRIPTION)
        assertHomeShellVisible()
    }

    fun assertHomeShellVisible() {
        if (!isHomeShellVisible()) {
            fail("Home shell is not visible. Markers=${summarizeWindowMarkers()}")
        }
    }

    fun dumpWindowHierarchy(): String {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val hierarchyFile = File(context.cacheDir, "laundryhub-e2e-window.xml")
        device.dumpWindowHierarchy(hierarchyFile)
        return hierarchyFile.readText()
    }

    private fun tapNav(label: String) {
        tapObjectCenter(
            selector = By.desc(label),
            debugLabel = "$label navigation",
            timeoutMs = FORM_TIMEOUT_MS
        )
    }

    private fun waitForHomeShellOrRelaunch() {
        val deadline = SystemClock.elapsedRealtime() + FORM_TIMEOUT_MS
        while (SystemClock.elapsedRealtime() < deadline) {
            if (isHomeShellVisible()) return
            if (device.currentPackageName != TARGET_PACKAGE) {
                launchFresh()
            }

            device.waitForIdle()
            SystemClock.sleep(250)
        }

        assertHomeShellVisible()
    }

    private fun selectFirstPackage() {
        tapAnyObjectCenterOrHierarchyBounds(
            selectors = listOf(
                By.descStartsWith(ORDER_PACKAGE_OPTION_DESCRIPTION_PREFIX),
                By.text("Reguler"),
                By.text("Regular"),
                By.text("Express - 24H"),
                By.text("Express - 6H")
            ),
            debugLabel = "first package option",
            timeoutMs = DATA_TIMEOUT_MS
        )
    }

    private fun fillOrderRequiredFields(orderName: String, price: String) {
        focusFieldAndInputText(
            selectors = listOf(By.desc(ORDER_NAME_FIELD_DESCRIPTION), By.text(NAME_LABEL)),
            value = orderName,
            debugLabel = ORDER_NAME_FIELD_DESCRIPTION
        )
        device.pressBack()
        device.waitForIdle()

        ensureObjectVisible(
            selectors = listOf(By.desc(ORDER_PRICE_FIELD_DESCRIPTION), By.text(PRICE_LABEL)),
            debugLabel = ORDER_PRICE_FIELD_DESCRIPTION
        )
        focusFieldAndInputText(
            selectors = listOf(By.desc(ORDER_PRICE_FIELD_DESCRIPTION), By.text(PRICE_LABEL)),
            value = price,
            debugLabel = ORDER_PRICE_FIELD_DESCRIPTION
        )
        device.pressBack()
        device.waitForIdle()
    }

    private fun focusFieldAndInputText(
        selectors: List<BySelector>,
        value: String,
        debugLabel: String
    ) {
        val field = waitForAnyObject(selectors, FORM_TIMEOUT_MS)
        val bounds = field.visibleBounds
        check(device.click(bounds.centerX(), bounds.centerY())) {
            "Failed to tap $debugLabel at $bounds"
        }
        device.executeShellCommand("input text ${escapeForShellInput(value)}")
        device.waitForIdle()
        SystemClock.sleep(250)
    }

    private fun ensureObjectVisible(selectors: List<BySelector>, debugLabel: String) {
        val deadline = SystemClock.elapsedRealtime() + FORM_TIMEOUT_MS
        while (SystemClock.elapsedRealtime() < deadline) {
            val visible = selectors.firstNotNullOfOrNull { selector ->
                device.findObject(selector)?.takeIf { !it.visibleBounds.isEmpty }
            }
            if (visible != null) return

            device.swipe(
                (device.displayWidth * 0.88f).toInt(),
                (device.displayHeight * 0.82f).toInt(),
                (device.displayWidth * 0.88f).toInt(),
                (device.displayHeight * 0.42f).toInt(),
                24
            )
            device.waitForIdle()
            SystemClock.sleep(300)
        }

        fail("Timed out waiting for visible object: $debugLabel. Markers=${summarizeWindowMarkers()}")
    }

    private fun isHomeShellVisible(): Boolean {
        return device.hasObject(By.desc(HOME_NAV_DESCRIPTION)) &&
            device.hasObject(By.desc(ORDER_NAV_DESCRIPTION)) &&
            device.hasObject(By.desc(PROFILE_NAV_DESCRIPTION))
    }

    private fun tapObjectCenter(selector: BySelector, debugLabel: String, timeoutMs: Long) {
        val current = waitForObject(selector, timeoutMs)
        val bounds = current.visibleBounds
        check(device.click(bounds.centerX(), bounds.centerY())) {
            "Failed to tap $debugLabel at $bounds"
        }
        device.waitForIdle()
    }

    private fun tapAnyObjectCenter(selectors: List<BySelector>, debugLabel: String, timeoutMs: Long) {
        val current = waitForAnyObject(selectors, timeoutMs)
        val bounds = current.visibleBounds
        check(device.click(bounds.centerX(), bounds.centerY())) {
            "Failed to tap $debugLabel at $bounds"
        }
        device.waitForIdle()
    }

    private fun tapAnyObjectCenterOrHierarchyBounds(
        selectors: List<BySelector>,
        debugLabel: String,
        timeoutMs: Long
    ) {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < deadline) {
            selectors.firstNotNullOfOrNull { selector -> device.findObject(selector) }?.let { current ->
                val bounds = current.visibleBounds
                check(device.click(bounds.centerX(), bounds.centerY())) {
                    "Failed to tap $debugLabel at $bounds"
                }
                device.waitForIdle()
                return
            }

            findFirstPackageOptionBoundsFromHierarchy()?.let { bounds ->
                check(device.click(bounds.centerX, bounds.centerY)) {
                    "Failed to tap $debugLabel from hierarchy at $bounds"
                }
                device.waitForIdle()
                return
            }

            device.waitForIdle()
            SystemClock.sleep(500)
        }

        fail("Timed out waiting for $debugLabel. Markers=${summarizeWindowMarkers()}")
    }

    private fun waitForObject(selector: BySelector, timeoutMs: Long): UiObject2 {
        device.wait(Until.hasObject(selector), timeoutMs)
        return requireNotNull(device.findObject(selector)) {
            "Timed out waiting for UI object: $selector. Markers=${summarizeWindowMarkers()}"
        }
    }

    private fun waitForAnyObject(selectors: List<BySelector>, timeoutMs: Long): UiObject2 {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < deadline) {
            selectors.firstNotNullOfOrNull { selector -> device.findObject(selector) }?.let { return it }

            device.waitForIdle()
            SystemClock.sleep(250)
        }

        fail("Timed out waiting for any UI object in selectors: $selectors. Markers=${summarizeWindowMarkers()}")
        throw AssertionError("Unreachable")
    }

    private fun summarizeWindowMarkers(): String {
        val hierarchy = dumpWindowHierarchy()
        val foundTexts = Regex("""text="([^"]*)"""").findAll(hierarchy).map { it.groupValues[1] }.toList()
        val markers = buildList {
            if (hierarchy.contains(LOGIN_WITH_GOOGLE_TEXT)) add("onboarding")
            if (hierarchy.contains(SPREADSHEET_SETUP_TITLE)) add("spreadsheet_setup")
            if (hierarchy.contains("content-desc=\"$HOME_NAV_DESCRIPTION\"")) add("home_nav")
            if (hierarchy.contains("content-desc=\"$HISTORY_NAV_DESCRIPTION\"")) add("history_nav")
            if (hierarchy.contains("content-desc=\"$ORDER_NAV_DESCRIPTION\"")) add("order_nav")
            if (hierarchy.contains("content-desc=\"$PROFILE_NAV_DESCRIPTION\"")) add("profile_nav")
            if (hierarchy.contains(ORDER_SHEET_DESCRIPTION)) add("order_sheet")
        }
        return "Markers=[${markers.joinToString()}], FoundTexts=[${foundTexts.take(20).joinToString()}${if (foundTexts.size > 20) "..." else ""}]"
    }

    private fun findFirstPackageOptionBoundsFromHierarchy(): NodeBounds? {
        val hierarchy = dumpWindowHierarchy()
        return findFirstNodeBounds(
            hierarchy = hierarchy,
            attribute = "content-desc",
            startsWith = ORDER_PACKAGE_OPTION_DESCRIPTION_PREFIX
        ) ?: findFirstNodeBounds(
            hierarchy = hierarchy,
            attribute = "text",
            exactValues = PACKAGE_OPTION_FALLBACK_TEXTS
        )
    }

    private fun findFirstNodeBounds(
        hierarchy: String,
        attribute: String,
        startsWith: String? = null,
        exactValues: Set<String> = emptySet()
    ): NodeBounds? {
        val attributePattern = Regex("""\b${Regex.escape(attribute)}="([^"]*)"""")
        val nodePattern = Regex("""<node\b[^>]*>""")

        return nodePattern.findAll(hierarchy).firstNotNullOfOrNull { nodeMatch ->
            val node = nodeMatch.value
            val value = attributePattern.find(node)?.groupValues?.get(1).orEmpty()
            val matches = startsWith?.let(value::startsWith) == true || value in exactValues

            if (matches) extractBounds(node) else null
        }
    }

    private fun extractBounds(node: String): NodeBounds? {
        val match = NODE_BOUNDS_PATTERN.find(node) ?: return null
        return NodeBounds(
            left = match.groupValues[1].toInt(),
            top = match.groupValues[2].toInt(),
            right = match.groupValues[3].toInt(),
            bottom = match.groupValues[4].toInt()
        )
    }

    private fun escapeForShellInput(value: String): String = buildString {
        value.forEach { char ->
            when {
                char.isLetterOrDigit() -> append(char)
                char == '_' || char == '-' -> append(char)
                char == ' ' -> append("%s")
                else -> append("\\").append(char)
            }
        }
    }

    private fun formatTextForSearch(text: String): String {
        return text
    }

    fun submitSandboxOutcome(purpose: String, price: String) {
        tapNav(OUTCOME_NAV_DESCRIPTION)
        tapObjectCenter(By.desc(ADD_OUTCOME_DESCRIPTION), "Add Outcome button", FORM_TIMEOUT_MS)
        
        focusFieldAndInputText(
            selectors = listOf(By.text("Purpose")),
            value = purpose,
            debugLabel = "Outcome Purpose"
        )
        device.pressBack()
        device.waitForIdle()

        focusFieldAndInputText(
            selectors = listOf(By.text("Price")),
            value = price,
            debugLabel = "Outcome Price"
        )
        device.pressBack()
        device.waitForIdle()

        ensureObjectVisible(listOf(By.text("Submit")), "Submit outcome button")
        tapObjectCenter(By.text("Submit"), "Submit outcome", FORM_TIMEOUT_MS)
        device.wait(Until.gone(By.text("Submit")), SUBMIT_TIMEOUT_MS)
    }

    fun submitSandboxOrder(orderName: String, price: String) {
        tapNav(ORDER_NAV_DESCRIPTION)
        waitForObject(By.desc(ORDER_SHEET_DESCRIPTION), FORM_TIMEOUT_MS)
        selectFirstPackage()
        fillOrderRequiredFields(orderName = orderName, price = price)
        ensureObjectVisible(listOf(By.desc(ORDER_SUBMIT_BUTTON_DESCRIPTION)), ORDER_SUBMIT_BUTTON_DESCRIPTION)
        tapObjectCenter(By.desc(ORDER_SUBMIT_BUTTON_DESCRIPTION), ORDER_SUBMIT_BUTTON_DESCRIPTION, FORM_TIMEOUT_MS)
        device.wait(Until.gone(By.desc(ORDER_SHEET_DESCRIPTION)), SUBMIT_TIMEOUT_MS)
    }

    private fun pullToRefreshHistory() {
        val hierarchy = dumpWindowHierarchy()
        if (hierarchy.contains(NO_TRANSACTIONS_TEXT)) return
        
        device.swipe(
            device.displayWidth / 2,
            (device.displayHeight * 0.25f).toInt(),
            device.displayWidth / 2,
            (device.displayHeight * 0.75f).toInt(),
            40
        )
        device.waitForIdle()
        SystemClock.sleep(2000)
    }

    private fun waitForHistoryPopulated() {
        val deadline = SystemClock.elapsedRealtime() + DATA_TIMEOUT_MS
        while (SystemClock.elapsedRealtime() < deadline) {
            val hierarchy = dumpWindowHierarchy()
            if (hierarchy.contains("Order #") || hierarchy.contains("Outcome #")) {
                return
            }
            if (hierarchy.contains(NO_TRANSACTIONS_TEXT)) {
                return
            }
            device.waitForIdle()
            SystemClock.sleep(500)
        }
    }

    fun updateSandboxOrder(oldOrderName: String, newOrderSuffix: String): String {
        tapNav(HISTORY_NAV_DESCRIPTION)
        waitForHistoryPopulated()
        pullToRefreshHistory()
        
        val searchName = formatTextForSearch(oldOrderName)
        ensureObjectVisible(listOf(By.textContains(searchName)), "History entry $searchName")
        val item = waitForAnyObject(listOf(By.textContains(searchName)), DATA_TIMEOUT_MS)
        val bounds = item.visibleBounds
        device.click(bounds.centerX(), bounds.centerY())
        device.waitForIdle()
        
        tapAnyObjectCenterOrHierarchyBounds(
            selectors = listOf(By.text("Update order")),
            debugLabel = "Update order action",
            timeoutMs = SHORT_TIMEOUT_MS
        )
        
        focusFieldAndInputText(
            selectors = listOf(By.desc(ORDER_NAME_FIELD_DESCRIPTION), By.text("Name")),
            value = newOrderSuffix,
            debugLabel = "Order Name update"
        )
        device.pressBack()
        device.waitForIdle()

        SystemClock.sleep(500)
        
        ensureObjectVisible(listOf(By.text("Update")), "Update button")
        tapObjectCenter(By.text("Update"), "Update button", FORM_TIMEOUT_MS)
        device.wait(Until.gone(By.text("Update")), SUBMIT_TIMEOUT_MS)
        
        return "$oldOrderName$newOrderSuffix"
    }

    fun updateSandboxOutcome(oldPurpose: String, newPurposeSuffix: String): String {
        tapNav(HISTORY_NAV_DESCRIPTION)
        waitForHistoryPopulated()
        pullToRefreshHistory()
        
        val searchName = formatTextForSearch(oldPurpose)
        ensureObjectVisible(listOf(By.textContains(searchName)), "History entry $searchName")
        val item = waitForAnyObject(listOf(By.textContains(searchName)), DATA_TIMEOUT_MS)
        val bounds = item.visibleBounds
        device.click(bounds.centerX(), bounds.centerY())
        device.waitForIdle()
        
        tapAnyObjectCenterOrHierarchyBounds(
            selectors = listOf(By.text("Update outcome")),
            debugLabel = "Update outcome action",
            timeoutMs = SHORT_TIMEOUT_MS
        )
        
        focusFieldAndInputText(
            selectors = listOf(By.text("Purpose")),
            value = newPurposeSuffix,
            debugLabel = "Outcome Purpose update"
        )
        device.pressBack()
        device.waitForIdle()

        SystemClock.sleep(500)
        
        ensureObjectVisible(listOf(By.text("Update")), "Update button")
        tapObjectCenter(By.text("Update"), "Update button", FORM_TIMEOUT_MS)
        device.wait(Until.gone(By.text("Update")), SUBMIT_TIMEOUT_MS)
        
        return "$oldPurpose$newPurposeSuffix"
    }

    fun deleteTransactionFromHistory(transactionText: String) {
        tapNav(HISTORY_NAV_DESCRIPTION)
        waitForHistoryPopulated()
        pullToRefreshHistory()
        
        val searchName = formatTextForSearch(transactionText)
        ensureObjectVisible(listOf(By.textContains(searchName)), "History entry $searchName")
        val item = waitForAnyObject(listOf(By.textContains(searchName)), DATA_TIMEOUT_MS)
        val bounds = item.visibleBounds
        device.click(bounds.centerX(), bounds.centerY())
        device.waitForIdle()
        
        tapAnyObjectCenterOrHierarchyBounds(
            selectors = listOf(By.text("Delete order"), By.text("Delete outcome")),
            debugLabel = "Delete action",
            timeoutMs = SHORT_TIMEOUT_MS
        )
        
        tapObjectCenter(By.text("Delete"), "Confirm Delete", SHORT_TIMEOUT_MS)
        device.wait(Until.gone(By.text("Delete")), SUBMIT_TIMEOUT_MS)
        device.waitForIdle()
    }

    fun cleanUpAllE2eTransactions() {
        tapNav(HISTORY_NAV_DESCRIPTION)
        
        while (true) {
            SystemClock.sleep(500)
            val e2eItem = device.findObject(By.textContains("E2E")) ?: device.findObject(By.textContains("E2e")) ?: break
            
            val bounds = e2eItem.visibleBounds
            device.click(bounds.centerX(), bounds.centerY())
            device.waitForIdle()
            
            tapAnyObjectCenterOrHierarchyBounds(
                selectors = listOf(By.text("Delete order"), By.text("Delete outcome")),
                debugLabel = "Delete action",
                timeoutMs = SHORT_TIMEOUT_MS
            )
            
            tapObjectCenter(By.text("Delete"), "Confirm Delete", SHORT_TIMEOUT_MS)
            device.wait(Until.gone(By.text("Delete")), SUBMIT_TIMEOUT_MS)
            device.waitForIdle()
        }
    }

    enum class StartupState {
        HOME,
        SPREADSHEET_SETUP,
        ONBOARDING,
        UNKNOWN
    }

    companion object {
        private const val TARGET_PACKAGE = "com.raylabs.laundryhub"
        private const val MAIN_ACTIVITY_CLASS = "com.raylabs.laundryhub.ui.MainActivity"
        private const val LAUNCH_FLAGS = "0x10008000"
        private const val LOGIN_WITH_GOOGLE_TEXT = "Login with Google"
        private const val SPREADSHEET_SETUP_TITLE = "Set Up Your Spreadsheet"
        private const val HOME_NAV_DESCRIPTION = "Home"
        private const val HISTORY_NAV_DESCRIPTION = "History"
        private const val ORDER_NAV_DESCRIPTION = "Order"
        private const val OUTCOME_NAV_DESCRIPTION = "Outcome"
        private const val PROFILE_NAV_DESCRIPTION = "Profile"
        private const val ORDER_SHEET_DESCRIPTION = "Order sheet"
        private const val ORDER_NAME_FIELD_DESCRIPTION = "Order name field"
        private const val ORDER_PRICE_FIELD_DESCRIPTION = "Order price field"
        private const val ORDER_PACKAGE_OPTION_DESCRIPTION_PREFIX = "Package option "
        private const val ORDER_SUBMIT_BUTTON_DESCRIPTION = "Submit order"
        private const val ORDER_SUBMIT_SUCCESS_TEXT = "submitted successfully."
        private const val ADD_OUTCOME_DESCRIPTION = "Add Outcome"
        private const val PROFILE_INVENTORY_TEXT = "Inventory"
        private const val NO_TRANSACTIONS_TEXT = "No Transactions Today"
        private const val NAME_LABEL = "Name"
        private const val PRICE_LABEL = "Price"
        private val PACKAGE_OPTION_FALLBACK_TEXTS = setOf(
            "Reguler",
            "Regular",
            "Express - 24H",
            "Express - 6H"
        )
        private val NODE_BOUNDS_PATTERN = Regex("""bounds="\[(\d+),(\d+)]\[(\d+),(\d+)]"""")

        private const val STARTUP_TIMEOUT_MS = 30_000L
        private const val DATA_TIMEOUT_MS = 30_000L
        private const val FORM_TIMEOUT_MS = 15_000L
        private const val SUBMIT_TIMEOUT_MS = 45_000L
        private const val SHORT_TIMEOUT_MS = 5_000L
    }

    private data class NodeBounds(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    ) {
        val centerX: Int = (left + right) / 2
        val centerY: Int = (top + bottom) / 2
    }
}
