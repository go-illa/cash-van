package com.illa.cashvan.core.notifications

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

class FcmTokenProvider {

    companion object {
        private val TOKEN_TIMEOUT = 5.seconds
    }

    // Returns null instead of throwing so a missing token never blocks login.
    suspend fun getToken(): String? = withTimeoutOrNull(TOKEN_TIMEOUT) {
        suspendCancellableCoroutine { continuation ->
            try {
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        continuation.resume(task.result)
                    } else {
                        Timber.e(task.exception, "Failed to fetch FCM token")
                        continuation.resume(null)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch FCM token")
                continuation.resume(null)
            }
        }
    }?.takeIf { it.isNotBlank() }
}
