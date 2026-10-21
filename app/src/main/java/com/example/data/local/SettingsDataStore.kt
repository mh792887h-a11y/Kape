package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AiProvider
import com.example.domain.model.ThemeMode
import com.example.util.AppConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "stupid_ai_settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        private val KEY_PROVIDER = stringPreferencesKey("selected_ai_provider")
        private val KEY_GEMINI_API_KEY = stringPreferencesKey("custom_gemini_api_key")
        private val KEY_GROK_API_KEY = stringPreferencesKey("custom_grok_api_key")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_SELECTED_GEMINI_MODEL = stringPreferencesKey("selected_gemini_model")
        private val KEY_SELECTED_GROK_MODEL = stringPreferencesKey("selected_grok_model")
    }

    val selectedProvider: Flow<AiProvider> = context.dataStore.data.map { preferences ->
        when (preferences[KEY_PROVIDER]) {
            AiProvider.GROK.name -> AiProvider.GROK
            else -> AiProvider.GEMINI
        }
    }

    val customApiKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_GEMINI_API_KEY] ?: ""
    }

    val grokApiKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_GROK_API_KEY] ?: ""
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        when (preferences[KEY_THEME_MODE]) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.SYSTEM.name -> ThemeMode.SYSTEM
            else -> ThemeMode.DARK
        }
    }

    val selectedGeminiModel: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_SELECTED_GEMINI_MODEL] ?: AppConstants.DEFAULT_GEMINI_MODEL
    }

    val selectedGrokModel: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_SELECTED_GROK_MODEL] ?: AppConstants.DEFAULT_GROK_MODEL
    }

    val selectedModel: Flow<String> = context.dataStore.data.map { preferences ->
        val provider = preferences[KEY_PROVIDER]
        if (provider == AiProvider.GROK.name) {
            preferences[KEY_SELECTED_GROK_MODEL] ?: AppConstants.DEFAULT_GROK_MODEL
        } else {
            preferences[KEY_SELECTED_GEMINI_MODEL] ?: AppConstants.DEFAULT_GEMINI_MODEL
        }
    }

    suspend fun setSelectedProvider(provider: AiProvider) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PROVIDER] = provider.name
        }
    }

    suspend fun setCustomApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_GEMINI_API_KEY] = apiKey.trim()
        }
    }

    suspend fun setGrokApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_GROK_API_KEY] = apiKey.trim()
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode.name
        }
    }

    suspend fun setSelectedModel(model: String) {
        context.dataStore.edit { preferences ->
            val provider = preferences[KEY_PROVIDER]
            if (provider == AiProvider.GROK.name) {
                preferences[KEY_SELECTED_GROK_MODEL] = model
            } else {
                preferences[KEY_SELECTED_GEMINI_MODEL] = model
            }
        }
    }

    suspend fun setSelectedGeminiModel(model: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SELECTED_GEMINI_MODEL] = model
        }
    }

    suspend fun setSelectedGrokModel(model: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SELECTED_GROK_MODEL] = model
        }
    }

    suspend fun clearCustomApiKey() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_GEMINI_API_KEY)
        }
    }

    suspend fun clearGrokApiKey() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_GROK_API_KEY)
        }
    }
}
