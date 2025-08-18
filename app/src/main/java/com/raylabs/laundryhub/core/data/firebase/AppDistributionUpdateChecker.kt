package com.raylabs.laundryhub.core.data.firebase

import com.google.firebase.appdistribution.FirebaseAppDistribution
import com.raylabs.laundryhub.core.domain.repository.UpdateCheckerRepository
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AppDistributionUpdateChecker(
    private val fad: FirebaseAppDistribution
) : UpdateCheckerRepository {

    override suspend fun checkAndPromptIfNeeded(): Boolean =
        suspendCancellableCoroutine { cont ->
            fad.updateIfNewReleaseAvailable()
                .addOnSuccessListener { cont.resume(true) }
                .addOnFailureListener { cont.resume(false) }
        }
}