package com.example.data

import kotlinx.coroutines.flow.Flow

class TheiaRepository(private val dao: TheiaDao) {
    val allGorevler: Flow<List<Gorev>> = dao.getAllGorevler()
    val allMemoryLogs: Flow<List<MemoryLog>> = dao.getAllMemoryLogs()
    val allSessions: Flow<List<ChatSession>> = dao.getAllSessions()

    suspend fun insertGorev(gorev: Gorev) = dao.insertGorev(gorev)
    suspend fun updateGorevStatus(id: String, done: Boolean) = dao.updateGorevStatus(id, done)
    suspend fun deleteGorev(id: String) = dao.deleteGorev(id)

    suspend fun insertMemoryLog(log: MemoryLog) = dao.insertMemoryLog(log)
    suspend fun updateMemoryLogStatus(key: String, status: String) = dao.updateMemoryLogStatus(key, status)
    suspend fun deleteMemoryLog(key: String) = dao.deleteMemoryLog(key)

    suspend fun insertSession(session: ChatSession) = dao.insertSession(session)
    suspend fun deleteSession(id: String) {
        dao.deleteSession(id)
        dao.deleteMessagesForSession(id)
    }

    fun getMessages(sessionId: String): Flow<List<ChatMessage>> = dao.getMessagesForSession(sessionId)
    suspend fun getMessagesOnce(sessionId: String): List<ChatMessage> = dao.getMessagesForSessionOnce(sessionId)
    suspend fun insertMessage(message: ChatMessage) = dao.insertMessage(message)
}
