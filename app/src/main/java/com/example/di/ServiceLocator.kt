package com.example.di

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.remote.ApiClient
import com.example.data.remote.DirectGeminiChatService
import com.example.data.remote.DirectGrokChatService
import com.example.data.repository.ChatRepositoryImpl
import com.example.domain.repository.ChatRepository
import com.example.domain.service.ChatService

object ServiceLocator {

    @Volatile
    private var database: AppDatabase? = null

    @Volatile
    private var settingsDataStore: SettingsDataStore? = null

    @Volatile
    private var geminiChatService: ChatService? = null

    @Volatile
    private var grokChatService: ChatService? = null

    @Volatile
    private var chatRepository: ChatRepository? = null

    fun provideDatabase(context: Context): AppDatabase {
        return database ?: synchronized(this) {
            database ?: AppDatabase.getInstance(context).also { database = it }
        }
    }

    fun provideSettingsDataStore(context: Context): SettingsDataStore {
        return settingsDataStore ?: synchronized(this) {
            settingsDataStore ?: SettingsDataStore(context.applicationContext).also { settingsDataStore = it }
        }
    }

    fun provideGeminiChatService(): ChatService {
        return geminiChatService ?: synchronized(this) {
            geminiChatService ?: DirectGeminiChatService(ApiClient.geminiApiService).also { geminiChatService = it }
        }
    }

    fun provideGrokChatService(): ChatService {
        return grokChatService ?: synchronized(this) {
            grokChatService ?: DirectGrokChatService(ApiClient.grokApiService).also { grokChatService = it }
        }
    }

    fun provideChatRepository(context: Context): ChatRepository {
        return chatRepository ?: synchronized(this) {
            val db = provideDatabase(context)
            val settings = provideSettingsDataStore(context)
            val geminiService = provideGeminiChatService()
            val grokService = provideGrokChatService()
            chatRepository ?: ChatRepositoryImpl(
                conversationDao = db.conversationDao(),
                messageDao = db.messageDao(),
                geminiChatService = geminiService,
                grokChatService = grokService,
                settingsDataStore = settings
            ).also { chatRepository = it }
        }
    }
}
