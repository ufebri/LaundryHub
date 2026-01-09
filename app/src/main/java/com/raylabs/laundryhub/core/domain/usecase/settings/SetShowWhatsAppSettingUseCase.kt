package com.raylabs.laundryhub.core.domain.usecase.settings

import com.raylabs.laundryhub.core.domain.repository.SettingsRepository
import javax.inject.Inject

class SetShowWhatsAppSettingUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(enabled: Boolean) {
        repository.setShowWhatsAppOption(enabled)
    }
}
