package com.raylabs.laundryhub.core.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.raylabs.laundryhub.core.data.repository.SettingsRepositoryImpl
import com.raylabs.laundryhub.core.domain.repository.SettingsRepository
import com.raylabs.laundryhub.core.domain.usecase.settings.ObserveShowWhatsAppSettingUseCase
import com.raylabs.laundryhub.core.domain.usecase.settings.SetShowWhatsAppSettingUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {

    @Provides
    @Singleton
    fun provideSettingsRepository(
        dataStore: DataStore<Preferences>
    ): SettingsRepository = SettingsRepositoryImpl(dataStore)

    @Provides
    @Singleton
    fun provideObserveShowWhatsAppSettingUseCase(
        repository: SettingsRepository
    ): ObserveShowWhatsAppSettingUseCase = ObserveShowWhatsAppSettingUseCase(repository)

    @Provides
    @Singleton
    fun provideSetShowWhatsAppSettingUseCase(
        repository: SettingsRepository
    ): SetShowWhatsAppSettingUseCase = SetShowWhatsAppSettingUseCase(repository)
}
