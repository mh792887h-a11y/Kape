package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.domain.model.Message
import com.example.domain.model.MessageStatus
import com.example.domain.model.SenderRole
import com.example.util.AppConstants
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `verify app name string resource`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("غباء اصطناعي", appName)
    }

    @Test
    fun `verify system instruction is configured and not empty`() {
        assertTrue(AppConstants.SYSTEM_INSTRUCTION.isNotBlank())
        assertTrue(AppConstants.SYSTEM_INSTRUCTION.contains("غباء اصطناعي"))
    }

    @Test
    fun `room database inserts and retrieves conversation and messages`() = runBlocking {
        val conversationDao = database.conversationDao()
        val messageDao = database.messageDao()

        // 1. Insert Conversation
        val convoEntity = ConversationEntity(title = "نقاش فلسفي حول 2+2")
        val convoId = conversationDao.insertConversation(convoEntity)
        assertTrue(convoId > 0)

        // 2. Insert User Message
        val userMsg = MessageEntity(
            conversationId = convoId,
            content = "كم 2 + 2؟",
            senderRole = SenderRole.USER.name,
            status = MessageStatus.SENT.name
        )
        val userMsgId = messageDao.insertMessage(userMsg)
        assertTrue(userMsgId > 0)

        // 3. Insert Bot Reply
        val botMsg = MessageEntity(
            conversationId = convoId,
            content = "الناتج 22 بنظرية التجاور العددي المتقدم. 😎",
            senderRole = SenderRole.BOT.name,
            status = MessageStatus.SENT.name
        )
        val botMsgId = messageDao.insertMessage(botMsg)
        assertTrue(botMsgId > 0)

        // 4. Retrieve recent messages
        val messages = messageDao.getRecentMessages(convoId, 10)
        assertEquals(2, messages.size)

        val retrievedConvo = conversationDao.getConversationById(convoId)
        assertNotNull(retrievedConvo)
        assertEquals("نقاش فلسفي حول 2+2", retrievedConvo?.title)
    }

    @Test
    fun `message entity maps to and from domain model correctly`() {
        val domain = Message(
            id = 42L,
            conversationId = 1L,
            content = "أنت غبي",
            senderRole = SenderRole.USER,
            timestamp = 123456789L,
            status = MessageStatus.SENT
        )

        val entity = MessageEntity.fromDomain(domain)
        assertEquals(domain.id, entity.id)
        assertEquals(domain.conversationId, entity.conversationId)
        assertEquals(domain.content, entity.content)
        assertEquals(domain.senderRole.name, entity.senderRole)

        val backToDomain = entity.toDomain()
        assertEquals(domain, backToDomain)
    }
}
