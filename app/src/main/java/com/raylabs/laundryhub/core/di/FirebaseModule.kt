package com.raylabs.laundryhub.core.di

import android.content.Context
import androidx.credentials.CredentialManager
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.appdistribution.FirebaseAppDistribution
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.raylabs.laundryhub.core.data.firebase.AppDistributionUpdateChecker
import com.raylabs.laundryhub.core.data.firebase.AuthRepositoryImpl
import com.raylabs.laundryhub.core.data.firebase.FirebaseDataSource
import com.raylabs.laundryhub.core.data.service.GoogleCredentialAuthManager
import com.raylabs.laundryhub.core.data.service.GoogleCredentialAuthManagerImpl
import com.raylabs.laundryhub.core.data.service.GoogleSheetsAccountProvider
import com.raylabs.laundryhub.core.data.service.GoogleSheetsAccountProviderImpl
import com.raylabs.laundryhub.core.data.service.GoogleSheetsAuthorizationManager
import com.raylabs.laundryhub.core.data.service.GoogleSheetsAuthorizationManagerImpl
import com.raylabs.laundryhub.core.domain.repository.AuthRepository
import com.raylabs.laundryhub.core.domain.repository.UpdateCheckerRepository
import com.raylabs.laundryhub.core.domain.usecase.auth.CheckUserLoggedInUseCase
import com.raylabs.laundryhub.core.domain.usecase.auth.SignInWithGoogleUseCase
import com.raylabs.laundryhub.core.domain.usecase.update.CheckAppUpdateUseCase
import com.raylabs.laundryhub.core.domain.usecase.user.UserUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    fun provideFirebaseAuthDataSource(
        firebaseAuth: FirebaseAuth,
        googleCredentialAuthManager: GoogleCredentialAuthManager
    ): FirebaseDataSource {
        return FirebaseDataSource(firebaseAuth, googleCredentialAuthManager)
    }

    @Provides
    fun provideAuthRepository(
        firebaseAuthDataSource: FirebaseDataSource
    ): AuthRepository {
        return AuthRepositoryImpl(firebaseAuthDataSource)
    }

    @Provides
    fun provideSignInWithGoogleUseCase(authRepository: AuthRepository): SignInWithGoogleUseCase {
        return SignInWithGoogleUseCase(authRepository)
    }

    @Provides
    fun provideCheckUserLoggedInUseCase(authRepository: AuthRepository): CheckUserLoggedInUseCase {
        return CheckUserLoggedInUseCase(authRepository)
    }

    @Provides
    @Singleton
    fun provideUserUseCase(authRepository: AuthRepository): UserUseCase {
        return UserUseCase(authRepository)
    }

    @Provides
    @Singleton
    fun provideGoogleCredentialAuthManager(
        credentialManager: CredentialManager
    ): GoogleCredentialAuthManager = GoogleCredentialAuthManagerImpl(credentialManager)

    @Provides
    @Singleton
    fun provideCredentialManager(
        @ApplicationContext context: Context
    ): CredentialManager = CredentialManager.create(context)

    @Provides
    @Singleton
    fun provideGoogleSheetsAccountProvider(
        firebaseAuth: FirebaseAuth
    ): GoogleSheetsAccountProvider = GoogleSheetsAccountProviderImpl(firebaseAuth)

    @Provides
    @Singleton
    fun provideGoogleSheetsAuthorizationManager(
        authorizationClient: AuthorizationClient,
        googleSheetsAccountProvider: GoogleSheetsAccountProvider
    ): GoogleSheetsAuthorizationManager =
        GoogleSheetsAuthorizationManagerImpl(authorizationClient, googleSheetsAccountProvider)

    @Provides
    @Singleton
    fun provideAuthorizationClient(
        @ApplicationContext context: Context
    ): AuthorizationClient = Identity.getAuthorizationClient(context)

    @Provides
    @Singleton
    fun provideCrashlytics(): FirebaseCrashlytics {
        return FirebaseCrashlytics.getInstance()
    }


    @Provides
    @Singleton
    fun provideFirebaseAppDistribution(): FirebaseAppDistribution =
        FirebaseAppDistribution.getInstance()

    @Provides
    @Singleton
    fun provideUpdateChecker(fad: FirebaseAppDistribution): UpdateCheckerRepository =
        AppDistributionUpdateChecker(fad)

    @Provides
    @Singleton
    fun provideCheckAppUpdateUseCase(
        repo: UpdateCheckerRepository
    ): CheckAppUpdateUseCase = CheckAppUpdateUseCase(repo)
}
