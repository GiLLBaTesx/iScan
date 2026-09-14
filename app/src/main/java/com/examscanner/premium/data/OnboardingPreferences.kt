package com.examscanner.premium.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore for onboarding preferences
 */
private val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "onboarding_preferences"
)

class OnboardingPreferences(private val context: Context) {
    
    companion object {
        private val HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
    }
    
    /**
     * Get whether user has completed onboarding
     */
    val hasSeenOnboarding: Flow<Boolean> = context.onboardingDataStore.data
        .map { preferences ->
            preferences[HAS_SEEN_ONBOARDING] ?: false
        }
    
    /**
     * Mark onboarding as completed
     */
    suspend fun setOnboardingCompleted() {
        context.onboardingDataStore.edit { preferences ->
            preferences[HAS_SEEN_ONBOARDING] = true
        }
    }
    
    /**
     * Reset onboarding (for testing)
     */
    suspend fun resetOnboarding() {
        context.onboardingDataStore.edit { preferences ->
            preferences[HAS_SEEN_ONBOARDING] = false
        }
    }
}
