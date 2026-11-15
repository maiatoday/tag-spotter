package net.maiatoday.tagspotter.core.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomChatRepository(
    private val chatDao: ChatDao
) : ChatRepository {

    override suspend fun createOrUpdateSession(session: ChatSession) {
        chatDao.insertSession(
            ChatSessionEntity(
                sessionId = session.sessionId,
                title = session.title,
                createdAt = session.createdAt,
                lastActiveAt = session.lastActiveAt
            )
        )
    }

    override fun getAllSessions(): Flow<List<ChatSession>> {
        return chatDao.getAllSessions().map { entities ->
            entities.map { entity ->
                ChatSession(
                    sessionId = entity.sessionId,
                    title = entity.title,
                    createdAt = entity.createdAt,
                    lastActiveAt = entity.lastActiveAt
                )
            }
        }
    }

    override suspend fun getSessionById(sessionId: String): ChatSession? {
        val entity = chatDao.getSessionById(sessionId) ?: return null
        return ChatSession(
            sessionId = entity.sessionId,
            title = entity.title,
            createdAt = entity.createdAt,
            lastActiveAt = entity.lastActiveAt
        )
    }

    override suspend fun deleteSession(sessionId: String) {
        chatDao.deleteSessionById(sessionId)
    }

    override fun getMessagesForSession(sessionId: String): Flow<List<ChatMessage>> {
        return chatDao.getMessagesForSession(sessionId).map { entities ->
            entities.map { entity ->
                ChatMessage(
                    id = entity.id,
                    sessionId = entity.sessionId,
                    sender = entity.sender,
                    text = entity.text,
                    a2uiPayloadJson = entity.a2uiPayloadJson,
                    timestamp = entity.timestamp,
                    imagePath = entity.imagePath
                )
            }
        }
    }

    override suspend fun getMessagesListForSession(sessionId: String): List<ChatMessage> {
        return chatDao.getMessagesListForSession(sessionId).map { entity ->
            ChatMessage(
                id = entity.id,
                sessionId = entity.sessionId,
                sender = entity.sender,
                text = entity.text,
                a2uiPayloadJson = entity.a2uiPayloadJson,
                timestamp = entity.timestamp,
                imagePath = entity.imagePath
            )
        }
    }

    override suspend fun insertMessage(message: ChatMessage): Long {
        return chatDao.insertMessage(
            ChatMessageEntity(
                id = message.id,
                sessionId = message.sessionId,
                sender = message.sender,
                text = message.text,
                a2uiPayloadJson = message.a2uiPayloadJson,
                timestamp = message.timestamp,
                imagePath = message.imagePath
            )
        )
    }

    override suspend fun deleteMessagesForSession(sessionId: String) {
        chatDao.deleteMessagesForSession(sessionId)
    }
}
