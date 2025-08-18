package com.raylabs.laundryhub.core.di

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.appdistribution.FirebaseAppDistribution
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.raylabs.laundryhub.BuildConfig
import com.raylabs.laundryhub.core.data.firebase.AppDistributionUpdateChecker
import com.raylabs.laundryhub.core.data.firebase.AuthRepositoryImpl
import com.raylabs.laundryhub.core.data.firebase.FirebaseDataSource
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
        firebaseAuth: FirebaseAuth
    ): FirebaseDataSource {
        return FirebaseDataSource(firebaseAuth)
    }

    @Provides
    fun provideAuthRepository(
        firebaseAuthDataSource: FirebaseDataSource
    ): AuthRepository {
        return AuthRepositoryImpl(firebaseAuthDataSource)
    }

    // GoogleSignInOptions
    @Provides
    fun provideGoogleSignInOptions(): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.WEB_CLIENT_ID)
            .requestEmail()
            .build()
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
    fun provideGoogleSignInClient(
        @ApplicationContext context: Context,
        gso: GoogleSignInOptions
    ): GoogleSignInClient {
        return GoogleSignIn.getClient(context, gso)
    }

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
