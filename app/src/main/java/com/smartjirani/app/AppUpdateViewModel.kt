package com.smartjirani.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartjirani.core.network.SupabaseClientProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.client.call.body
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

@Serializable
data class AppVersionInfo(
    val available: Boolean = false,
    val version: String? = null,
    val versionCode: Int? = null,
    val apkUrl: String? = null,
    val sizeMb: Double? = null,
    val changelog: List<String> = emptyList(),
)

@HiltViewModel
class AppUpdateViewModel
    @Inject
    constructor(
        private val supabaseClientProvider: SupabaseClientProvider,
    ) : ViewModel() {
        private val _update = MutableStateFlow<AppVersionInfo?>(null)
        val update: StateFlow<AppVersionInfo?> = _update.asStateFlow()

        fun checkForUpdate(currentVersionCode: Int) {
            viewModelScope.launch {
                runCatching {
                    val response = supabaseClientProvider.functions.invoke("app-version")
                    Json { ignoreUnknownKeys = true }
                        .decodeFromString<AppVersionInfo>(response.body<String>())
                }.onSuccess { info ->
                    if (info.available && (info.versionCode ?: 0) > currentVersionCode) {
                        _update.value = info
                    }
                }
            }
        }

        fun dismiss() {
            _update.value = null
        }
    }
