package com.raylabs.laundryhub.core.domain.model.sheets

import kotlinx.serialization.Serializable


@Serializable
data class SpreadsheetData(
    val key: String,
    val value: String
)