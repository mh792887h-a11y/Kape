package com.example.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.local.SettingsDataStore
import com.example.di.ServiceLocator
import com.example.domain.model.AiProvider
import com.example.domain.model.ThemeMode
import com.example.domain.repository.ChatRepository
import com.example.util.AppConstants
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application,
    private val settingsDataStore: SettingsDataStore,
    private val repository: ChatRepository
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application,
        ServiceLocator.provideSettingsDataStore(application),
        ServiceLocator.provideChatRepository(application)
    )

    val selectedProvider: StateFlow<AiProvider> = settingsDataStore.selectedProvider
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AiProvider.GEMINI)

    val themeMode: StateFlow<ThemeMode> = settingsDataStore.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.DARK)

    val selectedGeminiModel: StateFlow<String> = settingsDataStore.selectedGeminiModel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppConstants.DEFAULT_GEMINI_MODEL)

    val selectedGrokModel: StateFlow<String> = settingsDataStore.selectedGrokModel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppConstants.DEFAULT_GROK_MODEL)

    val geminiApiKey: StateFlow<String> = settingsDataStore.customApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val grokApiKey: StateFlow<String> = settingsDataStore.grokApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val hasBuildConfigGeminiKey: Boolean = try {
        BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"
    } catch (e: Throwable) {
        false
    }

    fun setSelectedProvider(provider: AiProvider) {
        viewModelScope.launch {
            settingsDataStore.setSelectedProvider(provider)
        }
    }

    fun setSelectedGeminiModel(model: String) {
        viewModelScope.launch {
            settingsDataStore.setSelectedGeminiModel(model)
        }
    }

    fun setSelectedGrokModel(model: String) {
        viewModelScope.launch {
            settingsDataStore.setSelectedGrokModel(model)
        }
    }

    fun saveGeminiApiKey(key: String) {
        viewModelScope.launch {
            settingsDataStore.setCustomApiKey(key)
        }
    }

    fun clearGeminiApiKey() {
        viewModelScope.launch {
            settingsDataStore.clearCustomApiKey()
        }
    }

    fun saveGrokApiKey(key: String) {
        viewModelScope.launch {
            settingsDataStore.setGrokApiKey(key)
        }
    }

    fun clearGrokApiKey() {
        viewModelScope.launch {
            settingsDataStore.clearGrokApiKey()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsDataStore.setThemeMode(mode)
        }
    }

    fun clearAllConversations(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.clearAllConversations()
            onDone()
        }
    }
}
