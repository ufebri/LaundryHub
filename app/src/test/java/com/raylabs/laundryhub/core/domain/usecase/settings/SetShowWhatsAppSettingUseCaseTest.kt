package com.raylabs.laundryhub.core.domain.usecase.settings

import com.raylabs.laundryhub.core.domain.repository.SettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class SetShowWhatsAppSettingUseCaseTest {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var useCase: SetShowWhatsAppSettingUseCase

    @Before
    fun setUp() {
        settingsRepository = mock()
        useCase = SetShowWhatsAppSettingUseCase(settingsRepository)
    }

    @Test
    fun `invoke updates setShowWhatsAppOption on repository`() = runTest {
        useCase(true)

        verify(settingsRepository).setShowWhatsAppOption(true)
    }
}
