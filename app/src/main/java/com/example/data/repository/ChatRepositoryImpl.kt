package com.example.data.repository

import com.example.BuildConfig
import com.example.data.local.ConversationDao
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageDao
import com.example.data.local.MessageEntity
import com.example.data.local.SettingsDataStore
import com.example.domain.model.Conversation
import com.example.domain.model.Message
import com.example.domain.model.MessageStatus
import com.example.domain.model.SenderRole
import com.example.domain.repository.ChatRepository
import com.example.domain.service.ChatService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ChatRepositoryImpl(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val chatService: ChatService,
    private val settingsDataStore: SettingsDataStore
) : ChatRepository {

    override fun getConversations(): Flow<List<Conversation>> {
        return conversationDao.getAllConversations().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getConversationById(id: Long): Conversation? = withContext(Dispatchers.IO) {
        conversationDao.getConversationById(id)?.toDomain()
    }

    override suspend fun createConversation(initialTitle: String): Long = withContext(Dispatchers.IO) {
        val entity = ConversationEntity(
            title = initialTitle.ifBlank { "محادثة جديدة" }
        )
        conversationDao.insertConversation(entity)
    }

    override suspend fun updateConversationTitle(id: Long, title: String) = withContext(Dispatchers.IO) {
        conversationDao.updateTitle(id, title)
    }

    override suspend fun deleteConversation(id: Long) = withContext(Dispatchers.IO) {
        messageDao.deleteMessagesForConversation(id)
        conversationDao.deleteConversationById(id)
    }

    override suspend fun clearAllConversations() = withContext(Dispatchers.IO) {
        messageDao.deleteAllMessages()
        conversationDao.deleteAllConversations()
    }

    override fun getMessages(conversationId: Long): Flow<List<Message>> {
        return messageDao.getMessagesForConversation(conversationId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun sendMessage(conversationId: Long, prompt: String): Result<Message> = withContext(Dispatchers.IO) {
        val trimmedPrompt = prompt.trim()
        if (trimmedPrompt.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("لا يمكن إرسال رسالة فارغة"))
        }

        // Auto-title conversation if this is the first message or title is default
        val conversation = conversationDao.getConversationById(conversationId)
        if (conversation != null && (conversation.title == "محادثة جديدة" || conversation.title.isBlank())) {
            val autoTitle = if (trimmedPrompt.length > 28) {
                trimmedPrompt.take(28) + "…"
            } else {
                trimmedPrompt
            }
            conversationDao.updateTitle(conversationId, autoTitle)
        } else {
            conversationDao.updateTimestamp(conversationId)
        }

        // 1. Insert User Message
        val userEntity = MessageEntity(
            conversationId = conversationId,
            content = trimmedPrompt,
            senderRole = SenderRole.USER.name,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.SENT.name
        )
        val userMsgId = messageDao.insertMessage(userEntity)

        // 2. Insert Pending Bot Message
        val pendingBotEntity = MessageEntity(
            conversationId = conversationId,
            content = "",
            senderRole = SenderRole.BOT.name,
            timestamp = System.currentTimeMillis() + 1,
            status = MessageStatus.SENDING.name
        )
        val botMsgId = messageDao.insertMessage(pendingBotEntity)

        // 3. Resolve API Key & Model
        val customKey = settingsDataStore.customApiKey.first()
        val buildConfigKey = try {
            if (BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY") BuildConfig.GEMINI_API_KEY else ""
        } catch (e: Throwable) {
            ""
        }
        val activeApiKey = if (customKey.isNotBlank()) customKey else buildConfigKey
        val selectedModel = settingsDataStore.selectedModel.first()

        // 4. Retrieve conversation history for context (exclude the pending bot message)
        val historyEntities = messageDao.getRecentMessages(conversationId, limit = 20)
            .filter { it.id != botMsgId && it.id != userMsgId }
            .sortedBy { it.timestamp }
        val history = historyEntities.map { it.toDomain() }

        // 5. Call ChatService
        val replyResult = chatService.generateReply(
            prompt = trimmedPrompt,
            conversationHistory = history,
            apiKey = activeApiKey,
            modelName = selectedModel
        )

        replyResult.fold(
            onSuccess = { replyText ->
                val updatedBotEntity = pendingBotEntity.copy(
                    id = botMsgId,
                    content = replyText,
                    status = MessageStatus.SENT.name,
                    errorMessage = null
                )
                messageDao.updateMessage(updatedBotEntity)
                conversationDao.updateTimestamp(conversationId)
                Result.success(updatedBotEntity.toDomain())
            },
            onFailure = { error ->
                val failedBotEntity = pendingBotEntity.copy(
                    id = botMsgId,
                    content = "",
                    status = MessageStatus.ERROR.name,
                    errorMessage = error.localizedMessage ?: "فشل في توليد الرد"
                )
                messageDao.updateMessage(failedBotEntity)
                Result.failure(error)
            }
        )
    }

    override suspend fun retryLastMessage(conversationId: Long): Result<Message> = withContext(Dispatchers.IO) {
        val lastMessage = messageDao.getLastMessage(conversationId)
        if (lastMessage == null || lastMessage.status != MessageStatus.ERROR.name) {
            return@withContext Result.failure(IllegalStateException("لا توجد رسالة فاشلة لإعادة محاولتها"))
        }

        // Find the user message before this error
        val recentMessages = messageDao.getRecentMessages(conversationId, limit = 5)
        val userPromptMsg = recentMessages.firstOrNull { it.senderRole == SenderRole.USER.name }
            ?: return@withContext Result.failure(IllegalStateException("تعذر العثور على سؤال المستخدم الأصلي"))

        // Set bot message to SENDING
        val pendingEntity = lastMessage.copy(
            status = MessageStatus.SENDING.name,
            errorMessage = null
        )
        messageDao.updateMessage(pendingEntity)

        val customKey = settingsDataStore.customApiKey.first()
        val buildConfigKey = try {
            if (BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY") BuildConfig.GEMINI_API_KEY else ""
        } catch (e: Throwable) {
            ""
        }
        val activeApiKey = if (customKey.isNotBlank()) customKey else buildConfigKey
        val selectedModel = settingsDataStore.selectedModel.first()

        val historyEntities = messageDao.getRecentMessages(conversationId, limit = 20)
            .filter { it.id != lastMessage.id && it.id != userPromptMsg.id }
            .sortedBy { it.timestamp }
        val history = historyEntities.map { it.toDomain() }

        val replyResult = chatService.generateReply(
            prompt = userPromptMsg.content,
            conversationHistory = history,
            apiKey = activeApiKey,
            modelName = selectedModel
        )

        replyResult.fold(
            onSuccess = { replyText ->
                val updatedBotEntity = pendingEntity.copy(
                    content = replyText,
                    status = MessageStatus.SENT.name,
                    errorMessage = null
                )
                messageDao.updateMessage(updatedBotEntity)
                conversationDao.updateTimestamp(conversationId)
                Result.success(updatedBotEntity.toDomain())
            },
            onFailure = { error ->
                val failedBotEntity = pendingEntity.copy(
                    status = MessageStatus.ERROR.name,
                    errorMessage = error.localizedMessage ?: "فشل في توليد الرد"
                )
                messageDao.updateMessage(failedBotEntity)
                Result.failure(error)
            }
        )
    }
}
