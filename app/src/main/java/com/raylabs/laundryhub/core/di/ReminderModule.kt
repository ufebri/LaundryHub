package com.raylabs.laundryhub.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.raylabs.laundryhub.core.data.repository.ReminderRepositoryImpl
import com.raylabs.laundryhub.core.domain.repository.ReminderRepository
import com.raylabs.laundryhub.core.reminder.AlarmReminderNotificationScheduler
import com.raylabs.laundryhub.core.reminder.ReminderNotificationScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ReminderModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideReminderRepository(
        dataStore: DataStore<Preferences>,
        firebaseAuth: FirebaseAuth,
        gson: Gson
    ): ReminderRepository = ReminderRepositoryImpl(
        dataStore = dataStore,
        firebaseAuth = firebaseAuth,
        gson = gson
    )

    @Provides
    @Singleton
    fun provideReminderNotificationScheduler(
        @ApplicationContext context: Context
    ): ReminderNotificationScheduler = AlarmReminderNotificationScheduler(context)
}
