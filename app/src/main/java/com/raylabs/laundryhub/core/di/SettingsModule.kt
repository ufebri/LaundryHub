package com.raylabs.laundryhub.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.raylabs.laundryhub.core.data.repository.CacheRepositoryImpl
import com.raylabs.laundryhub.core.data.repository.SettingsRepositoryImpl
import com.raylabs.laundryhub.core.domain.repository.CacheRepository
import com.raylabs.laundryhub.core.domain.repository.SettingsRepository
import com.raylabs.laundryhub.core.domain.usecase.settings.ClearCacheUseCase
import com.raylabs.laundryhub.core.domain.usecase.settings.GetCacheSizeUseCase
import com.raylabs.laundryhub.core.domain.usecase.settings.ObserveShowWhatsAppSettingUseCase
import com.raylabs.laundryhub.core.domain.usecase.settings.SetShowWhatsAppSettingUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
    fun provideCacheRepository(
        @ApplicationContext context: Context
    ): CacheRepository = CacheRepositoryImpl(context)

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

    @Provides
    @Singleton
    fun provideGetCacheSizeUseCase(
        repository: CacheRepository
    ): GetCacheSizeUseCase = GetCacheSizeUseCase(repository)

    @Provides
    @Singleton
    fun provideClearCacheUseCase(
        repository: CacheRepository
    ): ClearCacheUseCase = ClearCacheUseCase(repository)
}
