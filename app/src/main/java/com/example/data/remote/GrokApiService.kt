package com.example.data.remote

import com.example.data.remote.dto.GrokChatRequestDto
import com.example.data.remote.dto.GrokChatResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface GrokApiService {
    @POST("v1/chat/completions")
    suspend fun chatCompletions(
        @Header("Authorization") authHeader: String,
        @Body request: GrokChatRequestDto
    ): Response<GrokChatResponseDto>
}
