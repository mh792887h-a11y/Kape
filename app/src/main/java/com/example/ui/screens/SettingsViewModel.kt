package com.example.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.local.SettingsDataStore
import com.example.di.ServiceLocator
import com.example.domain.model.ThemeMode
import com.example.domain.repository.ChatRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application,
    private val settingsDataStore: SettingsDataStore,
    private val repository: ChatRepository
) : AndroidViewModel(application) {

    // Secondary constructor required by AndroidViewModelFactory reflection
    constructor(application: Application) : this(
        application,
        ServiceLocator.provideSettingsDataStore(application),
        ServiceLocator.provideChatRepository(application)
    )

    val themeMode: StateFlow<ThemeMode> = settingsDataStore.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.DARK)

    val selectedModel: StateFlow<String> = settingsDataStore.selectedModel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "gemini-3.8-flash")

    val customApiKey: StateFlow<String> = settingsDataStore.customApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val hasBuildConfigKey: Boolean = try {
        BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"
    } catch (e: Throwable) {
        false
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsDataStore.setThemeMode(mode)
        }
    }

    fun setSelectedModel(model: String) {
        viewModelScope.launch {
            settingsDataStore.setSelectedModel(model)
        }
    }

    fun saveCustomApiKey(key: String) {
        viewModelScope.launch {
            settingsDataStore.setCustomApiKey(key)
        }
    }

    fun clearCustomApiKey() {
        viewModelScope.launch {
            settingsDataStore.clearCustomApiKey()
        }
    }

    fun clearAllConversations(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.clearAllConversations()
            onDone()
        }
    }
}
