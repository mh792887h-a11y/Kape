package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenerateContentRequestDto(
    @Json(name = "contents") val contents: List<ContentDto>,
    @Json(name = "systemInstruction") val systemInstruction: ContentDto? = null,
    @Json(name = "generationConfig") val generationConfig: GenerationConfigDto? = null
)

@JsonClass(generateAdapter = true)
data class ContentDto(
    @Json(name = "role") val role: String? = null,
    @Json(name = "parts") val parts: List<PartDto>
)

@JsonClass(generateAdapter = true)
data class PartDto(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfigDto(
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "topP") val topP: Float? = null,
    @Json(name = "topK") val topK: Int? = null,
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponseDto(
    @Json(name = "candidates") val candidates: List<CandidateDto>? = null,
    @Json(name = "error") val error: GeminiErrorDto? = null
)

@JsonClass(generateAdapter = true)
data class CandidateDto(
    @Json(name = "content") val content: ContentDto? = null,
    @Json(name = "finishReason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiErrorDto(
    @Json(name = "code") val code: Int? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "status") val status: String? = null
)
