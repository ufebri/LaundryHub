package com.raylabs.laundryhub.core.di

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.raylabs.laundryhub.BuildConfig
import com.raylabs.laundryhub.core.data.config.BackendEndpointValidator
import com.raylabs.laundryhub.core.data.config.FirebaseRemoteConfigSource
import com.raylabs.laundryhub.core.data.config.KtorBackendHealthChecker
import com.raylabs.laundryhub.core.data.config.RemoteBackendConfigProvider
import com.raylabs.laundryhub.core.data.config.RemoteConfigSource
import com.raylabs.laundryhub.core.domain.config.BackendConfigProvider
import com.raylabs.laundryhub.core.domain.config.BackendHealthChecker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BackendConfigModule {

    @Provides
    @Singleton
    fun provideFirebaseRemoteConfig(): FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    @Provides
    @Singleton
    fun provideRemoteConfigSource(
        remoteConfig: FirebaseRemoteConfig
    ): RemoteConfigSource = FirebaseRemoteConfigSource(remoteConfig)

    @Provides
    @Singleton
    fun provideBackendEndpointValidator(): BackendEndpointValidator = BackendEndpointValidator()

    @Provides
    @Singleton
    fun provideBackendConfigProvider(
        remoteConfigSource: RemoteConfigSource,
        endpointValidator: BackendEndpointValidator
    ): BackendConfigProvider {
        return RemoteBackendConfigProvider(
            remoteConfigSource = remoteConfigSource,
            endpointValidator = endpointValidator,
            fallbackBaseUrl = BuildConfig.BASE_URL
        )
    }

    @Provides
    @Singleton
    fun provideBackendHealthChecker(): BackendHealthChecker = KtorBackendHealthChecker()
}
