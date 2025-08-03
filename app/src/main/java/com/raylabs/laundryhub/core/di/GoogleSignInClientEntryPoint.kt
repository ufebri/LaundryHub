package com.raylabs.laundryhub.core.di

import com.google.android.gms.auth.api.signin.GoogleSignInClient
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface GoogleSignInClientEntryPoint {
    fun googleSignInClient(): GoogleSignInClient
}