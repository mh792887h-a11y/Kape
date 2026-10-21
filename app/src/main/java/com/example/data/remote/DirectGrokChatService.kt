package com.example.data.remote

import com.example.data.remote.dto.GrokChatRequestDto
import com.example.data.remote.dto.GrokMessageDto
import com.example.domain.model.Message
import com.example.domain.model.SenderRole
import com.example.domain.service.ChatService
import com.example.util.AppConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class DirectGrokChatService(
    private val apiService: GrokApiService = ApiClient.grokApiService
) : ChatService {

    override suspend fun generateReply(
        prompt: String,
        conversationHistory: List<Message>,
        apiKey: String,
        modelName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("لم يتم توفير مفتاح Grok API. يرجى إدخال مفتاح xAI في الإعدادات.")
            )
        }

        try {
            val messages = mutableListOf<GrokMessageDto>()

            // 1. System Instruction (Stupid AI Personality)
            messages.add(
                GrokMessageDto(
                    role = "system",
                    content = AppConstants.SYSTEM_INSTRUCTION
                )
            )

            // 2. Recent conversation history
            val recentHistory = conversationHistory
                .takeLast(AppConstants.MAX_HISTORY_MESSAGES_FOR_CONTEXT)
                .filter { it.content.isNotBlank() }

            for (msg in recentHistory) {
                val role = if (msg.senderRole == SenderRole.USER) "user" else "assistant"
                messages.add(
                    GrokMessageDto(
                        role = role,
                        content = msg.content
                    )
                )
            }

            // 3. Current User Prompt
            messages.add(
                GrokMessageDto(
                    role = "user",
                    content = prompt
                )
            )

            val effectiveModel = modelName.ifBlank { AppConstants.DEFAULT_GROK_MODEL }
            val request = GrokChatRequestDto(
                model = effectiveModel,
                messages = messages,
                temperature = 0.85f,
                stream = false
            )

            val authHeader = if (cleanKey.startsWith("Bearer ", ignoreCase = true)) {
                cleanKey
            } else {
                "Bearer $cleanKey"
            }

            val response = apiService.chatCompletions(
                authHeader = authHeader,
                request = request
            )

            if (response.isSuccessful) {
                val body = response.body()
                val reply = body?.choices?.firstOrNull()?.message?.content
                if (!reply.isNullOrBlank()) {
                    Result.success(reply.trim())
                } else {
                    Result.failure(Exception("لم يقم Grok بتوليد رد. حاول صياغة سؤالك بطريقة أخرى."))
                }
            } else {
                val errorCode = response.code()
                val errorMsg = when (errorCode) {
                    400 -> "خطأ في صيغة الطلب أو اسم نموذج Grok ($errorCode). تأكد من اختيار نموذج متاح في الإعدادات."
                    401, 403 -> "مفتاح Grok (xAI) غير مصرح به أو غير صالح ($errorCode). يرجى التحقق من المفتاح في الإعدادات."
                    429 -> "تم استهلاك حصة الاستخدام لمفتاح Grok (Quota Exceeded - $errorCode). يرجى الانتظار قليلاً."
                    500, 503 -> "خوادم xAI Grok مشغولة حالياً ($errorCode). حاول مرة أخرى بعد لحظات."
                    else -> "فشل طلب Grok مع رمز الخطأ ($errorCode)."
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("انتهت مهلة انتظار الرد من خوادم Grok. يرجى إعادة المحاولة."))
        } catch (e: UnknownHostException) {
            Result.failure(Exception("لا يوجد اتصال بالإنترنت. يرجى التحقق من الشبكة."))
        } catch (e: IOException) {
            Result.failure(Exception("حدث خطأ في الاتصال بخدمة Grok: ${e.localizedMessage}"))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "حدث خطأ غير متوقع مع Grok"))
        }
    }
}
