package com.raylabs.laundryhub.core.data.service

object SpreadsheetIdParser {

    private val spreadsheetUrlRegex =
        Regex("""https?://docs\.google\.com/spreadsheets/d/([a-zA-Z0-9\-_]+)""")
    private val rawSpreadsheetIdRegex = Regex("""[a-zA-Z0-9\-_]{20,}""")

    fun normalize(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return null

        spreadsheetUrlRegex.find(trimmed)?.let { match ->
            return match.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() }
        }

        return trimmed.takeIf { rawSpreadsheetIdRegex.matches(it) }
    }
}
