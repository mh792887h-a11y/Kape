package com.example.data.remote

import com.example.data.remote.dto.ContentDto
import com.example.data.remote.dto.GenerateContentRequestDto
import com.example.data.remote.dto.GenerationConfigDto
import com.example.data.remote.dto.PartDto
import com.example.domain.model.Message
import com.example.domain.model.SenderRole
import com.example.domain.service.ChatService
import com.example.util.AppConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class DirectGeminiChatService(
    private val apiService: GeminiApiService = ApiClient.geminiApiService
) : ChatService {

    override suspend fun generateReply(
        prompt: String,
        conversationHistory: List<Message>,
        apiKey: String,
        modelName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("لم يتم توفير مفتاح Gemini API. يرجى إضافته في الإعدادات.")
            )
        }

        try {
            // Build contents array from conversation history (respecting token/turn limits)
            val contentsList = mutableListOf<ContentDto>()

            val recentHistory = conversationHistory
                .takeLast(AppConstants.MAX_HISTORY_MESSAGES_FOR_CONTEXT)
                .filter { it.content.isNotBlank() }

            for (msg in recentHistory) {
                val role = if (msg.senderRole == SenderRole.USER) "user" else "model"
                contentsList.add(
                    ContentDto(
                        role = role,
                        parts = listOf(PartDto(text = msg.content))
                    )
                )
            }

            // Append current prompt as latest user turn
            contentsList.add(
                ContentDto(
                    role = "user",
                    parts = listOf(PartDto(text = prompt))
                )
            )

            // System instruction for Stupid AI personality
            val systemInstruction = ContentDto(
                parts = listOf(PartDto(text = AppConstants.SYSTEM_INSTRUCTION))
            )

            val request = GenerateContentRequestDto(
                contents = contentsList,
                systemInstruction = systemInstruction,
                generationConfig = GenerationConfigDto(
                    temperature = 0.85f,
                    topP = 0.95f,
                    topK = 40
                )
            )

            val response = apiService.generateContent(
                model = modelName.ifBlank { AppConstants.DEFAULT_MODEL },
                apiKey = apiKey,
                request = request
            )

            if (response.isSuccessful) {
                val body = response.body()
                val candidateText = body?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!candidateText.isNullOrBlank()) {
                    Result.success(candidateText.trim())
                } else {
                    Result.failure(Exception("لم يقم الروبوت بتوليد أي نص. حاول صياغة سؤالك بشكل آخر."))
                }
            } else {
                val errorCode = response.code()
                val errorMsg = when (errorCode) {
                    400 -> "خطأ في صيغة الطلب أو اسم النموذج ($errorCode). يرجى التأكد من اختيار نموذج متوافق في الإعدادات."
                    401, 403 -> "مفتاح Gemini API غير مصرح به أو منتهي الصلاحية ($errorCode). يرجى التحقق من المفتاح في الإعدادات."
                    429 -> "تم استهلاك حصة الاستخدام لمفتاحك (Quota Exceeded - $errorCode). يرجى الانتظار قليلاً ثم إعادة المحاولة."
                    500, 503 -> "خوادم Gemini مشغولة حالياً ($errorCode). حاول مرة أخرى بعد لحظات."
                    else -> "فشل الطلب مع رمز الخطأ ($errorCode)."
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("انتهت مهلة انتظار الرد من الخادم. يرجى إعادة المحاولة."))
        } catch (e: UnknownHostException) {
            Result.failure(Exception("لا يوجد اتصال بالإنترنت. يرجى التحقق من اتصال الشبكة."))
        } catch (e: IOException) {
            Result.failure(Exception("حدث خطأ في الاتصال بالشبكة: ${e.localizedMessage}"))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "حدث خطأ غير متوقع"))
        }
    }
}
