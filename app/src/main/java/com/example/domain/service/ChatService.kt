package com.example.domain.service

import com.example.domain.model.Message

/**
 * واجهة مجردة لخدمة المحادثة تتيح الاستبدال المستقبلي بسهولة
 * بين الاتصال المباشر بـ Gemini API (في النسخة التجريبية)
 * وبين خادم وسيط آمن (BackendChatService) عند الإنتاج.
 */
interface ChatService {
    suspend fun generateReply(
        prompt: String,
        conversationHistory: List<Message>,
        apiKey: String,
        modelName: String
    ): Result<String>
}
