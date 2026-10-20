package com.katchy.focuslive.ui.chat

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Keep
@Entity(
    tableName = "chat_messages",
    indices = [Index(value = ["sessionId"])]
)
data class ChatMessage(
    @PrimaryKey val id: String = "",
    val sessionId: String = "",
    val sender: String = "", // "USER" or "BRISH"
    val text: String = "",
    val timestamp: Long = 0,
    val actionPayload: String? = null,
    val actionsExecuted: Boolean = false,
    val actionsDismissed: Boolean = false
)

@Keep
@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey val id: String = "",
    val title: String = "Nueva conversación",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val previewText: String = "",
    val messageCount: Int = 0
)

@Keep
@Entity(tableName = "user_ai_memory")
data class UserAiMemory(
    @PrimaryKey val id: String = "profile",
    val summaryBio: String = "",
    val keyFacts: List<String> = emptyList(),
    val preferences: Map<String, String> = emptyMap(),
    val lastUpdated: Long = System.currentTimeMillis()
)

