package com.smartjirani.core.security

import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricAuthUseCase
    @Inject
    constructor(
        private val biometricAuthManager: BiometricAuthManager,
    ) {
        suspend fun requireBiometricAuth(
            activity: FragmentActivity,
            title: String = "SmartJirani",
            subtitle: String = "Thibitisha utambulisho wako",
        ): BiometricAuthResult =
            suspendCancellableCoroutine { continuation ->
                biometricAuthManager.authenticate(
                    activity = activity,
                    title = title,
                    subtitle = subtitle,
                    onSuccess = {
                        if (continuation.isActive) {
                            continuation.resumeWith(Result.success(BiometricAuthResult.Success))
                        }
                    },
                    onError = { errorCode, message ->
                        if (continuation.isActive) {
                            continuation.resumeWith(Result.success(BiometricAuthResult.Error(errorCode, message)))
                        }
                    },
                    onCancelled = {
                        if (continuation.isActive) {
                            continuation.resumeWith(Result.success(BiometricAuthResult.Cancelled))
                        }
                    },
                )
            }

        fun isBiometricAvailable(): Boolean = biometricAuthManager.canAuthenticate()
    }
