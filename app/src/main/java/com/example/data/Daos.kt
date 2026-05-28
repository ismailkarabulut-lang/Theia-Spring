package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TheiaDao {
    // Tasks (Gorev)
    @Query("SELECT * FROM gorevler ORDER BY created DESC")
    fun getAllGorevler(): Flow<List<Gorev>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGorev(gorev: Gorev)

    @Query("UPDATE gorevler SET done = :done WHERE id = :id")
    suspend fun updateGorevStatus(id: String, done: Boolean)

    @Query("DELETE FROM gorevler WHERE id = :id")
    suspend fun deleteGorev(id: String)

    // MemoryLogs (Vault)
    @Query("SELECT * FROM memory ORDER BY updatedAt DESC")
    fun getAllMemoryLogs(): Flow<List<MemoryLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemoryLog(log: MemoryLog)

    @Query("UPDATE memory SET status = :status WHERE `key` = :key")
    suspend fun updateMemoryLogStatus(key: String, status: String)

    @Query("DELETE FROM memory WHERE `key` = :key")
    suspend fun deleteMemoryLog(key: String)

    // ChatSession
    @Query("SELECT * FROM sessions ORDER BY ts DESC")
    fun getAllSessions(): Flow<List<ChatSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSession)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSession(id: String)

    // ChatMessage
    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY id ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessage>>

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY id ASC")
    suspend fun getMessagesForSessionOnce(sessionId: String): List<ChatMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: String)
}
