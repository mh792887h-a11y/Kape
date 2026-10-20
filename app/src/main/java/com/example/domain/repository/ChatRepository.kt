package com.example.domain.repository

import com.example.domain.model.Conversation
import com.example.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getConversations(): Flow<List<Conversation>>
    suspend fun getConversationById(id: Long): Conversation?
    suspend fun createConversation(initialTitle: String = "محادثة جديدة"): Long
    suspend fun updateConversationTitle(id: Long, title: String)
    suspend fun deleteConversation(id: Long)
    suspend fun clearAllConversations()
    fun getMessages(conversationId: Long): Flow<List<Message>>
    suspend fun sendMessage(conversationId: Long, prompt: String): Result<Message>
    suspend fun retryLastMessage(conversationId: Long): Result<Message>
}
