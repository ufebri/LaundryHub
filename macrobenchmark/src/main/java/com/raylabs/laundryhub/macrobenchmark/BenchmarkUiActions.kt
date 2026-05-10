package com.raylabs.laundryhub.macrobenchmark

import android.os.SystemClock
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiObject2

internal val PACKAGE_OPTION_FALLBACK_TEXTS = setOf(
    "Reguler",
    "Regular",
    "Express - 24H",
    "Express - 6H",
)

internal fun MacrobenchmarkScope.tapAnyObjectCenterOrPackageFallback(
    selectors: List<BySelector>,
    debugLabel: String,
    timeoutMs: Long,
    packageOptionDescriptionPrefix: String,
    dumpWindowHierarchy: MacrobenchmarkScope.() -> String,
) {
    val deadline = SystemClock.uptimeMillis() + timeoutMs
    do {
        selectors.firstNotNullOfOrNull { selector ->
            device.findObject(selector)?.takeIf { it.hasVisibleBounds() }
        }?.let { target ->
            target.click()
            device.waitForIdle()
            return
        }

        findFirstPackageOptionBounds(
            hierarchy = dumpWindowHierarchy(),
            descriptionPrefix = packageOptionDescriptionPrefix,
        )?.let { bounds ->
            device.click(bounds.centerX, bounds.centerY)
            device.waitForIdle()
            return
        }

        SystemClock.sleep(RETRY_DELAY_MS)
    } while (SystemClock.uptimeMillis() < deadline)

    error("Could not find $debugLabel")
}

internal fun UiObject2.hasVisibleBounds(): Boolean {
    return runCatching {
        visibleBounds.width() > 0 && visibleBounds.height() > 0
    }.getOrDefault(false)
}

private fun findFirstPackageOptionBounds(
    hierarchy: String,
    descriptionPrefix: String,
): NodeBounds? {
    val descriptionMatch = NODE_PATTERN.findAll(hierarchy)
        .firstOrNull { node ->
            node.attributeValue("content-desc")?.startsWith(descriptionPrefix) == true
        }

    if (descriptionMatch != null) {
        return descriptionMatch.extractBounds()
    }

    return NODE_PATTERN.findAll(hierarchy)
        .firstOrNull { node ->
            node.attributeValue("text") in PACKAGE_OPTION_FALLBACK_TEXTS
        }
        ?.extractBounds()
}

private fun MatchResult.attributeValue(attribute: String): String? {
    return Regex("""\b${Regex.escape(attribute)}="([^"]*)"""")
        .find(value)
        ?.groupValues
        ?.get(1)
}

private fun MatchResult.extractBounds(): NodeBounds? {
    val values = BOUNDS_PATTERN.find(value)?.groupValues ?: return null
    val left = values[1].toIntOrNull() ?: return null
    val top = values[2].toIntOrNull() ?: return null
    val right = values[3].toIntOrNull() ?: return null
    val bottom = values[4].toIntOrNull() ?: return null
    if (right <= left || bottom <= top) return null
    return NodeBounds(
        left = left,
        top = top,
        right = right,
        bottom = bottom,
    )
}

private data class NodeBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val centerX: Int = (left + right) / 2
    val centerY: Int = (top + bottom) / 2
}

private const val RETRY_DELAY_MS = 100L
private val NODE_PATTERN = Regex("""<node\b[^>]*>""")
private val BOUNDS_PATTERN = Regex("""bounds="\[(\d+),(\d+)]\[(\d+),(\d+)]"""")
