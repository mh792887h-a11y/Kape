package com.example.domain.model

data class Message(
    val id: Long = 0,
    val conversationId: Long,
    val content: String,
    val senderRole: SenderRole,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENT,
    val errorMessage: String? = null
)
