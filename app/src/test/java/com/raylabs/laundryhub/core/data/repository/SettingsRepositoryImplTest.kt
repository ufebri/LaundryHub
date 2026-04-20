package com.raylabs.laundryhub.core.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryImplTest {

    @Test
    fun `showWhatsAppOption defaults to true`() = runTest {
        val repository = createRepository(backgroundScope)

        assertTrue(repository.showWhatsAppOption.first())
    }

    @Test
    fun `setShowWhatsAppOption persists toggled values`() = runTest {
        val repository = createRepository(backgroundScope)

        repository.setShowWhatsAppOption(false)
        assertFalse(repository.showWhatsAppOption.first())

        repository.setShowWhatsAppOption(true)
        assertTrue(repository.showWhatsAppOption.first())
    }

    private fun createRepository(scope: CoroutineScope): SettingsRepositoryImpl {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File.createTempFile("settings", ".preferences_pb") }
        )

        return SettingsRepositoryImpl(dataStore)
    }
}
