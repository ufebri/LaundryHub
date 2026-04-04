package com.raylabs.laundryhub.core.di

import com.raylabs.laundryhub.core.data.service.GoogleCredentialAuthManager
import com.raylabs.laundryhub.core.data.service.GoogleSheetsAuthorizationManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface GoogleAuthEntryPoint {
    fun googleCredentialAuthManager(): GoogleCredentialAuthManager
    fun googleSheetsAuthorizationManager(): GoogleSheetsAuthorizationManager
}
