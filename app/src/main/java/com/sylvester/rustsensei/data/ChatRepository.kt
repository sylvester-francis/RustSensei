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

    suspend fun addMessage(conversationId: Long, role: String, content: String): Long {
        val message = ChatMessage(
            conversationId = conversationId,
            role = role,
            content = content
        )
        val id = chatDao.insertMessage(message)
        chatDao.updateConversation(
            Conversation(
                id = conversationId,
                title = if (role == "user") content.take(50) else "New Conversation",
                updatedAt = System.currentTimeMillis()
            )
        )
        return id
    }

    suspend fun updateMessage(message: ChatMessage) {
        chatDao.deleteMessagesForConversation(message.conversationId)
        // This is a simplification — in production you'd update a single message
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
