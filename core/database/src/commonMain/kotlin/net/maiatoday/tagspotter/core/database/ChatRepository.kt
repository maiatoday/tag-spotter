package net.maiatoday.tagspotter.core.database

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class ChatSession(
    val sessionId: String,
    val title: String,
    val createdAt: Long,
    val lastActiveAt: Long = createdAt
)

@Serializable
data class ChatMessage(
    val id: Long = 0,
    val sessionId: String,
    val sender: String, // "user", "agent", "system"
    val text: String,
    val a2uiPayloadJson: String? = null,
    val timestamp: Long,
    val imagePath: String? = null
)

interface ChatRepository {
    suspend fun createOrUpdateSession(session: ChatSession)
    fun getAllSessions(): Flow<List<ChatSession>>
    suspend fun getSessionById(sessionId: String): ChatSession?
    suspend fun deleteSession(sessionId: String)
    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessage>>
    suspend fun getMessagesListForSession(sessionId: String): List<ChatMessage>
    suspend fun insertMessage(message: ChatMessage): Long
    suspend fun deleteMessagesForSession(sessionId: String)
}
