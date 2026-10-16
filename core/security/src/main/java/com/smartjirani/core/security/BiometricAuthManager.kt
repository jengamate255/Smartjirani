package com.smartjirani.core.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

sealed class BiometricAuthResult {
    data object Success : BiometricAuthResult()

    data class Error(
        val errorCode: Int,
        val message: String,
    ) : BiometricAuthResult()

    data object Cancelled : BiometricAuthResult()

    data object NotAvailable : BiometricAuthResult()
}

@Singleton
class BiometricAuthManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun canAuthenticate(): Boolean {
            val biometricManager = BiometricManager.from(context)
            return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
                BiometricManager.BIOMETRIC_SUCCESS -> true
                else -> false
            }
        }

        fun authenticate(
            activity: FragmentActivity,
            title: String = "SmartJirani",
            subtitle: String = "Thibitisha utambulisho wako",
            negativeButtonText: String = "Acha",
            onSuccess: () -> Unit,
            onError: (errorCode: Int, message: String) -> Unit,
            onCancelled: () -> Unit = {},
        ) {
            if (!canAuthenticate()) {
                onError(-1, "Biometric authentication not available")
                return
            }

            val executor = ContextCompat.getMainExecutor(context)
            val callback =
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }

                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence,
                    ) {
                        super.onAuthenticationError(errorCode, errString)
                        if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                            errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                            errorCode == BiometricPrompt.ERROR_CANCELED
                        ) {
                            onCancelled()
                        } else {
                            onError(errorCode, errString.toString())
                        }
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        // Don't treat as terminal error — user can retry
                    }
                }

            val biometricPrompt = BiometricPrompt(activity, executor, callback)
            val promptInfo =
                BiometricPrompt.PromptInfo
                    .Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setNegativeButtonText(negativeButtonText)
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .build()

            biometricPrompt.authenticate(promptInfo)
        }
    }
