package com.raylabs.laundryhub.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.google.firebase.auth.FirebaseAuth
import com.raylabs.laundryhub.core.data.repository.CacheRepositoryImpl
import com.raylabs.laundryhub.core.data.repository.CurrentSpreadsheetIdProvider
import com.raylabs.laundryhub.core.data.repository.SettingsRepositoryImpl
import com.raylabs.laundryhub.core.data.repository.SpreadsheetConfigRepositoryImpl
import com.raylabs.laundryhub.core.domain.repository.CacheRepository
import com.raylabs.laundryhub.core.domain.repository.SettingsRepository
import com.raylabs.laundryhub.core.domain.repository.SpreadsheetConfigRepository
import com.raylabs.laundryhub.core.domain.repository.SpreadsheetIdProvider
import com.raylabs.laundryhub.core.domain.usecase.settings.ClearCacheUseCase
import com.raylabs.laundryhub.core.domain.usecase.settings.ClearSpreadsheetConnectionUseCase
import com.raylabs.laundryhub.core.domain.usecase.settings.GetCacheSizeUseCase
import com.raylabs.laundryhub.core.domain.usecase.settings.ObserveShowWhatsAppSettingUseCase
import com.raylabs.laundryhub.core.domain.usecase.settings.ObserveSpreadsheetConfigUseCase
import com.raylabs.laundryhub.core.domain.usecase.settings.SaveSpreadsheetConnectionUseCase
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
    fun provideSpreadsheetConfigRepository(
        dataStore: DataStore<Preferences>,
        firebaseAuth: FirebaseAuth
    ): SpreadsheetConfigRepository = SpreadsheetConfigRepositoryImpl(dataStore, firebaseAuth)

    @Provides
    @Singleton
    fun provideSpreadsheetIdProvider(
        repository: SpreadsheetConfigRepository
    ): SpreadsheetIdProvider = CurrentSpreadsheetIdProvider(repository)

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

    @Provides
    @Singleton
    fun provideObserveSpreadsheetConfigUseCase(
        repository: SpreadsheetConfigRepository
    ): ObserveSpreadsheetConfigUseCase = ObserveSpreadsheetConfigUseCase(repository)

    @Provides
    @Singleton
    fun provideSaveSpreadsheetConnectionUseCase(
        repository: SpreadsheetConfigRepository
    ): SaveSpreadsheetConnectionUseCase = SaveSpreadsheetConnectionUseCase(repository)

    @Provides
    @Singleton
    fun provideClearSpreadsheetConnectionUseCase(
        repository: SpreadsheetConfigRepository
    ): ClearSpreadsheetConnectionUseCase = ClearSpreadsheetConnectionUseCase(repository)
}
