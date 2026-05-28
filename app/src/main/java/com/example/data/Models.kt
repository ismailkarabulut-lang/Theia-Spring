package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gorevler")
data class Gorev(
    @PrimaryKey
    val id: String,
    val title: String,
    val date: String, // "YYYY-MM-DD"
    val time: String, // "HH:MM" or empty
    val reminderTime: String, // "HH:MM" or empty
    val repeat: String, // "yok", "gunluk", "haftalik", "ozel"
    val category: String, // "saglik", "kisisel", "is"
    val type: String, // "gorev", "rutin"
    val done: Boolean,
    val created: String
)

@Entity(tableName = "memory")
data class MemoryLog(
    @PrimaryKey
    val key: String,
    val value: String,
    val entryType: String, // "core", "daily_summary", "memory"
    val updatedAt: String, // ISO timestamp
    val status: String // "active", "passive", "archived" (Scout decay status)
)

@Entity(tableName = "sessions")
data class ChatSession(
    @PrimaryKey
    val id: String,
    val model: String, // "claude", "deepseek", "kimi", "ollama"
    val modelColor: String,
    val ts: Long,
    val preview: String
)

@Entity(tableName = "messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val sessionId: String,
    val role: String, // "user", "assistant", "system"
    val content: String,
    val time: String, // "HH:MM:SS"
    val model: String
)
