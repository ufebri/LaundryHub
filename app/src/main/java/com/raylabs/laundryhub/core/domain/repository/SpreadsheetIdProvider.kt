package com.raylabs.laundryhub.core.domain.repository

interface SpreadsheetIdProvider {
    suspend fun getSpreadsheetId(): String?
}
