package com.example.data.repository

import com.example.BuildConfig
import com.example.data.local.ConversationDao
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageDao
import com.example.data.local.MessageEntity
import com.example.data.local.SettingsDataStore
import com.example.domain.model.AiProvider
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
    private val geminiChatService: ChatService,
    private val grokChatService: ChatService,
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

        // 3. Resolve Provider, API Key & Model
        val provider = settingsDataStore.selectedProvider.first()
        val (activeService, activeApiKey, activeModel) = resolveServiceAndCredentials(provider)

        if (activeApiKey.isBlank()) {
            val missingKeyError = if (provider == AiProvider.GROK) {
                "مفتاح Grok (xAI) غير متوفر! يرجى إدخال مفتاح Grok في الإعدادات."
            } else {
                "مفتاح Gemini API غير متوفر! يرجى إدخاله في الإعدادات."
            }
            val failedBotEntity = pendingBotEntity.copy(
                id = botMsgId,
                status = MessageStatus.ERROR.name,
                errorMessage = missingKeyError
            )
            messageDao.updateMessage(failedBotEntity)
            return@withContext Result.failure(IllegalStateException(missingKeyError))
        }

        // 4. Retrieve conversation history for context (exclude the pending bot message)
        val historyEntities = messageDao.getRecentMessages(conversationId, limit = 20)
            .filter { it.id != botMsgId && it.id != userMsgId }
            .sortedBy { it.timestamp }
        val history = historyEntities.map { it.toDomain() }

        // 5. Call ChatService
        val replyResult = activeService.generateReply(
            prompt = trimmedPrompt,
            conversationHistory = history,
            apiKey = activeApiKey,
            modelName = activeModel
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

        val provider = settingsDataStore.selectedProvider.first()
        val (activeService, activeApiKey, activeModel) = resolveServiceAndCredentials(provider)

        if (activeApiKey.isBlank()) {
            val errorText = "لم يتم ضبط مفتاح API لمزود الخدمة المختار ($provider)."
            val failedBotEntity = pendingEntity.copy(
                status = MessageStatus.ERROR.name,
                errorMessage = errorText
            )
            messageDao.updateMessage(failedBotEntity)
            return@withContext Result.failure(IllegalStateException(errorText))
        }

        val historyEntities = messageDao.getRecentMessages(conversationId, limit = 20)
            .filter { it.id != lastMessage.id && it.id != userPromptMsg.id }
            .sortedBy { it.timestamp }
        val history = historyEntities.map { it.toDomain() }

        val replyResult = activeService.generateReply(
            prompt = userPromptMsg.content,
            conversationHistory = history,
            apiKey = activeApiKey,
            modelName = activeModel
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

    private suspend fun resolveServiceAndCredentials(provider: AiProvider): Triple<ChatService, String, String> {
        return when (provider) {
            AiProvider.GROK -> {
                val grokKey = settingsDataStore.grokApiKey.first()
                val grokModel = settingsDataStore.selectedGrokModel.first()
                Triple(grokChatService, grokKey, grokModel)
            }
            AiProvider.GEMINI -> {
                val customKey = settingsDataStore.customApiKey.first()
                val buildConfigKey = try {
                    if (BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY") BuildConfig.GEMINI_API_KEY else ""
                } catch (e: Throwable) {
                    ""
                }
                val activeApiKey = if (customKey.isNotBlank()) customKey else buildConfigKey
                val geminiModel = settingsDataStore.selectedGeminiModel.first()
                Triple(geminiChatService, activeApiKey, geminiModel)
            }
        }
    }
}
