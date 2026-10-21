package com.example.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.local.SettingsDataStore
import com.example.di.ServiceLocator
import com.example.domain.model.AiProvider
import com.example.domain.model.Conversation
import com.example.domain.model.Message
import com.example.domain.model.MessageStatus
import com.example.domain.repository.ChatRepository
import com.example.util.AppConstants
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(
    application: Application,
    private val repository: ChatRepository,
    private val settingsDataStore: SettingsDataStore
) : AndroidViewModel(application) {

    // Secondary constructor required by AndroidViewModelFactory reflection
    constructor(application: Application) : this(
        application,
        ServiceLocator.provideChatRepository(application),
        ServiceLocator.provideSettingsDataStore(application)
    )

    val conversations: StateFlow<List<Conversation>> = repository.getConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentConversationId = MutableStateFlow<Long?>(null)
    val currentConversationId: StateFlow<Long?> = _currentConversationId.asStateFlow()

    val messages: StateFlow<List<Message>> = _currentConversationId.flatMapLatest { id ->
        if (id == null) {
            flowOf(emptyList())
        } else {
            repository.getMessages(id)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    val selectedProvider: StateFlow<AiProvider> = settingsDataStore.selectedProvider
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AiProvider.GEMINI)

    val activeModel: StateFlow<String> = settingsDataStore.selectedModel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppConstants.DEFAULT_GEMINI_MODEL)

    val isApiKeyConfigured: StateFlow<Boolean> = combine(
        settingsDataStore.selectedProvider,
        settingsDataStore.customApiKey,
        settingsDataStore.grokApiKey
    ) { provider, geminiKey, grokKey ->
        when (provider) {
            AiProvider.GROK -> grokKey.isNotBlank()
            AiProvider.GEMINI -> {
                val buildConfigKey = try {
                    if (BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY") BuildConfig.GEMINI_API_KEY else ""
                } catch (e: Throwable) {
                    ""
                }
                geminiKey.isNotBlank() || buildConfigKey.isNotBlank()
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    init {
        // Automatically select the most recently updated conversation if available
        viewModelScope.launch {
            val list = repository.getConversations().first()
            if (list.isNotEmpty()) {
                _currentConversationId.value = list.first().id
            }
        }
    }

    fun onInputTextChanged(newText: String) {
        _inputText.value = newText
    }

    fun startNewConversation() {
        viewModelScope.launch {
            val newId = repository.createConversation("محادثة جديدة")
            _currentConversationId.value = newId
            _inputText.value = ""
        }
    }

    fun selectConversation(id: Long) {
        _currentConversationId.value = id
    }

    fun deleteConversation(id: Long) {
        viewModelScope.launch {
            repository.deleteConversation(id)
            if (_currentConversationId.value == id) {
                val remaining = repository.getConversations().first()
                _currentConversationId.value = remaining.firstOrNull()?.id
            }
        }
    }

    fun sendMessage(overridePrompt: String? = null) {
        val promptToSend = overridePrompt ?: _inputText.value
        val trimmed = promptToSend.trim()
        if (trimmed.isBlank() || _isGenerating.value) return

        viewModelScope.launch {
            _isGenerating.value = true
            try {
                var convoId = _currentConversationId.value
                if (convoId == null) {
                    convoId = repository.createConversation("محادثة جديدة")
                    _currentConversationId.value = convoId
                }

                if (overridePrompt == null) {
                    _inputText.value = ""
                }

                repository.sendMessage(convoId, trimmed)
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun retryLastMessage() {
        val convoId = _currentConversationId.value ?: return
        if (_isGenerating.value) return

        viewModelScope.launch {
            _isGenerating.value = true
            try {
                repository.retryLastMessage(convoId)
            } finally {
                _isGenerating.value = false
            }
        }
    }
}
