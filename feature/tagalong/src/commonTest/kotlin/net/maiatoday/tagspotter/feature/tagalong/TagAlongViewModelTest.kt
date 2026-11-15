package net.maiatoday.tagspotter.feature.tagalong

import com.google.adk.kt.models.LlmRequest
import com.google.adk.kt.models.LlmResponse
import com.google.adk.kt.models.Model
import com.google.adk.kt.types.Content
import com.google.adk.kt.types.Part
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.maiatoday.tagspotter.core.a2ui.A2UIAction
import net.maiatoday.tagspotter.core.agent.TagAlongAgentFactory
import net.maiatoday.tagspotter.core.database.ChatMessage
import net.maiatoday.tagspotter.core.database.ChatSession
import net.maiatoday.tagspotter.core.database.FakeChatRepository
import net.maiatoday.tagspotter.feature.tagalong.fixtures.TagAlongMockFixtures
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
}
