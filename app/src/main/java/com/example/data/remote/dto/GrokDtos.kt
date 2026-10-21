package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GrokChatRequestDto(
    @Json(name = "model") val model: String,
    @Json(name = "messages") val messages: List<GrokMessageDto>,
    @Json(name = "temperature") val temperature: Float? = 0.85f,
    @Json(name = "stream") val stream: Boolean? = false
)

@JsonClass(generateAdapter = true)
data class GrokMessageDto(
    @Json(name = "role") val role: String, // "system", "user", "assistant"
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class GrokChatResponseDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "choices") val choices: List<GrokChoiceDto>? = null,
    @Json(name = "error") val error: GrokErrorDto? = null
)

@JsonClass(generateAdapter = true)
data class GrokChoiceDto(
    @Json(name = "index") val index: Int? = null,
    @Json(name = "message") val message: GrokMessageDto? = null,
    @Json(name = "finish_reason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class GrokErrorDto(
    @Json(name = "message") val message: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "code") val code: String? = null
)
