package com.example.data.chat

import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatDao: ChatDao) {
    val allMessages: Flow<List<ChatMessage>> = chatDao.getAllMessages()

    suspend fun insert(message: ChatMessage) {
        chatDao.insertMessage(message)
    }

    suspend fun clearHistory() {
        chatDao.clearHistory()
    }
}
