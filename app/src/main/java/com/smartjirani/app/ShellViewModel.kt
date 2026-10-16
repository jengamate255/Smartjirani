package com.smartjirani.app

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartjirani.core.common.result.Resource
import com.smartjirani.core.datastore.UserPreferencesRepository
import com.smartjirani.core.network.SupabaseClientProvider
import com.smartjirani.feature.neighborhood.Neighborhood
import com.smartjirani.feature.neighborhood.NeighborhoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.gotrue.SessionStatus
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Shell-level state owner: keeps the locally selected mtaa in sync with the
 * memberships that actually exist on the server.
 *
 * The DataStore selection drives Feed/Ujamaa/Baraza queries, while RLS gates the
 * same queries on `neighborhood_members`. Whenever those two disagree (stale
 * pref, membership revoked elsewhere, fresh install), every screen silently
 * renders an empty state. Reconciling once per authenticated session — right
 * after the shell comes up — heals the drift at the source instead of in each
 * screen.
 */
@HiltViewModel
class ShellViewModel
    @Inject
    constructor(
        private val supabaseClient: SupabaseClientProvider,
        private val neighborhoodRepository: NeighborhoodRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
    ) : ViewModel() {
        private var reconciled = false

        init {
            // Self-trigger: waiting on the auth state flow directly avoids racing
            // other ViewModels' session-restore reads. No timeout — an anonymous
            // shell simply waits until a sign-in happens (reconcile is a no-op
            // until then), and the collection dies with this ViewModel.
            viewModelScope.launch {
                Log.d(TAG, "init: watching sessionStatus, now=${supabaseClient.auth.sessionStatus.value}")
                supabaseClient.auth.sessionStatus
                    .filter { it is SessionStatus.Authenticated }
                    .first()
                Log.d(TAG, "init: authenticated, reconciling")
                reconcileSelectedNeighborhood()
            }
        }

        fun reconcileSelectedNeighborhood() {
            if (reconciled) return
            reconciled = true
            viewModelScope.launch {
                when (val result = neighborhoodRepository.getMyNeighborhoods()) {
                    is Resource.Success -> {
                        Log.d(TAG, "reconcile: memberships=${result.data.map { it.id to it.name }}")
                        applyMembership(result.data)
                    }

                    // Offline/failure: keep the current selection — Room cache and the
                    // empty-state CTAs still work; the next session retries.
                    is Resource.Error -> {
                        Log.d(TAG, "reconcile: error=${result.message}")
                        reconciled = false
                    }

                    is Resource.Loading -> {
                        Unit
                    }
                }
            }
        }

        private suspend fun applyMembership(mine: List<Neighborhood>) {
            val prefs = userPreferencesRepository.userPreferences.first()
            val selected = prefs.selectedNeighborhoodId
            Log.d(TAG, "reconcile: selected=$selected name=${prefs.selectedNeighborhoodName}")
            when {
                mine.isEmpty() && selected != null -> {
                    Log.d(TAG, "reconcile: clearing stale selection")
                    userPreferencesRepository.clearSelectedNeighborhood()
                }

                mine.isEmpty() -> {
                    Unit
                }

                selected == null || mine.none { it.id == selected } -> {
                    val first = mine.first()
                    Log.d(TAG, "reconcile: switching to ${first.name} (${first.id})")
                    userPreferencesRepository.setSelectedNeighborhood(first.id, first.name)
                }

                prefs.selectedNeighborhoodName == null -> {
                    userPreferencesRepository.setSelectedNeighborhood(selected, mine.first { it.id == selected }.name)
                }

                else -> {
                    Log.d(TAG, "reconcile: selection valid, keeping")
                }
            }
        }

        private companion object {
            const val TAG = "ShellReconcile"
        }
    }
