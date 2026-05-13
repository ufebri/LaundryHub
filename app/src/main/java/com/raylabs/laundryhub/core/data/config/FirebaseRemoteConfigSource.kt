package com.raylabs.laundryhub.core.data.config

import com.google.android.gms.tasks.Task
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseRemoteConfigSource(
    private val remoteConfig: FirebaseRemoteConfig
) : RemoteConfigSource {

    init {
        remoteConfig.setConfigSettingsAsync(
            FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(DEFAULT_FETCH_INTERVAL_SECONDS)
                .build()
        )
    }

    override suspend fun refresh(force: Boolean): Boolean {
        return runCatching {
            if (force) {
                remoteConfig.fetch(FORCE_FETCH_INTERVAL_SECONDS).awaitTask()
                remoteConfig.activate().awaitTask()
            } else {
                remoteConfig.fetchAndActivate().awaitTask()
            }
        }.getOrDefault(false)
    }

    override fun getString(key: String): String = remoteConfig.getString(key)

    override fun getBoolean(key: String): Boolean = remoteConfig.getBoolean(key)

    override fun getLong(key: String): Long = remoteConfig.getLong(key)

    private companion object {
        const val DEFAULT_FETCH_INTERVAL_SECONDS = 3600L
        const val FORCE_FETCH_INTERVAL_SECONDS = 0L
    }
}

private suspend fun <T> Task<T>.awaitTask(): T {
    return suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            when {
                task.isSuccessful -> continuation.resume(task.result)
                task.isCanceled -> continuation.resumeWithException(
                    CancellationException("Firebase Remote Config task was cancelled")
                )
                else -> continuation.resumeWithException(
                    task.exception ?: IllegalStateException("Firebase Remote Config task failed")
                )
            }
        }
    }
}
