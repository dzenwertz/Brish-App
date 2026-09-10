package com.katchy.focuslive.data.local.dao

import androidx.room.*
import com.katchy.focuslive.ui.chat.ChatMessage
import com.katchy.focuslive.ui.chat.ChatSession
import com.katchy.focuslive.ui.chat.UserAiMemory
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<ChatSession>>

    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): ChatSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSession)

    @Update
    suspend fun updateSession(session: ChatSession)

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: String)

    @Transaction
    suspend fun deleteSessionAndMessages(sessionId: String) {
        deleteMessagesForSession(sessionId)
        deleteSession(sessionId)
    }

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("UPDATE chat_messages SET actionsExecuted = :executed WHERE id = :messageId")
    suspend fun updateActionsExecuted(messageId: String, executed: Boolean)

    @Query("UPDATE chat_messages SET actionsDismissed = :dismissed WHERE id = :messageId")
    suspend fun updateActionsDismissed(messageId: String, dismissed: Boolean)

    @Query("SELECT * FROM user_ai_memory WHERE id = 'profile' LIMIT 1")
    fun getUserMemory(): Flow<UserAiMemory?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserMemory(memory: UserAiMemory)

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllMessages()

    @Query("DELETE FROM chat_sessions")
    suspend fun deleteAllSessions()

    @Query("DELETE FROM user_ai_memory")
    suspend fun deleteAllMemory()

    @Transaction
    suspend fun clearAll() {
        deleteAllMessages()
        deleteAllSessions()
        deleteAllMemory()
    }
}
