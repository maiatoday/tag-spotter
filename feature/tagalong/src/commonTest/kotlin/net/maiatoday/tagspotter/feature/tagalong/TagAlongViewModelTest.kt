package net.maiatoday.tagspotter.feature.tagalong

import com.google.adk.kt.models.LlmRequest
import com.google.adk.kt.models.LlmResponse
import com.google.adk.kt.models.Model
import com.google.adk.kt.runners.Runner
import com.google.adk.kt.types.Content
import com.google.adk.kt.types.FunctionCall
import com.google.adk.kt.types.Part
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.maiatoday.tagspotter.core.a2ui.A2UIAction
import net.maiatoday.tagspotter.core.a2ui.A2UIEngineType
import net.maiatoday.tagspotter.core.a2ui.A2UIRendererEngine
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.maiatoday.tagspotter.core.agent.StreetArtTools
import net.maiatoday.tagspotter.core.agent.TagAlongAgentFactory
import net.maiatoday.tagspotter.core.database.ChatMessage
import net.maiatoday.tagspotter.core.database.ChatSession
import net.maiatoday.tagspotter.core.database.FakeChatRepository
import net.maiatoday.tagspotter.core.database.FakeSpotRepository
import net.maiatoday.tagspotter.core.model.SharedPack
import net.maiatoday.tagspotter.core.model.Spot
import net.maiatoday.tagspotter.core.model.SpotDetails
import net.maiatoday.tagspotter.core.photo.FakePhotoProcessor
import net.maiatoday.tagspotter.core.photo.PhotoMetadata
import net.maiatoday.tagspotter.core.sync.HybridSpotRepository
import net.maiatoday.tagspotter.core.sync.SyncManager
import net.maiatoday.tagspotter.feature.tagalong.fixtures.TagAlongMockFixtures
import net.maiatoday.tagspotter.feature.tagalong.ui.TagAlongRunnerProvider
import net.maiatoday.tagspotter.feature.tagalong.ui.TagAlongSender
import net.maiatoday.tagspotter.feature.tagalong.ui.TagAlongViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TagAlongViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeLlmModel(
        private val responseText: String,
        private val shouldThrow: Boolean = false
    ) : Model {
        override val name: String = "fake-test-model"

        override fun generateContent(request: LlmRequest, stream: Boolean): Flow<LlmResponse> = flow {
            if (shouldThrow) {
                throw RuntimeException("Simulated connection timeout")
            }
            emit(
                LlmResponse(
                    content = Content(
                        role = "model",
                        parts = listOf(Part(text = responseText))
                    )
                )
            )
        }
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialWelcomeMessageInsertedWhenSessionEmpty() = runTest(testDispatcher) {
        val chatRepo = FakeChatRepository()
        val viewModel = TagAlongViewModel(
            runner = null,
            chatRepository = chatRepo,
            initialSessionId = "empty-session"
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("empty-session", state.sessionId)
        assertEquals(1, state.messages.size)
        assertEquals(TagAlongSender.AGENT, state.messages.first().sender)
        assertTrue(state.messages.first().text!!.contains("TagAlong"))

        val dbMessages = chatRepo.getMessagesListForSession("empty-session")
        assertEquals(1, dbMessages.size)
        assertEquals("agent", dbMessages.first().sender)
    }

    @Test
    fun testLoadsExistingMessagesFromSession() = runTest(testDispatcher) {
        val chatRepo = FakeChatRepository()
        chatRepo.createOrUpdateSession(
            ChatSession(
                sessionId = "existing-session",
                title = "Historical Art Chat",
                createdAt = 1000L,
                lastActiveAt = 2000L
            )
        )
        chatRepo.insertMessage(
            ChatMessage(
                sessionId = "existing-session",
                sender = "user",
                text = "Where is the Cosmic Chameleon?",
                timestamp = 1001L
            )
        )
        chatRepo.insertMessage(
            ChatMessage(
                sessionId = "existing-session",
                sender = "agent",
                text = "It is at Teufelsberg!",
                timestamp = 1002L
            )
        )

        val viewModel = TagAlongViewModel(
            runner = null,
            chatRepository = chatRepo,
            initialSessionId = "existing-session"
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)
        assertEquals(TagAlongSender.USER, state.messages[0].sender)
        assertEquals("Where is the Cosmic Chameleon?", state.messages[0].text)
        assertEquals(TagAlongSender.AGENT, state.messages[1].sender)
        assertEquals("It is at Teufelsberg!", state.messages[1].text)
    }

    @Test
    fun testOfflineFallbackWhenRunnerIsNull() = runTest(testDispatcher) {
        val chatRepo = FakeChatRepository()
        val viewModel = TagAlongViewModel(
            runner = null,
            chatRepository = chatRepo,
            initialSessionId = "offline-session"
        )
        advanceUntilIdle()

        viewModel.sendMessage("Can you find me murals in Mitte?")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isOffline)
        assertNotNull(state.offlineMessage)

        // Initial welcome + User msg + Offline agent reply = 3 messages
        assertEquals(3, state.messages.size)
        val userMsg = state.messages[1]
        assertEquals(TagAlongSender.USER, userMsg.sender)
        assertEquals("Can you find me murals in Mitte?", userMsg.text)

        val offlineReply = state.messages[2]
        assertEquals(TagAlongSender.AGENT, offlineReply.sender)
        assertTrue(offlineReply.text!!.contains("offline mode"))

        // Check persisted messages in DB
        val dbMessages = chatRepo.getMessagesListForSession("offline-session")
        assertEquals(3, dbMessages.size)
    }

    @Test
    fun testAgentStreamingAndMapSyncWithSpotCard() = runTest(testDispatcher) {
        val chatRepo = FakeChatRepository()
        val response = """
            Here is a stunning mural in Kreuzberg:
            ```json
            ${TagAlongMockFixtures.SPOT_DOSSIER_JSON}
            ```
        """.trimIndent()

        val model = FakeLlmModel(response)
        val runner = TagAlongAgentFactory.createRunner(TagAlongAgentFactory.createAgent(model))

        val viewModel = TagAlongViewModel(
            runner = runner,
            chatRepository = chatRepo,
            initialSessionId = "spot-session"
        )
        advanceUntilIdle()

        viewModel.sendMessage("Find murals nearby")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isGenerating)
        assertFalse(state.isOffline)

        val agentMsg = state.messages.last()
        assertEquals(TagAlongSender.AGENT, agentMsg.sender)
        assertTrue(agentMsg.text!!.contains("Here is a stunning mural in Kreuzberg:"))
        assertNotNull(agentMsg.a2uiPayload)
        assertTrue(agentMsg.a2uiPayload!!.contains("Cosmic Chameleon"))

        // Verify Map Sync
        assertEquals(52.4972, state.mapCenterLat, 0.0001)
        assertEquals(13.2415, state.mapCenterLng, 0.0001)
        assertEquals("Cosmic Chameleon", state.focusedTitle)
        assertTrue(state.mapMarkers.any { it.title == "Cosmic Chameleon" })

        // Check persisted messages in DB
        val dbMessages = chatRepo.getMessagesListForSession("spot-session")
        val dbAgentMsg = dbMessages.last()
        assertEquals("agent", dbAgentMsg.sender)
        assertNotNull(dbAgentMsg.a2uiPayloadJson)
    }

    @Test
    fun testTourGenerationAndMapSyncWithWaypoints() = runTest(testDispatcher) {
        val chatRepo = FakeChatRepository()
        val response = """
            I planned a walking tour for you:
            ```json
            ${TagAlongMockFixtures.TOUR_SAMPLE_JSON}
            ```
        """.trimIndent()

        val model = FakeLlmModel(response)
        val runner = TagAlongAgentFactory.createRunner(TagAlongAgentFactory.createAgent(model))

        val viewModel = TagAlongViewModel(
            runner = runner,
            chatRepository = chatRepo,
            initialSessionId = "tour-session"
        )
        advanceUntilIdle()

        viewModel.sendMessage("Plan an alleyways tour")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isGenerating)

        // Tour card has 4 waypoints. First waypoint is at 52.5251, 13.4021
        assertEquals(52.5251, state.mapCenterLat, 0.0001)
        assertEquals(13.4021, state.mapCenterLng, 0.0001)
        assertTrue(state.focusedTitle.contains("The Cyber Samurai"))

        // Waypoints must be present in map markers
        assertTrue(state.mapMarkers.any { it.title == "The Cyber Samurai" })
        assertTrue(state.mapMarkers.any { it.title == "Neon Falcon" })
        assertTrue(state.mapMarkers.any { it.title == "Quantum Drip Wall" })
        assertTrue(state.mapMarkers.any { it.title == "Analog Dreamscape" })
    }

    @Test
    fun testSelectMockFixtureSyncsMapAndPersists() = runTest(testDispatcher) {
        val chatRepo = FakeChatRepository()
        val viewModel = TagAlongViewModel(
            runner = null,
            chatRepository = chatRepo,
            initialSessionId = "fixture-session"
        )
        advanceUntilIdle()

        viewModel.selectMockFixture("Walking Tour", TagAlongMockFixtures.TOUR_SAMPLE_JSON)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val lastMsg = state.messages.last()
        assertEquals(TagAlongSender.AGENT, lastMsg.sender)
        assertTrue(lastMsg.text!!.contains("Walking Tour"))
        assertEquals(TagAlongMockFixtures.TOUR_SAMPLE_JSON, lastMsg.a2uiPayload)

        // Verify map coordinates updated from the tour fixture
        assertEquals(52.5251, state.mapCenterLat, 0.0001)
        assertEquals(13.4021, state.mapCenterLng, 0.0001)

        val dbMessages = chatRepo.getMessagesListForSession("fixture-session")
        assertEquals(2, dbMessages.size)
        assertEquals(TagAlongMockFixtures.TOUR_SAMPLE_JSON, dbMessages.last().a2uiPayloadJson)
    }

    @Test
    fun testA2UIActionsHandledProperly() = runTest(testDispatcher) {
        val chatRepo = FakeChatRepository()
        val viewModel = TagAlongViewModel(
            runner = null,
            chatRepository = chatRepo,
            initialSessionId = "actions-session"
        )
        advanceUntilIdle()

        // 1. ViewOnMap action
        viewModel.handleAction(
            A2UIAction.ViewOnMap(
                latitude = 52.5300,
                longitude = 13.4100,
                title = "Rosenthaler Mural"
            )
        )
        assertEquals(52.5300, viewModel.uiState.value.mapCenterLat)
        assertEquals(13.4100, viewModel.uiState.value.mapCenterLng)
        assertEquals("Rosenthaler Mural", viewModel.uiState.value.focusedTitle)
        assertTrue(viewModel.uiState.value.mapMarkers.any { it.title == "Rosenthaler Mural" })

        // 2. NavigateToStop action
        viewModel.handleAction(
            A2UIAction.NavigateToStop(
                stopIndex = 3,
                latitude = 52.5350,
                longitude = 13.4150,
                title = "Urban Spree"
            )
        )
        assertEquals(52.5350, viewModel.uiState.value.mapCenterLat)
        assertEquals("Stop #3: Urban Spree", viewModel.uiState.value.focusedTitle)

        // 3. StarSpot action
        val initialStarred = viewModel.uiState.value.mapMarkers.first { it.title.contains("Cosmic Chameleon") }.isStarred
        viewModel.handleAction(A2UIAction.StarSpot("Cosmic Chameleon"))
        val toggledStarred = viewModel.uiState.value.mapMarkers.first { it.title.contains("Cosmic Chameleon") }.isStarred
        assertEquals(!initialStarred, toggledStarred)

        // 4. AnswerQuiz action
        val countBeforeQuiz = viewModel.uiState.value.messages.size
        viewModel.handleAction(
            A2UIAction.AnswerQuiz(
                quizId = "quiz-1",
                optionId = "opt-1",
                isCorrect = true
            )
        )
        advanceUntilIdle()
        assertEquals(countBeforeQuiz + 1, viewModel.uiState.value.messages.size)
        assertTrue(viewModel.uiState.value.messages.last().text!!.contains("Spot on!"))

        // 5. VoteOption action
        viewModel.handleAction(
            A2UIAction.VoteOption(
                pollId = "poll-1",
                optionId = "preserve"
            )
        )
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.messages.last().text!!.contains("Vote recorded"))
    }

    @Test
    fun testRunnerExceptionSetsOfflineFallback() = runTest(testDispatcher) {
        val chatRepo = FakeChatRepository()
        val failingModel = FakeLlmModel(responseText = "", shouldThrow = true)
        val runner = TagAlongAgentFactory.createRunner(TagAlongAgentFactory.createAgent(failingModel))

        val viewModel = TagAlongViewModel(
            runner = runner,
            chatRepository = chatRepo,
            initialSessionId = "failing-session"
        )
        advanceUntilIdle()

        viewModel.sendMessage("Will this fail?")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isGenerating)
        assertTrue(state.isOffline)
        assertNotNull(state.offlineMessage)
        assertTrue(state.offlineMessage!!.contains("Simulated connection timeout"))

        val lastMsg = state.messages.last()
        assertTrue(lastMsg.text!!.contains("Unable to reach TagAlong agent"))

        val dbMessages = chatRepo.getMessagesListForSession("failing-session")
        assertTrue(dbMessages.last().text.contains("Unable to reach TagAlong agent"))
    }

    @Test
    fun testStartNewConversation() = runTest(testDispatcher) {
        val chatRepo = FakeChatRepository()
        var clearSurfacesCalled = false
        val fakeEngine = object : A2UIRendererEngine {
            override val engineType: A2UIEngineType = A2UIEngineType.A2UI_KMP_4K
            override val outboundEvents: Flow<A2UIAction> = flowOf()
            override fun getCatalogSchemaJson(): String = "{}"
            override fun processProtocolMessage(json: String): List<String> = emptyList()
            override fun updateDataModel(surfaceId: String, path: String, value: Any?) {}
            override fun clearSurfaces() {
                clearSurfacesCalled = true
            }
            @Composable
            override fun RenderSurface(surfaceId: String, modifier: Modifier) {}
            @Composable
            override fun RenderSurface(
                payloadJson: String,
                onAction: (A2UIAction) -> Unit,
                modifier: Modifier
            ) {}
        }
        val viewModel = TagAlongViewModel(
            runner = null,
            chatRepository = chatRepo,
            initialSessionId = "session-1"
        )
        advanceUntilIdle()
        assertEquals("session-1", viewModel.activeSessionId)

        viewModel.startNewConversation(fakeEngine)
        advanceUntilIdle()

        assertTrue(clearSurfacesCalled)
        val newSessionId = viewModel.activeSessionId
        assertTrue(newSessionId != "session-1")
        assertTrue(newSessionId.startsWith("session-"))

        val sessionInRepo = chatRepo.getSessionById(newSessionId)
        assertNotNull(sessionInRepo)

        val messagesInRepo = chatRepo.getMessagesListForSession(newSessionId)
        assertEquals(1, messagesInRepo.size)
        assertTrue(messagesInRepo.first().text.contains("TagAlong"))

        val uiState = viewModel.uiState.value
        assertEquals(newSessionId, uiState.sessionId)
        assertEquals(1, uiState.messages.size)
    }

    @Test
    fun testAttachEngineAndSelectMockFixturePopulatesSurfaceIds() = runTest(testDispatcher) {
        val chatRepo = FakeChatRepository()
        var runRenderLoopCalled = false
        val fakeEngine = object : A2UIRendererEngine {
            override val engineType: A2UIEngineType = A2UIEngineType.A2UI_KMP_4K
            override val outboundEvents: Flow<A2UIAction> = flowOf()
            override fun getCatalogSchemaJson(): String = "{}"
            override suspend fun runRenderLoop() {
                runRenderLoopCalled = true
            }
            override fun processProtocolMessage(json: String): List<String> = listOf("surface-fixture-1")
            override fun updateDataModel(surfaceId: String, path: String, value: Any?) {}
            override fun clearSurfaces() {}
            @Composable
            override fun RenderSurface(surfaceId: String, modifier: Modifier) {}
            @Composable
            override fun RenderSurface(
                payloadJson: String,
                onAction: (A2UIAction) -> Unit,
                modifier: Modifier
            ) {}
        }

        val viewModel = TagAlongViewModel(
            runner = null,
            chatRepository = chatRepo,
            initialSessionId = "fixture-session"
        )
        advanceUntilIdle()

        viewModel.attachEngine(fakeEngine)
        advanceUntilIdle()
        assertTrue(runRenderLoopCalled)

        viewModel.selectMockFixture("SpotCard", TagAlongMockFixtures.SPOT_DOSSIER_JSON)
        advanceUntilIdle()

        val messages = viewModel.uiState.value.messages
        val fixtureMessage = messages.last()
        assertEquals(listOf("surface-fixture-1"), fixtureMessage.surfaceIds)
    }

    private class FakeTestSyncManager(
        private val publicSpots: List<SpotDetails> = emptyList()
    ) : SyncManager {
        override val isSyncing: StateFlow<Boolean> = MutableStateFlow(false)
        override suspend fun syncNow() {}
        override fun startRealtimeSync(userId: String) {}
        override fun stopRealtimeSync() {}
        override suspend fun deleteSpot(uuid: String) {}
        override suspend fun sharePack(title: String, description: String, authorName: String, spots: List<SpotDetails>): String = ""
        override suspend fun importPackByCode(code: String): SharedPack = SharedPack("", "", "", "", emptyList())
        override suspend fun saveImportedPack(sharedPack: SharedPack) {}
        override suspend fun fetchPublicSpots(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): List<SpotDetails> = publicSpots
    }

    @Test
    fun testSaveSpotClientEventSavesToRepositoryAndUpdatesDataModel() = runTest(testDispatcher) {
        val chatRepo = FakeChatRepository()
        val localSpotRepo = FakeSpotRepository()
        val syncManager = FakeTestSyncManager()
        val hybridRepo = HybridSpotRepository(localSpotRepo, syncManager)

        val updatedPaths = mutableListOf<Pair<String, Any?>>()
        val fakeEngine = object : A2UIRendererEngine {
            override val engineType: A2UIEngineType = A2UIEngineType.A2UI_KMP_4K
            override val outboundEvents: Flow<A2UIAction> = flowOf()
            override fun getCatalogSchemaJson(): String = "{}"
            override fun processProtocolMessage(json: String): List<String> = emptyList()
            override fun updateDataModel(surfaceId: String, path: String, value: Any?) {
                updatedPaths.add(path to value)
            }
            override fun clearSurfaces() {}
            @Composable
            override fun RenderSurface(surfaceId: String, modifier: Modifier) {}
            @Composable
            override fun RenderSurface(
                payloadJson: String,
                onAction: (A2UIAction) -> Unit,
                modifier: Modifier
            ) {}
        }

        val viewModel = TagAlongViewModel(
            runner = null,
            chatRepository = chatRepo,
            hybridSpotRepository = hybridRepo,
            initialSessionId = "save-spot-session"
        )
        advanceUntilIdle()

        viewModel.attachEngine(fakeEngine)
        advanceUntilIdle()

        val saveAction = A2UIAction.ClientEvent(
            surfaceId = "surface-spot-1",
            name = "save_spot",
            context = mapOf(
                "spotId" to "spot-uuid-123",
                "title" to "Awesome Mural",
                "artist" to "Banksy",
                "category" to "mural",
                "latitude" to "52.52",
                "longitude" to "13.40",
                "imageUrl" to "https://example.com/mural.jpg"
            )
        )

        viewModel.handleAction(saveAction)
        advanceUntilIdle()

        val savedSpots = localSpotRepo.getAllSpots().first()
        assertEquals(1, savedSpots.size)
        val saved = savedSpots.first()
        assertEquals("spot-uuid-123", saved.spot.uuid)
        assertEquals("Awesome Mural", saved.spot.description)
        assertEquals(listOf("Banksy"), saved.spot.artists)
        assertEquals("graffiti", saved.spot.category) // mapped from mural to graffiti
        assertEquals(52.52, saved.spot.latitude)
        assertEquals(13.40, saved.spot.longitude)

        assertTrue(updatedPaths.contains("/spot/isSaved" to true))
    }

    @Test
    fun testToggleStarClientEventTogglesInRepositoryAndUpdatesDataModel() = runTest(testDispatcher) {
        val chatRepo = FakeChatRepository()
        val localSpotRepo = FakeSpotRepository()
        val syncManager = FakeTestSyncManager()
        val hybridRepo = HybridSpotRepository(localSpotRepo, syncManager)

        localSpotRepo.saveSpot(
            spot = Spot(
                id = 1L,
                uuid = "star-uuid-456",
                description = "Starry Spot",
                category = "graffiti",
                latitude = 10.0,
                longitude = 20.0,
                createdAt = 1000L,
                tags = listOf("graffiti"),
                status = "active",
                isStarred = false
            ),
            imagePath = "",
            thumbnailPath = ""
        )

        val updatedPaths = mutableListOf<Pair<String, Any?>>()
        val fakeEngine = object : A2UIRendererEngine {
            override val engineType: A2UIEngineType = A2UIEngineType.A2UI_KMP_4K
            override val outboundEvents: Flow<A2UIAction> = flowOf()
            override fun getCatalogSchemaJson(): String = "{}"
            override fun processProtocolMessage(json: String): List<String> = emptyList()
            override fun updateDataModel(surfaceId: String, path: String, value: Any?) {
                updatedPaths.add(path to value)
            }
            override fun clearSurfaces() {}
            @Composable
            override fun RenderSurface(surfaceId: String, modifier: Modifier) {}
            @Composable
            override fun RenderSurface(
                payloadJson: String,
                onAction: (A2UIAction) -> Unit,
                modifier: Modifier
            ) {}
        }

        val viewModel = TagAlongViewModel(
            runner = null,
            chatRepository = chatRepo,
            hybridSpotRepository = hybridRepo,
            initialSessionId = "toggle-star-session"
        )
        advanceUntilIdle()

        viewModel.attachEngine(fakeEngine)
        advanceUntilIdle()

        val toggleAction = A2UIAction.ClientEvent(
            surfaceId = "surface-star-1",
            name = "toggle_star",
            context = mapOf("spotId" to "star-uuid-456")
        )

        viewModel.handleAction(toggleAction)
        advanceUntilIdle()

        val spotAfter = localSpotRepo.getAllSpots().first().find { it.spot.uuid == "star-uuid-456" }
        assertNotNull(spotAfter)
        assertTrue(spotAfter.spot.isStarred)
        assertTrue(updatedPaths.contains("/spot/isStarred" to true))

        viewModel.handleAction(toggleAction)
        advanceUntilIdle()

        val spotAfterSecond = localSpotRepo.getAllSpots().first().find { it.spot.uuid == "star-uuid-456" }
        assertNotNull(spotAfterSecond)
        assertFalse(spotAfterSecond.spot.isStarred)
        assertTrue(updatedPaths.contains("/spot/isStarred" to false))
    }

    @Test
    fun testOnPhotoAttachedExtractsMetadataAndMovesMapCenter() = runTest(testDispatcher) {
        val chatRepo = FakeChatRepository()
        val fakePhotoProcessor = FakePhotoProcessor().apply {
            metadataResult = PhotoMetadata(latitude = 48.8566, longitude = 2.3522, timestamp = 1700000000L)
            createThumbUriResult = "thumb_paris_path"
        }

        val viewModel = TagAlongViewModel(
            runner = null,
            chatRepository = chatRepo,
            photoProcessor = fakePhotoProcessor,
            initialSessionId = "photo-session"
        )
        advanceUntilIdle()

        viewModel.onPhotoAttached("content://media/external/images/media/42")
        advanceUntilIdle()

        assertEquals("content://media/external/images/media/42", fakePhotoProcessor.extractMetadataFromUriCalledWith)
        assertEquals("content://media/external/images/media/42", fakePhotoProcessor.createThumbnailFromUriCalledWith)

        val state = viewModel.uiState.value
        assertEquals("content://media/external/images/media/42", state.attachedPhotoUri)
        assertEquals(48.8566, state.mapCenterLat)
        assertEquals(2.3522, state.mapCenterLng)
    }

    @Test
    fun testAllSessionsFlowEmission() = runTest(testDispatcher) {
        val chatRepo = FakeChatRepository()
        chatRepo.createOrUpdateSession(
            ChatSession(
                sessionId = "session-1",
                title = "Kreuzberg Murals",
                createdAt = 1000L,
                lastActiveAt = 2000L
            )
        )
        chatRepo.createOrUpdateSession(
            ChatSession(
                sessionId = "session-2",
                title = "Mitte Tour",
                createdAt = 1500L,
                lastActiveAt = 3000L
            )
        )

        val viewModel = TagAlongViewModel(
            runner = null,
            chatRepository = chatRepo,
            initialSessionId = "session-1"
        )
        advanceUntilIdle()

        val initialSessions = viewModel.allSessions.value
        assertEquals(2, initialSessions.size)
        assertTrue(initialSessions.any { it.sessionId == "session-1" })
        assertTrue(initialSessions.any { it.sessionId == "session-2" })

        // Add a 3rd session
        chatRepo.createOrUpdateSession(
            ChatSession(
                sessionId = "session-3",
                title = "Friedrichshain Tags",
                createdAt = 2000L,
                lastActiveAt = 4000L
            )
        )
        advanceUntilIdle()

        val updatedSessions = viewModel.allSessions.value
        assertEquals(3, updatedSessions.size)
        assertTrue(updatedSessions.any { it.sessionId == "session-3" })
    }

    @Test
    fun testSwitchSessionClearsSurfacesAndReplaysA2UIPayload() = runTest(testDispatcher) {
        val chatRepo = FakeChatRepository()
        chatRepo.createOrUpdateSession(
            ChatSession(
                sessionId = "session-A",
                title = "Session A",
                createdAt = 1000L
            )
        )
        chatRepo.createOrUpdateSession(
            ChatSession(
                sessionId = "session-B",
                title = "Session B",
                createdAt = 2000L
            )
        )

        val payloadJson = TagAlongMockFixtures.SPOT_DOSSIER_JSON
        chatRepo.insertMessage(
            ChatMessage(
                sessionId = "session-B",
                sender = "user",
                text = "Show me the mural",
                timestamp = 2001L
            )
        )
        chatRepo.insertMessage(
            ChatMessage(
                sessionId = "session-B",
                sender = "agent",
                text = "Here is the spot card",
                a2uiPayloadJson = payloadJson,
                timestamp = 2002L
            )
        )

        var clearSurfacesCount = 0
        val processedPayloads = mutableListOf<String>()

        val fakeEngine = object : A2UIRendererEngine {
            override val engineType: A2UIEngineType = A2UIEngineType.A2UI_KMP_4K
            override val outboundEvents: Flow<A2UIAction> = flowOf()
            override fun getCatalogSchemaJson(): String = "{}"
            override fun processProtocolMessage(json: String): List<String> {
                processedPayloads.add(json)
                return listOf("surface-reconstructed-1")
            }
            override fun updateDataModel(surfaceId: String, path: String, value: Any?) {}
            override fun clearSurfaces() {
                clearSurfacesCount++
            }
            @Composable
            override fun RenderSurface(surfaceId: String, modifier: Modifier) {}
            @Composable
            override fun RenderSurface(
                payloadJson: String,
                onAction: (A2UIAction) -> Unit,
                modifier: Modifier
            ) {}
        }

        val viewModel = TagAlongViewModel(
            runner = null,
            chatRepository = chatRepo,
            initialSessionId = "session-A"
        )
        advanceUntilIdle()

        viewModel.attachEngine(fakeEngine)
        advanceUntilIdle()

        val clearCountBefore = clearSurfacesCount

        viewModel.switchSession("session-B")
        advanceUntilIdle()

        assertTrue(clearSurfacesCount > clearCountBefore, "Expected clearSurfaces to be called on switchSession")
        assertEquals(1, processedPayloads.size)
        assertEquals(payloadJson, processedPayloads.first())

        val state = viewModel.uiState.value
        assertEquals("session-B", state.sessionId)
        assertEquals(2, state.messages.size)

        val agentMsg = state.messages[1]
        assertEquals(TagAlongSender.AGENT, agentMsg.sender)
        assertEquals("Here is the spot card", agentMsg.text)
        assertEquals(payloadJson, agentMsg.a2uiPayload)
        assertEquals(listOf("surface-reconstructed-1"), agentMsg.surfaceIds)
    }

    @Test
    fun testDeleteActiveSessionSwitchesToNextSessionWhenSessionsRemain() = runTest(testDispatcher) {
        val chatRepo = FakeChatRepository()
        chatRepo.createOrUpdateSession(
            ChatSession(
                sessionId = "session-active",
                title = "Active Session",
                createdAt = 1000L,
                lastActiveAt = 1000L
            )
        )
        chatRepo.createOrUpdateSession(
            ChatSession(
                sessionId = "session-next",
                title = "Next Session",
                createdAt = 2000L,
                lastActiveAt = 2000L
            )
        )
        chatRepo.insertMessage(
            ChatMessage(
                sessionId = "session-next",
                sender = "agent",
                text = "Hello from session next",
                timestamp = 2001L
            )
        )

        val viewModel = TagAlongViewModel(
            runner = null,
            chatRepository = chatRepo,
            initialSessionId = "session-active"
        )
        advanceUntilIdle()

        assertEquals("session-active", viewModel.activeSessionId)

        viewModel.deleteSession("session-active")
        advanceUntilIdle()

        // Check chatRepo has deleted session-active
        val deletedSession = chatRepo.getSessionById("session-active")
        assertEquals(null, deletedSession)

        // Check activeSessionId switched to session-next
        assertEquals("session-next", viewModel.activeSessionId)
        val state = viewModel.uiState.value
        assertEquals("session-next", state.sessionId)
        assertEquals(1, state.messages.size)
        assertEquals("Hello from session next", state.messages.first().text)
    }

    @Test
    fun testDeleteActiveSessionStartsNewConversationWhenNoSessionsRemain() = runTest(testDispatcher) {
        val chatRepo = FakeChatRepository()
        chatRepo.createOrUpdateSession(
            ChatSession(
                sessionId = "only-session",
                title = "Only Session",
                createdAt = 1000L,
                lastActiveAt = 1000L
            )
        )

        val viewModel = TagAlongViewModel(
            runner = null,
            chatRepository = chatRepo,
            initialSessionId = "only-session"
        )
        advanceUntilIdle()

        assertEquals("only-session", viewModel.activeSessionId)

        viewModel.deleteSession("only-session")
        advanceUntilIdle()

        // Verify old session deleted
        assertEquals(null, chatRepo.getSessionById("only-session"))

        // Verify switched to brand new session
        val state = viewModel.uiState.value
        assertTrue(state.sessionId != "only-session")
        assertTrue(state.sessionId.startsWith("session-"))
        assertEquals(1, state.messages.size)
        assertEquals(TagAlongSender.AGENT, state.messages.first().sender)
        assertTrue(state.messages.first().text!!.contains("TagAlong"))
    }

    @Test
    fun testDeleteNonActiveSessionLeavesActiveSessionUntouched() = runTest(testDispatcher) {
        val chatRepo = FakeChatRepository()
        chatRepo.createOrUpdateSession(
            ChatSession(
                sessionId = "active-session",
                title = "Active Session",
                createdAt = 1000L,
                lastActiveAt = 2000L
            )
        )
        chatRepo.createOrUpdateSession(
            ChatSession(
                sessionId = "other-session",
                title = "Other Session",
                createdAt = 500L,
                lastActiveAt = 1000L
            )
        )

        val viewModel = TagAlongViewModel(
            runner = null,
            chatRepository = chatRepo,
            initialSessionId = "active-session"
        )
        advanceUntilIdle()

        assertEquals("active-session", viewModel.activeSessionId)

        viewModel.deleteSession("other-session")
        advanceUntilIdle()

        assertEquals(null, chatRepo.getSessionById("other-session"))
        assertEquals("active-session", viewModel.activeSessionId)
        assertEquals("active-session", viewModel.uiState.value.sessionId)
    }

    @Test
    fun testStreamingPartialsAndFinalAggregatedEventDoNotDuplicateProseOrSurface() = runTest(testDispatcher) {
        val chatRepo = FakeChatRepository()
        val fullResponse = """
            Check out Cosmic Chameleon!
            ```a2ui
            ${TagAlongMockFixtures.SPOT_DOSSIER_JSON}
            ```
        """.trimIndent()
        val splitIndex = fullResponse.length / 2
        val part1 = fullResponse.substring(0, splitIndex)
        val part2 = fullResponse.substring(splitIndex)

        val streamingModel = object : Model {
            override val name: String = "fake-streaming-model"
            override fun generateContent(request: LlmRequest, stream: Boolean): Flow<LlmResponse> = flow {
                emit(
                    LlmResponse(
                        content = Content(role = "model", parts = listOf(Part(text = part1))),
                        partial = true
                    )
                )
                emit(
                    LlmResponse(
                        content = Content(role = "model", parts = listOf(Part(text = part2))),
                        partial = true
                    )
                )
                // Final aggregated non-partial event repeating the full response
                emit(
                    LlmResponse(
                        content = Content(role = "model", parts = listOf(Part(text = fullResponse))),
                        partial = false
                    )
                )
            }
        }

        var processPayloadCalls = 0
        val fakeEngine = object : A2UIRendererEngine {
            override val engineType: A2UIEngineType = A2UIEngineType.A2UI_KMP_4K
            override val outboundEvents: Flow<A2UIAction> = flowOf()
            override fun getCatalogSchemaJson(): String = "{}"
            override fun processProtocolMessage(json: String): List<String> {
                processPayloadCalls++
                return listOf("spot-surface-1")
            }
            override fun updateDataModel(surfaceId: String, path: String, value: Any?) {}
            override fun clearSurfaces() {}
            @Composable
            override fun RenderSurface(surfaceId: String, modifier: Modifier) {}
            @Composable
            override fun RenderSurface(payloadJson: String, onAction: (A2UIAction) -> Unit, modifier: Modifier) {}
        }

        val runner = TagAlongAgentFactory.createRunner(TagAlongAgentFactory.createAgent(streamingModel))
        val viewModel = TagAlongViewModel(
            runner = runner,
            chatRepository = chatRepo,
            initialSessionId = "streaming-session"
        )
        advanceUntilIdle()
        viewModel.attachEngine(fakeEngine)
        advanceUntilIdle()

        viewModel.sendMessage("Show me Teufelsberg")
        advanceUntilIdle()

        val agentMsg = viewModel.uiState.value.messages.last()
        assertEquals("Check out Cosmic Chameleon!", agentMsg.text)
        assertEquals(listOf("spot-surface-1"), agentMsg.surfaceIds)
        assertEquals(1, processPayloadCalls, "Identical A2UI surface should only be processed once")
    }

    @Test
    fun testIntermediateToolCallAndResponseEventsAreFiltered() = runTest(testDispatcher) {
        val chatRepo = FakeChatRepository()
        val localSpotRepo = FakeSpotRepository()
        val hybridRepo = HybridSpotRepository(localSpotRepo, FakeTestSyncManager())
        val tools = StreetArtTools(hybridRepo)

        var callCount = 0
        val toolCallingModel = object : Model {
            override val name: String = "fake-tool-model"
            override fun generateContent(request: LlmRequest, stream: Boolean): Flow<LlmResponse> = flow {
                callCount++
                if (callCount == 1) {
                    emit(
                        LlmResponse(
                            content = Content(
                                role = "model",
                                parts = listOf(
                                    Part(text = "INTERNAL_TOOL_PREAMBLE"),
                                    Part(
                                        functionCall = FunctionCall(
                                            id = "call-1",
                                            name = "getNearbySpots",
                                            args = mapOf("latitude" to 52.5, "longitude" to 13.4)
                                        )
                                    )
                                )
                            ),
                            partial = false
                        )
                    )
                } else {
                    emit(
                        LlmResponse(
                            content = Content(
                                role = "model",
                                parts = listOf(Part(text = "Here are the nearby murals I found!"))
                            ),
                            partial = false
                        )
                    )
                }
            }
        }

        val runner = TagAlongAgentFactory.createRunner(
            model = toolCallingModel,
            streetArtTools = tools
        )
        val viewModel = TagAlongViewModel(
            runner = runner,
            chatRepository = chatRepo,
            initialSessionId = "tool-filter-session"
        )
        advanceUntilIdle()

        viewModel.sendMessage("Any murals near 52.5, 13.4?")
        advanceUntilIdle()

        val agentMsg = viewModel.uiState.value.messages.last()
        assertEquals("Here are the nearby murals I found!", agentMsg.text)
        assertFalse(agentMsg.text!!.contains("INTERNAL_TOOL_PREAMBLE"))
        assertEquals(2, callCount, "Expected model to be invoked twice (tool call + final response)")
    }

    @Test
    fun testDynamicRunnerProviderTransitionsFromOfflineToLive() = runTest(testDispatcher) {
        val chatRepo = FakeChatRepository()
        var currentRunner: Runner? = null

        val viewModel = TagAlongViewModel(
            runner = null,
            chatRepository = chatRepo,
            initialSessionId = "dynamic-runner-session",
            runnerProvider = TagAlongRunnerProvider { currentRunner }
        )
        advanceUntilIdle()

        // First send: no API key / runner yet -> offline fallback
        viewModel.sendMessage("Hello offline")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isOffline)
        assertTrue(viewModel.uiState.value.messages.last().text!!.contains("offline mode"))

        // Simulate user entering Gemini API key in Settings -> runnerProvider now returns live runner
        currentRunner = TagAlongAgentFactory.createRunner(
            TagAlongAgentFactory.createAgent(FakeLlmModel("Welcome back online!"))
        )

        viewModel.sendMessage("Hello online")
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isOffline)
        assertEquals("Welcome back online!", viewModel.uiState.value.messages.last().text)
    }
}
