package net.maiatoday.tagspotter.core.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeChatRepository : ChatRepository {
    private val sessions = MutableStateFlow<Map<String, ChatSession>>(emptyMap())
    private val messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private var nextId = 1L

    override suspend fun createOrUpdateSession(session: ChatSession) {
        sessions.value = sessions.value + (session.sessionId to session)
    }

    override fun getAllSessions(): Flow<List<ChatSession>> =
        sessions.map { it.values.sortedByDescending { s -> s.lastActiveAt } }

    override suspend fun getSessionById(sessionId: String): ChatSession? =
        sessions.value[sessionId]

    override suspend fun deleteSession(sessionId: String) {
        sessions.value = sessions.value - sessionId
        messages.value = messages.value.filter { it.sessionId != sessionId }
    }

    override fun getMessagesForSession(sessionId: String): Flow<List<ChatMessage>> =
        messages.map { list -> list.filter { it.sessionId == sessionId }.sortedBy { it.timestamp } }

    override suspend fun getMessagesListForSession(sessionId: String): List<ChatMessage> =
        messages.value.filter { it.sessionId == sessionId }.sortedBy { it.timestamp }

    override suspend fun insertMessage(message: ChatMessage): Long {
        val id = nextId++
        val saved = message.copy(id = id)
        messages.value = messages.value + saved
        return id
    }

    override suspend fun deleteMessagesForSession(sessionId: String) {
        messages.value = messages.value.filter { it.sessionId != sessionId }
    }
}
