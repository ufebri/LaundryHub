package com.raylabs.laundryhub.core.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val showWhatsAppOption: Flow<Boolean>
    suspend fun setShowWhatsAppOption(enabled: Boolean)
}
