package net.maiatoday.tagspotter.core.sync

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import net.maiatoday.tagspotter.core.database.FakeSpotRepository
import net.maiatoday.tagspotter.core.model.Spot
import net.maiatoday.tagspotter.core.model.SpotDetails
import net.maiatoday.tagspotter.core.model.SpotOrigin
import net.maiatoday.tagspotter.core.settings.SecureStorage
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private class InMemorySecureStorage : SecureStorage {
    private val map = mutableMapOf<String, String>()
    override fun getString(key: String, defaultValue: String): String = map[key] ?: defaultValue
    override fun putString(key: String, value: String) {
        map[key] = value
    }
}

class JvmCanonicalSpotsSyncTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun buildRunQueryRow(doc: FirestoreSpotDocument): String {
        val elem = json.encodeToJsonElement(FirestoreSpotDocument.serializer(), doc)
        val fields = elem.jsonObject.toFirestoreValue()["mapValue"]!!.jsonObject["fields"]!!
        return buildJsonObject {
            put("document", buildJsonObject {
                put("name", "projects/test/databases/(default)/documents/spots/${doc.spot.uuid}")
                put("fields", fields)
            })
        }.toString()
    }

    @Test
    fun canonicalThumbnailPathUsesSpotsPrefix() {
        assertEquals(
            "spots/user-1/spot-abc/thumbnails/img-1.jpg",
            spotThumbnailStoragePath("user-1", "spot-abc", "img-1")
        )
    }

    @Test
    fun unauthenticatedSyncPublicSpotsQueriesCanonicalSpotsAndPrunesRemoved(): Unit = runBlocking {
        val aliceSpot = FirestoreSpotDocument.fromSpotDetails(
            ownerUid = "alice",
            details = SpotDetails(
                spot = Spot(
                    latitude = 45.46,
                    longitude = 9.19,
                    createdAt = 1000L,
                    description = "Alice mural",
                    tags = listOf("mural"),
                    category = "graffiti",
                    status = "active",
                    uuid = "alice-uuid-1",
                    ownerUid = "alice",
                    isPublic = true
                ),
                images = emptyList(),
                notes = emptyList()
            )
        )
        val bobSpot = FirestoreSpotDocument.fromSpotDetails(
            ownerUid = "bob",
            details = SpotDetails(
                spot = Spot(
                    latitude = 51.50,
                    longitude = -0.12,
                    createdAt = 2000L,
                    description = "Bob stencil",
                    tags = listOf("stencil"),
                    category = "graffiti",
                    status = "active",
                    uuid = "bob-uuid-1",
                    ownerUid = "bob",
                    isPublic = true
                ),
                images = emptyList(),
                notes = emptyList()
            )
        )

        var responseBody = "[${buildRunQueryRow(aliceSpot)},${buildRunQueryRow(bobSpot)}]"
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val bytes = responseBody.toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            val repo = FakeSpotRepository()
            val client = JvmFirebaseClient(InMemorySecureStorage())
            val syncManager = JvmSyncManager(
                repository = repo,
                client = client,
                firestoreOrigin = "http://127.0.0.1:${server.address.port}"
            )

            // Signed-out sync pulls both public spots
            syncManager.syncPublicSpots()
            val afterFirst = repo.getAllSpots().first()
            assertEquals(2, afterFirst.size)
            assertTrue(afterFirst.all { it.spot.origin == SpotOrigin.PUBLIC_CACHE })

            // Author filter returns only Alice's public spot
            val aliceOnly = repo.getPublicSpotsByAuthor("alice").first()
            assertEquals(1, aliceOnly.size)
            assertEquals("alice-uuid-1", aliceOnly.first().spot.uuid)

            // Bob unpublishes -> second sync prunes Bob's cached spot
            responseBody = "[${buildRunQueryRow(aliceSpot)}]"
            syncManager.syncPublicSpots()
            val afterPrune = repo.getAllSpots().first()
            assertEquals(1, afterPrune.size)
            assertNotNull(afterPrune.find { it.spot.uuid == "alice-uuid-1" })
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun syncPublicSpotsSkipsPruningWhenAnyDocumentFailsToDecode(): Unit = runBlocking {
        val aliceSpot = FirestoreSpotDocument.fromSpotDetails(
            ownerUid = "alice",
            details = SpotDetails(
                spot = Spot(
                    latitude = 45.46,
                    longitude = 9.19,
                    createdAt = 1000L,
                    description = "Alice mural",
                    tags = emptyList(),
                    category = "graffiti",
                    status = "active",
                    uuid = "alice-uuid-1",
                    ownerUid = "alice",
                    isPublic = true
                ),
                images = emptyList(),
                notes = emptyList()
            )
        )
        val malformedRow = """{"document":{"name":"projects/test/databases/(default)/documents/spots/bad","fields":{"unexpected":{"stringValue":"broken"}}}}"""

        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val bytes = "[${buildRunQueryRow(aliceSpot)},$malformedRow]".toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            val repo = FakeSpotRepository()
            // Pre-seed a previously cached public spot from Bob
            repo.savePublicSpot(
                SpotDetails(
                    spot = Spot(
                        latitude = 51.50,
                        longitude = -0.12,
                        createdAt = 2000L,
                        description = "Bob cached",
                        tags = emptyList(),
                        category = "graffiti",
                        status = "active",
                        uuid = "bob-cached-1",
                        ownerUid = "bob",
                        isPublic = true,
                        origin = SpotOrigin.PUBLIC_CACHE
                    ),
                    images = emptyList(),
                    notes = emptyList()
                )
            )
            val client = JvmFirebaseClient(InMemorySecureStorage())
            val syncManager = JvmSyncManager(
                repository = repo,
                client = client,
                firestoreOrigin = "http://127.0.0.1:${server.address.port}"
            )

            syncManager.syncPublicSpots()
            val spots = repo.getAllSpots().first()
            // Bob's cached spot must NOT be pruned because another doc failed to decode
            assertEquals(2, spots.size)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun forceBackupSpotUploadsLocalFileAndRejectsTokenlessResponse(): Unit = runBlocking {
        val tempDir = java.nio.file.Files.createTempDirectory("jvm_backup_test").toFile()
        val localThumb = java.io.File(tempDir, "thumb.jpg").apply {
            writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x01, 0x02))
        }
        var storageResponseBody = """{"downloadTokens":"tok-123,tok-456"}"""
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val path = exchange.requestURI.path
            if (path.endsWith(":runQuery")) {
                val errBytes = "{}".toByteArray()
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(500, errBytes.size.toLong())
                exchange.responseBody.use { it.write(errBytes) }
                return@createContext
            }
            val body = if (path.startsWith("/v0/b/")) storageResponseBody else "{}"
            val bytes = body.toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        var syncManager: JvmSyncManager? = null
        try {
            val origin = "http://127.0.0.1:${server.address.port}"
            val repo = FakeSpotRepository()
            repo.activeUid = "user-1"
            repo.saveSpot(
                spot = Spot(
                    latitude = 52.52,
                    longitude = 13.405,
                    createdAt = 1000L,
                    description = "Desktop mural",
                    tags = emptyList(),
                    category = "graffiti",
                    status = "active",
                    uuid = "spot-desktop-1",
                    ownerUid = "user-1"
                ),
                imagePath = localThumb.absolutePath,
                thumbnailPath = localThumb.absolutePath
            )
            val storage = InMemorySecureStorage().apply {
                putString("firebase_id_token", "fake-token")
            }
            val client = JvmFirebaseClient(storage)
            val manager = JvmSyncManager(
                repository = repo,
                client = client,
                firestoreOrigin = origin,
                storageOrigin = origin,
                thumbnailCacheRoot = java.io.File(tempDir, "cache")
            )
            syncManager = manager
            manager.startRealtimeSync("user-1")
            val successResult = manager.forceBackupSpot("spot-desktop-1")
            assertEquals(1, successResult.uploaded)
            assertEquals(0, successResult.failed)
            val updatedUrl = repo.getAllSpots().first().first().images.first().remoteThumbnailUrl
            assertTrue(updatedUrl.contains("token=tok-123"), "Expected first download token in URL: $updatedUrl")

            // Now simulate tokenless Storage response: must fail rather than persist a tokenless URL
            storageResponseBody = """{"name":"spots/user-1/spot-desktop-1/thumbnails/x.jpg"}"""
            val failedResult = manager.forceBackupSpot("spot-desktop-1")
            assertEquals(0, failedResult.uploaded)
            assertEquals(1, failedResult.failed)
        } finally {
            syncManager?.stopRealtimeSync()
            server.stop(0)
            tempDir.deleteRecursively()
        }
    }
}
