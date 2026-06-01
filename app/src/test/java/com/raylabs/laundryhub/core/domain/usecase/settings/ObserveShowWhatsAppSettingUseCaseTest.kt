package com.raylabs.laundryhub.core.domain.usecase.settings

import com.raylabs.laundryhub.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ObserveShowWhatsAppSettingUseCaseTest {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var useCase: ObserveShowWhatsAppSettingUseCase

    @Before
    fun setUp() {
        settingsRepository = mock()
        useCase = ObserveShowWhatsAppSettingUseCase(settingsRepository)
    }

    @Test
    fun `invoke observes showWhatsAppOption from repository`() = runTest {
        whenever(settingsRepository.showWhatsAppOption).thenReturn(flowOf(true))

        val result = useCase().first()

        assertTrue(result)
    }
}
