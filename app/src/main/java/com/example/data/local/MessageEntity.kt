package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.Message
import com.example.domain.model.MessageStatus
import com.example.domain.model.SenderRole

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["conversationId"])]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: Long,
    val content: String,
    val senderRole: String, // "USER" or "BOT"
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "SENT", // "SENDING", "SENT", "ERROR"
    val errorMessage: String? = null
) {
    fun toDomain(): Message = Message(
        id = id,
        conversationId = conversationId,
        content = content,
        senderRole = try {
            SenderRole.valueOf(senderRole)
        } catch (e: Exception) {
            SenderRole.BOT
        },
        timestamp = timestamp,
        status = try {
            MessageStatus.valueOf(status)
        } catch (e: Exception) {
            MessageStatus.SENT
        },
        errorMessage = errorMessage
    )

    companion object {
        fun fromDomain(domain: Message): MessageEntity = MessageEntity(
            id = domain.id,
            conversationId = domain.conversationId,
            content = domain.content,
            senderRole = domain.senderRole.name,
            timestamp = domain.timestamp,
            status = domain.status.name,
            errorMessage = domain.errorMessage
        )
    }
}
