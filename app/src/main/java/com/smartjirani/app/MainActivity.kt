package com.smartjirani.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.smartjirani.core.datastore.UserPreferencesRepository
import com.smartjirani.core.designsystem.theme.SmartJiraniTheme
import com.smartjirani.core.security.BiometricAuthResult
import com.smartjirani.core.security.BiometricAuthUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject
    lateinit var biometricAuthUseCase: BiometricAuthUseCase

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    /** Set when the app leaves the foreground; the next start must unlock first. */
    private var needsUnlock = true

    /**
     * Gates the whole NavGraph: while true only a black surface composes, so
     * feed content never sits visible behind the biometric sheet.
     */
    private var isLocked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartJiraniTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    if (isLocked) {
                        Box(modifier = Modifier.fillMaxSize())
                    } else {
                        SmartJiraniNavGraph()
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Backgrounding re-arms the lock; the prompt itself does not fire onStop.
        needsUnlock = true
    }

    override fun onStart() {
        super.onStart()
        if (!needsUnlock) return
        lifecycleScope.launch {
            val appLockEnabled = userPreferencesRepository.userPreferences.first().appLockEnabled
            if (!appLockEnabled || !biometricAuthUseCase.isBiometricAvailable()) {
                needsUnlock = false
                return@launch
            }
            isLocked = true
            when (
                biometricAuthUseCase.requireBiometricAuth(activity = this@MainActivity)
            ) {
                is BiometricAuthResult.Success -> {
                    isLocked = false
                    needsUnlock = false
                }

                // Cancel/error: keep the black gate and send the app to the
                // background — onStop re-arms the lock for the next foreground.
                else -> {
                    moveTaskToBack(true)
                }
            }
        }
    }
}
