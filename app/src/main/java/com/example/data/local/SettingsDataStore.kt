package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.ThemeMode
import com.example.util.AppConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "stupid_ai_settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        private val KEY_API_KEY = stringPreferencesKey("custom_gemini_api_key")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_SELECTED_MODEL = stringPreferencesKey("selected_gemini_model")
    }

    val customApiKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_API_KEY] ?: ""
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        when (preferences[KEY_THEME_MODE]) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.SYSTEM.name -> ThemeMode.SYSTEM
            else -> ThemeMode.DARK // Default dark theme as requested
        }
    }

    val selectedModel: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_SELECTED_MODEL] ?: AppConstants.DEFAULT_MODEL
    }

    suspend fun setCustomApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_API_KEY] = apiKey.trim()
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode.name
        }
    }

    suspend fun setSelectedModel(model: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SELECTED_MODEL] = model
        }
    }

    suspend fun clearCustomApiKey() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_API_KEY)
        }
    }
}
