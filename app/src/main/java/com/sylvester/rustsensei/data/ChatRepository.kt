package com.sylvester.rustsensei.data

import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatDao: ChatDao) {

    fun getConversations(): Flow<List<Conversation>> = chatDao.getAllConversations()

    fun getMessages(conversationId: Long): Flow<List<ChatMessage>> =
        chatDao.getMessagesForConversation(conversationId)

    suspend fun getMessagesOnce(conversationId: Long): List<ChatMessage> =
        chatDao.getMessagesForConversationOnce(conversationId)

    suspend fun createConversation(title: String = "New Conversation"): Long {
        return chatDao.insertConversation(Conversation(title = title))
    }

    // P1 Fix #15: preserve createdAt by reading the existing conversation first
    suspend fun addMessage(conversationId: Long, role: String, content: String): Long {
        val message = ChatMessage(
            conversationId = conversationId,
            role = role,
            content = content
        )
        val id = chatDao.insertMessage(message)

        // Update title only on first user message, preserve createdAt
        if (role == "user") {
            val existing = chatDao.getConversation(conversationId)
            if (existing != null) {
                chatDao.updateConversation(
                    existing.copy(
                        title = content.take(50),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
        return id
    }

    suspend fun clearAllData() {
        chatDao.deleteAllMessages()
        chatDao.deleteAllConversations()
    }

    suspend fun deleteConversation(conversationId: Long) {
        chatDao.deleteMessagesForConversation(conversationId)
        chatDao.deleteConversation(conversationId)
    }
}
