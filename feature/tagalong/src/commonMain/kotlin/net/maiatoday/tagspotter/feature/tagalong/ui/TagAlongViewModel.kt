package net.maiatoday.tagspotter.feature.tagalong.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.adk.kt.events.Event
import com.google.adk.kt.runners.Runner
import com.google.adk.kt.sessions.SessionKey
import com.google.adk.kt.types.Blob
import com.google.adk.kt.types.Content
import com.google.adk.kt.types.Part
import net.maiatoday.tagspotter.core.agent.TagAlongAgentFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.maiatoday.tagspotter.core.a2ui.A2UIAction
import net.maiatoday.tagspotter.core.a2ui.A2UIRendererEngine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.maiatoday.tagspotter.core.database.ChatMessage
import net.maiatoday.tagspotter.core.database.ChatRepository
import net.maiatoday.tagspotter.core.database.ChatSession
import net.maiatoday.tagspotter.core.database.epochMillis
import net.maiatoday.tagspotter.core.location.LocationProvider
import net.maiatoday.tagspotter.core.model.Spot
import net.maiatoday.tagspotter.core.model.generateUuid
import net.maiatoday.tagspotter.core.photo.PhotoProcessor
import net.maiatoday.tagspotter.core.sync.HybridSpotRepository
import net.maiatoday.tagspotter.core.ui.MapMarker

enum class TagAlongSender {
    USER,
    AGENT
}

data class TagAlongMessage(
    val id: String,
    val sender: TagAlongSender,
    val text: String? = null,
    val a2uiPayload: String? = null,
    val surfaceIds: List<String> = emptyList(),
    val photoUri: String? = null,
    val timestamp: Long = 0L
)

data class TagAlongUiState(
    val sessionId: String = "session-default",
    val messages: List<TagAlongMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val isOffline: Boolean = false,
    val offlineMessage: String? = null,
    val mapCenterLat: Double = 52.4972,
    val mapCenterLng: Double = 13.2415,
    val mapZoom: Double = 14.0,
    val focusedTitle: String = "Teufelsberg Street Art",
    val mapMarkers: List<MapMarker> = emptyList(),
    val inputText: String = "",
    val attachedPhotoUri: String? = null
)

sealed interface TagAlongNavEvent {
    data class OpenSpotDetail(val spotId: Long) : TagAlongNavEvent
    data class ViewOnMainMap(val latitude: Double, val longitude: Double) : TagAlongNavEvent
}

fun interface TagAlongRunnerProvider {
    fun getRunner(): Runner?
}

class TagAlongViewModel(
    private val runner: Runner? = null,
    private val chatRepository: ChatRepository,
    private val locationProvider: LocationProvider? = null,
    private val hybridSpotRepository: HybridSpotRepository? = null,
    private val photoProcessor: PhotoProcessor? = null,
    private val initialSessionId: String = "session-default",
    private val runnerProvider: TagAlongRunnerProvider? = null
) : ViewModel() {

    private fun resolveRunner(): Runner? = runnerProvider?.getRunner() ?: runner

    private val _navigationEvents = MutableSharedFlow<TagAlongNavEvent>(extraBufferCapacity = 8)
    val navigationEvents: SharedFlow<TagAlongNavEvent> = _navigationEvents.asSharedFlow()

    private val _uiState = MutableStateFlow(
        TagAlongUiState(
            sessionId = initialSessionId,
            mapMarkers = defaultBerlinMarkers()
        )
    )
    val uiState: StateFlow<TagAlongUiState> = _uiState.asStateFlow()

    val allSessions: StateFlow<List<ChatSession>> = chatRepository.getAllSessions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    var engine: A2UIRendererEngine? = null
        private set

    private var renderLoopJob: Job? = null
    private var outboundEventsJob: Job? = null

    val activeSessionId: String
        get() = _uiState.value.sessionId

    fun attachEngine(rendererEngine: A2UIRendererEngine) {
        if (engine === rendererEngine && renderLoopJob?.isActive == true) return
        engine = rendererEngine

        renderLoopJob?.cancel()
        renderLoopJob = viewModelScope.launch {
            rendererEngine.runRenderLoop()
        }

        outboundEventsJob?.cancel()
        outboundEventsJob = viewModelScope.launch {
            rendererEngine.outboundEvents.collect { action ->
                handleAction(action)
            }
        }

        reapplyA2UIPayloads(rendererEngine)
    }

    fun switchEngine(newEngine: A2UIRendererEngine) {
        engine?.clearSurfaces()
        attachEngine(newEngine)
    }

    private fun reapplyA2UIPayloads(rendererEngine: A2UIRendererEngine) {
        _uiState.update { state ->
            val updated = state.messages.map { msg ->
                if (!msg.a2uiPayload.isNullOrBlank()) {
                    val ids = try {
                        rendererEngine.processProtocolMessage(msg.a2uiPayload)
                    } catch (_: Exception) {
                        emptyList()
                    }
                    msg.copy(surfaceIds = ids)
                } else msg
            }
            state.copy(messages = updated)
        }
    }

    fun startNewConversation(rendererEngine: A2UIRendererEngine? = null) {
        val targetEngine = rendererEngine ?: engine
        targetEngine?.clearSurfaces()
        viewModelScope.launch {
            val now = epochMillis()
            val newSessionId = "session-$now"
            chatRepository.createOrUpdateSession(
                ChatSession(
                    sessionId = newSessionId,
                    title = "Street Art Exploration",
                    createdAt = now,
                    lastActiveAt = now
                )
            )
            val welcomeText = DEFAULT_WELCOME_TEXT
            val initialMsg = TagAlongMessage(
                id = "init-welcome-$now",
                sender = TagAlongSender.AGENT,
                text = welcomeText,
                timestamp = now
            )
            chatRepository.insertMessage(
                ChatMessage(
                    sessionId = newSessionId,
                    sender = "agent",
                    text = welcomeText,
                    timestamp = initialMsg.timestamp
                )
            )
            _uiState.update {
                it.copy(
                    sessionId = newSessionId,
                    messages = listOf(initialMsg),
                    inputText = "",
                    attachedPhotoUri = null,
                    isGenerating = false
                )
            }
        }
    }

    fun switchSession(targetSessionId: String, rendererEngine: A2UIRendererEngine? = null) {
        val targetEngine = rendererEngine ?: engine
        if (rendererEngine != null && engine !== rendererEngine) {
            attachEngine(rendererEngine)
        }
        targetEngine?.clearSurfaces()
        viewModelScope.launch {
            val dbMessages = chatRepository.getMessagesListForSession(targetSessionId)
            val uiMessages = dbMessages.map { msg ->
                val uiMsg = msg.toUiMessage(targetEngine)
                val payload = msg.a2uiPayloadJson
                if (!payload.isNullOrBlank()) {
                    syncMapWithA2UI(payload)
                }
                uiMsg
            }
            _uiState.update {
                it.copy(
                    sessionId = targetSessionId,
                    messages = uiMessages,
                    inputText = "",
                    attachedPhotoUri = null,
                    isGenerating = false
                )
            }
            resolveRunner()?.let { activeRunner ->
                ensureAndSeedAdkSession(activeRunner, targetSessionId, dbMessages)
            }
        }
    }

    fun deleteSession(sessionId: String, rendererEngine: A2UIRendererEngine? = null) {
        val targetEngine = rendererEngine ?: engine
        viewModelScope.launch {
            chatRepository.deleteSession(sessionId)
            chatRepository.deleteMessagesForSession(sessionId)
            resolveRunner()?.let { activeRunner ->
                runCatching {
                    activeRunner.sessionService.deleteSession(
                        SessionKey(
                            appName = activeRunner.appName,
                            userId = ADK_USER_ID,
                            id = sessionId
                        )
                    )
                }
            }

            if (sessionId == activeSessionId) {
                val remainingSessions = chatRepository.getAllSessions().first().filter { it.sessionId != sessionId }
                if (remainingSessions.isNotEmpty()) {
                    val nextSession = remainingSessions.first()
                    switchSession(nextSession.sessionId, targetEngine)
                } else {
                    startNewConversation(targetEngine)
                }
            }
        }
    }

    private suspend fun ensureAndSeedAdkSession(
        activeRunner: Runner,
        sessionId: String,
        persistedMessages: List<ChatMessage>
    ) {
        runCatching {
            val sessionService = activeRunner.sessionService
            val sessionKey = SessionKey(
                appName = activeRunner.appName,
                userId = ADK_USER_ID,
                id = sessionId
            )
            val session = sessionService.getSession(sessionKey)
                ?: sessionService.createSession(sessionKey)

            if (session.events.isNotEmpty()) return@runCatching

            val agentAuthor = activeRunner.agent.name.ifBlank { TagAlongAgentFactory.AGENT_NAME }
            persistedMessages.forEachIndexed { index, msg ->
                val isUser = msg.sender == "user"
                val trimmedText = msg.text.trim()
                if (!isUser && trimmedText == DEFAULT_WELCOME_TEXT && msg.a2uiPayloadJson.isNullOrBlank()) {
                    return@forEachIndexed
                }
                val contentText = trimmedText.ifBlank { msg.a2uiPayloadJson?.trim().orEmpty() }
                if (contentText.isBlank()) {
                    return@forEachIndexed
                }

                val event = Event(
                    invocationId = "seed-$sessionId-$index",
                    author = if (isUser) "user" else agentAuthor,
                    content = Content(
                        role = if (isUser) "user" else "model",
                        parts = listOf(Part(text = contentText))
                    ),
                    timestamp = if (msg.timestamp > 0L) msg.timestamp else epochMillis()
                )
                sessionService.appendEvent(session, event)
            }
        }
    }

    init {
        viewModelScope.launch {
            loadSessionMessages()
            initMapFromSources()
        }
    }

    private suspend fun loadSessionMessages() {
        if (chatRepository.getSessionById(initialSessionId) == null) {
            chatRepository.createOrUpdateSession(
                ChatSession(
                    sessionId = initialSessionId,
                    title = "Street Art Exploration",
                    createdAt = epochMillis(),
                    lastActiveAt = epochMillis()
                )
            )
        }
        val existing = chatRepository.getMessagesListForSession(initialSessionId)
        if (existing.isNotEmpty()) {
            _uiState.update { it.copy(messages = existing.map { m -> m.toUiMessage() }) }
            resolveRunner()?.let { activeRunner ->
                ensureAndSeedAdkSession(activeRunner, initialSessionId, existing)
            }
        } else {
            val welcomeText = DEFAULT_WELCOME_TEXT
            val initialMsg = TagAlongMessage(
                id = "init-welcome",
                sender = TagAlongSender.AGENT,
                text = welcomeText,
                timestamp = epochMillis()
            )
            _uiState.update { it.copy(messages = listOf(initialMsg)) }
            chatRepository.insertMessage(
                ChatMessage(
                    sessionId = initialSessionId,
                    sender = "agent",
                    text = welcomeText,
                    timestamp = initialMsg.timestamp
                )
            )
        }
    }

    private suspend fun initMapFromSources() {
        if (locationProvider != null) {
            try {
                val loc = locationProvider.getCurrentLocation()
                if (loc != null) {
                    _uiState.update { it.copy(mapCenterLat = loc.latitude, mapCenterLng = loc.longitude) }
                }
            } catch (_: Exception) {}
        }

        if (hybridSpotRepository != null) {
            try {
                val spots = hybridSpotRepository.getAllSpots()
                if (spots.isNotEmpty()) {
                    val markers = spots.mapIndexed { index, s ->
                        MapMarker(
                            id = (index + 1).toLong(),
                            latitude = s.spot.latitude,
                            longitude = s.spot.longitude,
                            category = s.spot.category.ifBlank { "Mural" },
                            status = if (s.spot.isPublic) "Public" else "Saved",
                            title = s.spot.description.lines().firstOrNull()?.trim()?.take(30)?.ifBlank { "Mural #${index + 1}" } ?: "Mural #${index + 1}",
                            isStarred = s.spot.isStarred,
                            onClick = {}
                        )
                    }
                    _uiState.update { state ->
                        val first = spots.first()
                        state.copy(
                            mapMarkers = markers,
                            mapCenterLat = if (locationProvider == null) first.spot.latitude else state.mapCenterLat,
                            mapCenterLng = if (locationProvider == null) first.spot.longitude else state.mapCenterLng,
                            focusedTitle = first.spot.description.lines().firstOrNull()?.trim()?.take(30) ?: state.focusedTitle
                        )
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun onPhotoAttached(uri: String) {
        _uiState.update { it.copy(attachedPhotoUri = uri) }
        if (photoProcessor != null) {
            viewModelScope.launch {
                try {
                    val metadata = photoProcessor.extractMetadataFromUri(uri)
                    val photoLat = metadata?.latitude
                    val photoLon = metadata?.longitude
                    if (photoLat != null && photoLon != null) {
                        _uiState.update {
                            it.copy(
                                mapCenterLat = photoLat,
                                mapCenterLng = photoLon
                            )
                        }
                    }
                    photoProcessor.createThumbnailFromUri(uri)
                } catch (_: Exception) {}
            }
        }
    }

    fun onClearPhoto() {
        _uiState.update { it.copy(attachedPhotoUri = null) }
    }

    fun sendMessage(
        text: String = _uiState.value.inputText,
        photoUri: String? = _uiState.value.attachedPhotoUri
    ) {
        val trimmed = text.trim()
        val currentPhoto = photoUri

        if (trimmed.isBlank() && currentPhoto == null) return

        val currentSessionId = _uiState.value.sessionId
        val userTimestamp = epochMillis()
        val userMsg = TagAlongMessage(
            id = "user-$userTimestamp",
            sender = TagAlongSender.USER,
            text = trimmed.ifBlank { null },
            photoUri = currentPhoto,
            timestamp = userTimestamp
        )

        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMsg,
                inputText = "",
                attachedPhotoUri = null
            )
        }

        val userChatMessage = ChatMessage(
            sessionId = currentSessionId,
            sender = "user",
            text = trimmed,
            imagePath = currentPhoto,
            timestamp = userTimestamp
        )

        val activeRunner = resolveRunner()
        if (activeRunner == null) {
            val offlineText = "I'm currently in offline mode. You can still explore the mock catalog chips and view local murals on the map!"
            val agentMsg = TagAlongMessage(
                id = "agent-${epochMillis()}",
                sender = TagAlongSender.AGENT,
                text = offlineText,
                timestamp = epochMillis()
            )
            _uiState.update { state ->
                state.copy(
                    isOffline = true,
                    offlineMessage = "Agent runner not configured (offline mode)",
                    messages = state.messages + agentMsg
                )
            }
            viewModelScope.launch {
                chatRepository.insertMessage(userChatMessage)
                chatRepository.insertMessage(
                    ChatMessage(
                        sessionId = currentSessionId,
                        sender = "agent",
                        text = offlineText,
                        timestamp = agentMsg.timestamp
                    )
                )
            }
            return
        }

        _uiState.update { it.copy(isGenerating = true, isOffline = false, offlineMessage = null) }
        val agentMsgId = "agent-${epochMillis()}"

        _uiState.update { state ->
            state.copy(
                messages = state.messages + TagAlongMessage(
                    id = agentMsgId,
                    sender = TagAlongSender.AGENT,
                    text = "",
                    timestamp = epochMillis()
                )
            )
        }

        viewModelScope.launch {
            val priorMessages = chatRepository.getMessagesListForSession(currentSessionId)
            chatRepository.insertMessage(userChatMessage)
            ensureAndSeedAdkSession(activeRunner, currentSessionId, priorMessages)

            var accumulated = ""
            var receivedPartials = false
            var lastProcessedPayload: String? = null
            var lastSurfaceIds: List<String> = emptyList()
            try {
                val parts = mutableListOf<Part>()
                var contextLat: Double? = null
                var contextLon: Double? = null
                var resolvedThumbPath: String? = null

                if (!currentPhoto.isNullOrBlank()) {
                    var addedBytes = false
                    if (photoProcessor != null) {
                        runCatching {
                            val bytes = photoProcessor.decodeScaledBitmap(currentPhoto, maxDimension = 1024)
                            if (bytes != null && bytes.isNotEmpty()) {
                                parts.add(Part(inlineData = Blob(mimeType = "image/jpeg", data = bytes)))
                                addedBytes = true
                            }
                        }
                        val metadata = runCatching { photoProcessor.extractMetadataFromUri(currentPhoto) }.getOrNull()
                        if (metadata?.latitude != null && metadata.longitude != null) {
                            contextLat = metadata.latitude
                            contextLon = metadata.longitude
                        }
                        resolvedThumbPath = runCatching { photoProcessor.createThumbnailFromUri(currentPhoto) }.getOrNull()
                    }
                    val photoDescriptor = if (addedBytes) {
                        "User attached photo (binary JPEG image included): $currentPhoto"
                    } else {
                        "User attached photo URI: $currentPhoto"
                    }
                    parts.add(Part(text = photoDescriptor))
                }

                if (contextLat == null || contextLon == null) {
                    val deviceLoc = runCatching { locationProvider?.getCurrentLocation() }.getOrNull()
                    if (deviceLoc != null && (deviceLoc.latitude != 0.0 || deviceLoc.longitude != 0.0)) {
                        contextLat = deviceLoc.latitude
                        contextLon = deviceLoc.longitude
                    }
                }

                val contextFields = buildList {
                    if (contextLat != null && contextLon != null) {
                        add("latitude=$contextLat")
                        add("longitude=$contextLon")
                    }
                    if (!currentPhoto.isNullOrBlank()) {
                        add("imagePath=$currentPhoto")
                    }
                    if (!resolvedThumbPath.isNullOrBlank()) {
                        add("thumbnailPath=$resolvedThumbPath")
                    }
                }
                if (contextFields.isNotEmpty()) {
                    parts.add(Part(text = "[Context: ${contextFields.joinToString(", ")}]"))
                }

                if (trimmed.isNotBlank()) {
                    parts.add(Part(text = trimmed))
                }
                val content = Content(role = "user", parts = parts)

                activeRunner.runAsync(
                    ADK_USER_ID,
                    currentSessionId,
                    "inv-${epochMillis()}",
                    content,
                    emptyMap()
                ).collect { event ->
                    // Ignore user-echoed events and intermediate tool-call / tool-response turns.
                    if (event.author == "user") return@collect
                    if (event.functionCalls().isNotEmpty() || event.functionResponses().isNotEmpty()) {
                        return@collect
                    }

                    val chunk = event.content?.parts?.mapNotNull { it.text }?.joinToString("") ?: ""
                    if (chunk.isEmpty()) return@collect

                    if (event.partial) {
                        accumulated += chunk
                        receivedPartials = true
                    } else {
                        if (receivedPartials) {
                            // Final non-partial aggregated event replaces accumulated partials without duplication.
                            accumulated = chunk
                            receivedPartials = false
                        } else if (chunk != accumulated) {
                            accumulated = if (accumulated.isEmpty()) chunk else "$accumulated\n$chunk"
                        }
                    }

                    val (prose, a2uiPayload) = extractTextAndA2UIPayload(accumulated)
                    if (!a2uiPayload.isNullOrBlank() && a2uiPayload != lastProcessedPayload) {
                        lastProcessedPayload = a2uiPayload
                        lastSurfaceIds = if (engine != null) {
                            try {
                                engine!!.processProtocolMessage(a2uiPayload)
                            } catch (_: Exception) {
                                emptyList()
                            }
                        } else emptyList()
                        syncMapWithA2UI(a2uiPayload)
                    }

                    _uiState.update { state ->
                        val updated = state.messages.map { msg ->
                            if (msg.id == agentMsgId) {
                                msg.copy(
                                    text = prose.ifBlank { null },
                                    a2uiPayload = a2uiPayload,
                                    surfaceIds = lastSurfaceIds
                                )
                            } else msg
                        }
                        state.copy(messages = updated)
                    }
                }

                val (finalProse, finalPayload) = extractTextAndA2UIPayload(accumulated)
                val effectiveText = if (finalProse.isBlank() && finalPayload == null) {
                    "I searched the area, but couldn't find additional details for that request."
                } else {
                    finalProse
                }

                val finalSurfaceIds = if (!finalPayload.isNullOrBlank() && finalPayload != lastProcessedPayload && engine != null) {
                    try {
                        engine!!.processProtocolMessage(finalPayload)
                    } catch (_: Exception) {
                        emptyList()
                    }
                } else {
                    lastSurfaceIds
                }

                _uiState.update { state ->
                    val updated = state.messages.map { msg ->
                        if (msg.id == agentMsgId) {
                            msg.copy(
                                text = effectiveText.ifBlank { null },
                                a2uiPayload = finalPayload,
                                surfaceIds = finalSurfaceIds
                            )
                        } else msg
                    }
                    state.copy(messages = updated)
                }

                chatRepository.insertMessage(
                    ChatMessage(
                        sessionId = _uiState.value.sessionId,
                        sender = "agent",
                        text = effectiveText,
                        a2uiPayloadJson = finalPayload,
                        timestamp = epochMillis()
                    )
                )
            } catch (e: Exception) {
                val errorMsg = "⚠️ Unable to reach TagAlong agent (${e.message ?: "network error"}). Switched to offline mode."
                _uiState.update { state ->
                    val updated = state.messages.map { msg ->
                        if (msg.id == agentMsgId) {
                            msg.copy(text = if (accumulated.isNotBlank()) "$accumulated\n\n$errorMsg" else errorMsg)
                        } else msg
                    }
                    state.copy(
                        isOffline = true,
                        offlineMessage = e.message ?: "Agent connection failed",
                        messages = updated
                    )
                }
                chatRepository.insertMessage(
                    ChatMessage(
                        sessionId = _uiState.value.sessionId,
                        sender = "agent",
                        text = errorMsg,
                        timestamp = epochMillis()
                    )
                )
            } finally {
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }

    fun selectMockFixture(fixtureName: String, fixtureJson: String) {
        val fixtureMsgId = "fixture-${epochMillis()}"
        val introText = "Here is the simulated **$fixtureName** A2UI surface:"
        val surfaceIds = if (engine != null) {
            try {
                engine!!.processProtocolMessage(fixtureJson)
            } catch (_: Exception) {
                emptyList()
            }
        } else emptyList()

        val msg = TagAlongMessage(
            id = fixtureMsgId,
            sender = TagAlongSender.AGENT,
            text = introText,
            a2uiPayload = fixtureJson,
            surfaceIds = surfaceIds,
            timestamp = epochMillis()
        )
        _uiState.update { it.copy(messages = it.messages + msg) }
        syncMapWithA2UI(fixtureJson)

        viewModelScope.launch {
            chatRepository.insertMessage(
                ChatMessage(
                    sessionId = _uiState.value.sessionId,
                    sender = "agent",
                    text = introText,
                    a2uiPayloadJson = fixtureJson,
                    timestamp = msg.timestamp
                )
            )
        }
    }

    fun handleAction(action: A2UIAction) {
        when (action) {
            is A2UIAction.ViewOnMap -> {
                if (action.latitude != 0.0 || action.longitude != 0.0) {
                    _uiState.update { state ->
                        val markers = state.mapMarkers.toMutableList()
                        if (markers.none { it.latitude == action.latitude && it.longitude == action.longitude }) {
                            markers.add(
                                MapMarker(
                                    id = (markers.size + 1).toLong(),
                                    latitude = action.latitude,
                                    longitude = action.longitude,
                                    category = "Art",
                                    status = "Active",
                                    title = action.title,
                                    onClick = {}
                                )
                            )
                        }
                        state.copy(
                            mapCenterLat = action.latitude,
                            mapCenterLng = action.longitude,
                            focusedTitle = action.title,
                            mapMarkers = markers
                        )
                    }
                    _navigationEvents.tryEmit(TagAlongNavEvent.ViewOnMainMap(action.latitude, action.longitude))
                }
            }
            is A2UIAction.NavigateToStop -> {
                _uiState.update { state ->
                    state.copy(
                        mapCenterLat = action.latitude,
                        mapCenterLng = action.longitude,
                        focusedTitle = "Stop #${action.stopIndex}: ${action.title}"
                    )
                }
                if (action.latitude != 0.0 || action.longitude != 0.0) {
                    _navigationEvents.tryEmit(TagAlongNavEvent.ViewOnMainMap(action.latitude, action.longitude))
                }
            }
            is A2UIAction.StarSpot -> {
                _uiState.update { state ->
                    val updated = state.mapMarkers.map { marker ->
                        if (marker.title.contains(action.spotId, ignoreCase = true)) {
                            marker.copy(isStarred = !marker.isStarred)
                        } else marker
                    }
                    state.copy(mapMarkers = updated)
                }
                if (hybridSpotRepository != null) {
                    viewModelScope.launch {
                        hybridSpotRepository.toggleSpotStar(action.spotId)
                    }
                }
            }
            is A2UIAction.AnswerQuiz -> {
                if (resolveRunner() != null) {
                    forwardClientEventToRunner(
                        eventName = "answer_quiz",
                        surfaceId = null,
                        context = mapOf(
                            "quizId" to action.quizId,
                            "optionId" to action.optionId,
                            "isCorrect" to action.isCorrect
                        )
                    )
                } else {
                    val feedbackText = if (action.isCorrect) {
                        "🎯 Spot on! That's the correct technique."
                    } else {
                        "❌ Not quite! Check the explanation on the card."
                    }
                    appendLocalAgentFeedback(feedbackText)
                }
            }
            is A2UIAction.VoteOption -> {
                if (resolveRunner() != null) {
                    forwardClientEventToRunner(
                        eventName = "vote_option",
                        surfaceId = null,
                        context = mapOf(
                            "pollId" to action.pollId,
                            "optionId" to action.optionId
                        )
                    )
                } else {
                    val voteText = "🗳️ Vote recorded for '${action.optionId}'! Thanks for curating the city's street art."
                    appendLocalAgentFeedback(voteText)
                }
            }
            is A2UIAction.Retry -> {
                val retryText = "🔄 Retrying card render..."
                val agentMsg = TagAlongMessage(
                    id = "msg-${epochMillis()}",
                    sender = TagAlongSender.AGENT,
                    text = retryText,
                    a2uiPayload = action.rawJson,
                    timestamp = epochMillis()
                )
                _uiState.update { it.copy(messages = it.messages + agentMsg) }
                viewModelScope.launch {
                    chatRepository.insertMessage(
                        ChatMessage(
                            sessionId = _uiState.value.sessionId,
                            sender = "agent",
                            text = retryText,
                            a2uiPayloadJson = action.rawJson,
                            timestamp = epochMillis()
                        )
                    )
                }
            }
            is A2UIAction.OpenSpotDetail -> {
                val spotTitle = "Spot #${action.spotId}"
                _uiState.update { it.copy(focusedTitle = spotTitle) }
                resolveAndEmitOpenSpotDetail(action.spotId)
            }
            is A2UIAction.ClientEvent -> {
                when (action.name) {
                    "focusMap", "viewOnMap" -> {
                        val lat = (action.context["latitude"] as? Number)?.toDouble()
                            ?: (action.context["lat"] as? Number)?.toDouble()
                            ?: action.context["latitude"]?.toString()?.toDoubleOrNull()
                            ?: action.context["lat"]?.toString()?.toDoubleOrNull() ?: 0.0
                        val lng = (action.context["longitude"] as? Number)?.toDouble()
                            ?: (action.context["lng"] as? Number)?.toDouble()
                            ?: (action.context["lon"] as? Number)?.toDouble()
                            ?: action.context["longitude"]?.toString()?.toDoubleOrNull()
                            ?: action.context["lng"]?.toString()?.toDoubleOrNull()
                            ?: action.context["lon"]?.toString()?.toDoubleOrNull() ?: 0.0
                        val title = (action.context["title"] as? String) ?: "Selected Spot"
                        if (lat != 0.0 || lng != 0.0) {
                            _uiState.update { it.copy(mapCenterLat = lat, mapCenterLng = lng, focusedTitle = title) }
                            _navigationEvents.tryEmit(TagAlongNavEvent.ViewOnMainMap(lat, lng))
                        }
                    }
                    "openSpotDetail", "open_spot_detail", "openDetail" -> {
                        val spotId = (action.context["spotId"] ?: action.context["uuid"] ?: action.context["id"])?.toString() ?: ""
                        _uiState.update { it.copy(focusedTitle = "Spot #$spotId") }
                        resolveAndEmitOpenSpotDetail(spotId)
                    }
                    "save_spot", "saveSpot" -> {
                        val spotUuid = (action.context["spotId"] ?: action.context["uuid"] ?: action.context["id"])?.toString()
                        val title = (action.context["title"] ?: action.context["name"])?.toString() ?: "Saved Mural"
                        val artist = action.context["artist"]?.toString()
                        var lat = (action.context["latitude"] as? Number)?.toDouble()
                            ?: (action.context["lat"] as? Number)?.toDouble()
                            ?: action.context["latitude"]?.toString()?.toDoubleOrNull()
                            ?: action.context["lat"]?.toString()?.toDoubleOrNull() ?: 0.0
                        var lon = (action.context["longitude"] as? Number)?.toDouble()
                            ?: (action.context["lng"] as? Number)?.toDouble()
                            ?: (action.context["lon"] as? Number)?.toDouble()
                            ?: action.context["longitude"]?.toString()?.toDoubleOrNull()
                            ?: action.context["lng"]?.toString()?.toDoubleOrNull()
                            ?: action.context["lon"]?.toString()?.toDoubleOrNull() ?: 0.0
                        val rawImageUrl = (action.context["imageUrl"] ?: action.context["photoUri"] ?: action.context["imageUri"] ?: action.context["imagePath"])?.toString()
                            ?: _uiState.value.attachedPhotoUri ?: ""
                        val category = action.context["category"]?.toString() ?: "graffiti"

                        viewModelScope.launch {
                            var thumbnailPath = rawImageUrl
                            if (rawImageUrl.isNotBlank() && photoProcessor != null) {
                                try {
                                    val thumb = photoProcessor.createThumbnailFromUri(rawImageUrl)
                                    if (!thumb.isNullOrBlank()) {
                                        thumbnailPath = thumb
                                    }
                                    if (lat == 0.0 && lon == 0.0) {
                                        val metadata = photoProcessor.extractMetadataFromUri(rawImageUrl)
                                        val photoLat = metadata?.latitude
                                        val photoLon = metadata?.longitude
                                        if (photoLat != null && photoLon != null) {
                                            lat = photoLat
                                            lon = photoLon
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                            if (lat == 0.0 && lon == 0.0) {
                                val deviceLoc = runCatching { locationProvider?.getCurrentLocation() }.getOrNull()
                                if (deviceLoc != null && (deviceLoc.latitude != 0.0 || deviceLoc.longitude != 0.0)) {
                                    lat = deviceLoc.latitude
                                    lon = deviceLoc.longitude
                                }
                            }

                            if (hybridSpotRepository != null) {
                                val normalizedCategory = category.trim().lowercase()
                                val tagsList = (action.context["tags"] as? List<*>)?.mapNotNull { it?.toString() }?.toMutableList() ?: mutableListOf("saved")
                                val finalCategory = if (normalizedCategory in Spot.CATEGORIES) {
                                    normalizedCategory
                                } else {
                                    if (!tagsList.any { it.equals(category.trim(), ignoreCase = true) }) {
                                        tagsList.add(category.trim())
                                    }
                                    "graffiti"
                                }

                                val spot = Spot(
                                    uuid = if (!spotUuid.isNullOrBlank()) spotUuid else generateUuid(),
                                    latitude = lat,
                                    longitude = lon,
                                    createdAt = epochMillis(),
                                    description = title,
                                    tags = tagsList,
                                    category = finalCategory,
                                    status = "active",
                                    artists = if (!artist.isNullOrBlank()) listOf(artist.trim()) else emptyList(),
                                    isPublic = false
                                )
                                hybridSpotRepository.saveSpot(
                                    spot = spot,
                                    imagePath = rawImageUrl,
                                    thumbnailPath = thumbnailPath
                                )
                            }

                            engine?.updateDataModel(
                                surfaceId = action.surfaceId,
                                path = "/spot/isSaved",
                                value = true
                            )

                            if (lat != 0.0 || lon != 0.0) {
                                _uiState.update { state ->
                                    val markers = state.mapMarkers.toMutableList()
                                    val existingIndex = markers.indexOfFirst { it.latitude == lat && it.longitude == lon }
                                    if (existingIndex >= 0) {
                                        markers[existingIndex] = markers[existingIndex].copy(
                                            title = title,
                                            status = "Saved"
                                        )
                                    } else {
                                        markers.add(
                                            MapMarker(
                                                id = (markers.size + 1).toLong(),
                                                latitude = lat,
                                                longitude = lon,
                                                category = "Mural",
                                                status = "Saved",
                                                title = title,
                                                onClick = {}
                                            )
                                        )
                                    }
                                    state.copy(
                                        mapCenterLat = lat,
                                        mapCenterLng = lon,
                                        focusedTitle = title,
                                        mapMarkers = markers
                                    )
                                }
                            }
                        }
                    }
                    "toggle_star", "toggleStar" -> {
                        val spotUuid = (action.context["spotId"] ?: action.context["uuid"] ?: action.context["id"])?.toString() ?: ""
                        viewModelScope.launch {
                            val newStarred = if (hybridSpotRepository != null && spotUuid.isNotBlank()) {
                                hybridSpotRepository.toggleSpotStar(spotUuid)
                            } else {
                                val current = _uiState.value.mapMarkers.firstOrNull {
                                    it.title.contains(spotUuid, ignoreCase = true) || it.id.toString() == spotUuid
                                }?.isStarred ?: false
                                !current
                            }

                            engine?.updateDataModel(
                                surfaceId = action.surfaceId,
                                path = "/spot/isStarred",
                                value = newStarred
                            )

                            _uiState.update { state ->
                                val updated = state.mapMarkers.map { marker ->
                                    if (marker.title.contains(spotUuid, ignoreCase = true) || marker.id.toString() == spotUuid) {
                                        marker.copy(isStarred = newStarred)
                                    } else marker
                                }
                                state.copy(mapMarkers = updated)
                            }
                        }
                    }
                    else -> {
                        if (resolveRunner() != null) {
                            forwardClientEventToRunner(
                                eventName = action.name,
                                surfaceId = action.surfaceId,
                                context = action.context
                            )
                        } else {
                            when (action.name) {
                                "answer_quiz", "submit_quiz" -> {
                                    val selected = (action.context["selectedOption"] ?: action.context["optionId"])?.toString()
                                    val msg = if (!selected.isNullOrBlank()) {
                                        "🎯 Answer submitted: $selected"
                                    } else {
                                        "🎯 Quiz answer submitted!"
                                    }
                                    appendLocalAgentFeedback(msg)
                                }
                                "vote_option", "cast_vote" -> {
                                    val selected = (action.context["selectedOption"] ?: action.context["optionId"])?.toString() ?: "your choice"
                                    appendLocalAgentFeedback("🗳️ Vote recorded for '$selected'! Thanks for curating the city's street art.")
                                }
                            }
                        }
                    }
                }
            }
            is A2UIAction.ClientError -> {
                println("A2UI ClientError [${action.code}]: ${action.message}")
            }
            is A2UIAction.Custom -> {
                // Custom action hook
            }
        }
    }

    private fun resolveAndEmitOpenSpotDetail(rawSpotId: String) {
        val trimmed = rawSpotId.trim()
        if (trimmed.isBlank()) return

        val directId = trimmed.toLongOrNull()
        if (directId != null) {
            if (directId > 0L) {
                _navigationEvents.tryEmit(TagAlongNavEvent.OpenSpotDetail(directId))
            }
            return
        }

        val repo = hybridSpotRepository ?: return
        viewModelScope.launch {
            val spotDetails = runCatching { repo.getSpotByUuid(trimmed) }.getOrNull()
            val localId = spotDetails?.spot?.id ?: 0L
            if (localId > 0L) {
                _navigationEvents.emit(TagAlongNavEvent.OpenSpotDetail(localId))
            }
        }
    }

    private fun appendLocalAgentFeedback(text: String) {
        val now = epochMillis()
        val agentMsg = TagAlongMessage(
            id = "msg-$now",
            sender = TagAlongSender.AGENT,
            text = text,
            timestamp = now
        )
        _uiState.update { it.copy(messages = it.messages + agentMsg) }
        viewModelScope.launch {
            chatRepository.insertMessage(
                ChatMessage(
                    sessionId = _uiState.value.sessionId,
                    sender = "agent",
                    text = text,
                    timestamp = now
                )
            )
        }
    }

    private fun forwardClientEventToRunner(
        eventName: String,
        surfaceId: String?,
        context: Map<String, Any?>
    ) {
        val activeRunner = resolveRunner() ?: return

        val payloadJson = JsonObject(
            buildMap {
                put("name", JsonPrimitive(eventName))
                if (!surfaceId.isNullOrBlank()) {
                    put("surfaceId", JsonPrimitive(surfaceId))
                }
                put("context", context.toJsonElement())
            }
        ).toString()

        val currentSessionId = _uiState.value.sessionId
        _uiState.update { it.copy(isGenerating = true, isOffline = false, offlineMessage = null) }

        viewModelScope.launch {
            val persistedMessages = chatRepository.getMessagesListForSession(currentSessionId)
            ensureAndSeedAdkSession(activeRunner, currentSessionId, persistedMessages)

            var accumulated = ""
            var receivedPartials = false
            var lastProcessedPayload: String? = null
            var lastSurfaceIds: List<String> = emptyList()
            try {
                val content = Content(
                    role = "user",
                    parts = listOf(Part(text = "[A2UI ClientEvent] $payloadJson"))
                )

                activeRunner.runAsync(
                    ADK_USER_ID,
                    currentSessionId,
                    "inv-evt-${epochMillis()}",
                    content,
                    emptyMap()
                ).collect { event ->
                    if (event.author == "user") return@collect
                    if (event.functionCalls().isNotEmpty() || event.functionResponses().isNotEmpty()) {
                        return@collect
                    }

                    val chunk = event.content?.parts?.mapNotNull { it.text }?.joinToString("") ?: ""
                    if (chunk.isEmpty()) return@collect

                    if (event.partial) {
                        accumulated += chunk
                        receivedPartials = true
                    } else {
                        if (receivedPartials) {
                            accumulated = chunk
                            receivedPartials = false
                        } else if (chunk != accumulated) {
                            accumulated = if (accumulated.isEmpty()) chunk else "$accumulated\n$chunk"
                        }
                    }

                    val (_, a2uiPayload) = extractTextAndA2UIPayload(accumulated)
                    if (!a2uiPayload.isNullOrBlank() && a2uiPayload != lastProcessedPayload) {
                        lastProcessedPayload = a2uiPayload
                        lastSurfaceIds = if (engine != null) {
                            runCatching { engine!!.processProtocolMessage(a2uiPayload) }.getOrDefault(emptyList())
                        } else emptyList()
                        syncMapWithA2UI(a2uiPayload)
                    }
                }

                val (finalProse, finalPayload) = extractTextAndA2UIPayload(accumulated)
                val finalSurfaceIds = if (!finalPayload.isNullOrBlank() && finalPayload != lastProcessedPayload && engine != null) {
                    runCatching { engine!!.processProtocolMessage(finalPayload) }.getOrDefault(emptyList())
                } else {
                    lastSurfaceIds
                }
                if (!finalPayload.isNullOrBlank() && finalPayload != lastProcessedPayload) {
                    syncMapWithA2UI(finalPayload)
                }

                val existingSurfaceIds = _uiState.value.messages.flatMap { it.surfaceIds }.toSet()
                val newSurfaceIds = finalSurfaceIds.filter { it !in existingSurfaceIds }

                if (finalProse.isNotBlank() || newSurfaceIds.isNotEmpty()) {
                    val now = epochMillis()
                    val agentMsg = TagAlongMessage(
                        id = "agent-evt-$now",
                        sender = TagAlongSender.AGENT,
                        text = finalProse.ifBlank { null },
                        a2uiPayload = if (newSurfaceIds.isNotEmpty()) finalPayload else null,
                        surfaceIds = newSurfaceIds,
                        timestamp = now
                    )
                    _uiState.update { state -> state.copy(messages = state.messages + agentMsg) }
                    chatRepository.insertMessage(
                        ChatMessage(
                            sessionId = _uiState.value.sessionId,
                            sender = "agent",
                            text = finalProse,
                            a2uiPayloadJson = if (newSurfaceIds.isNotEmpty()) finalPayload else null,
                            timestamp = now
                        )
                    )
                }
            } catch (e: Exception) {
                val errorMsg = "⚠️ Unable to process action (${e.message ?: "network error"})."
                appendLocalAgentFeedback(errorMsg)
            } finally {
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }

    private fun Any?.toJsonElement(): JsonElement = when (this) {
        null -> JsonNull
        is JsonElement -> this
        is Boolean -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is String -> JsonPrimitive(this)
        is Map<*, *> -> JsonObject(entries.associate { (k, v) -> k.toString() to v.toJsonElement() })
        is Iterable<*> -> JsonArray(map { it.toJsonElement() })
        else -> JsonPrimitive(toString())
    }

    private fun extractTextAndA2UIPayload(fullText: String): Pair<String, String?> {
        val trimmed = fullText.trim()
        if (trimmed.isEmpty()) return Pair("", null)

        val fenceRegex = Regex("```(?:json|a2ui)?\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
        val match = fenceRegex.find(trimmed)
        if (match != null) {
            val jsonCandidate = match.groupValues[1].trim()
            if ((jsonCandidate.startsWith("{") && jsonCandidate.endsWith("}")) ||
                (jsonCandidate.startsWith("[") && jsonCandidate.endsWith("]"))
            ) {
                val prose = trimmed.removeRange(match.range).trim()
                return Pair(prose, jsonCandidate)
            }
        }

        val firstBrace = trimmed.indexOf('{')
        val firstBracket = trimmed.indexOf('[')
        val startIdx = when {
            firstBrace >= 0 && firstBracket >= 0 -> minOf(firstBrace, firstBracket)
            firstBrace >= 0 -> firstBrace
            firstBracket >= 0 -> firstBracket
            else -> -1
        }

        if (startIdx >= 0) {
            val candidate = trimmed.substring(startIdx).trim()
            if ((candidate.startsWith("{") && candidate.endsWith("}")) ||
                (candidate.startsWith("[") && candidate.endsWith("]"))
            ) {
                if (candidate.contains("\"type\"") || candidate.contains("\"component\"") ||
                    candidate.contains("\"createSurface\"") || candidate.contains("\"updateComponents\"") ||
                    candidate.contains("\"updateDataModel\"") || candidate.contains("\"version\"")
                ) {
                    val prose = trimmed.substring(0, startIdx).trim()
                    return Pair(prose, candidate)
                }
            }
        }

        return Pair(trimmed, null)
    }

    private fun syncMapWithA2UI(a2uiJson: String) {
        try {
            val json = Json { ignoreUnknownKeys = true; isLenient = true }
            val root = json.parseToJsonElement(a2uiJson)

            val components = mutableListOf<JsonObject>()
            val dataModel = mutableMapOf<String, JsonElement>()

            fun collectDataAndComponents(element: JsonElement) {
                when (element) {
                    is JsonArray -> element.forEach { collectDataAndComponents(it) }
                    is JsonObject -> {
                        if (element.containsKey("updateDataModel")) {
                            val udm = element["updateDataModel"]?.jsonObject
                            if (udm != null) {
                                val basePath = udm["path"]?.jsonPrimitive?.contentOrNull ?: ""
                                val value = udm["value"]
                                if (value is JsonObject) {
                                    for ((k, v) in value) {
                                        dataModel["$basePath/$k"] = v
                                        dataModel["/$k"] = v
                                        dataModel[k] = v
                                    }
                                } else if (value != null) {
                                    dataModel[basePath] = value
                                    dataModel[basePath.removePrefix("/")] = value
                                }
                            }
                        }
                        if (element.containsKey("updateComponents")) {
                            val uc = element["updateComponents"]?.jsonObject
                            uc?.get("components")?.jsonArray?.forEach { collectDataAndComponents(it) }
                        } else if (element.containsKey("components") && element["components"] is JsonArray) {
                            element["components"]?.jsonArray?.forEach { collectDataAndComponents(it) }
                        } else if (element.containsKey("type") || element.containsKey("component")) {
                            components.add(element)
                        }
                    }
                    else -> {}
                }
            }

            collectDataAndComponents(root)

            fun resolveString(prop: JsonElement?): String? {
                if (prop == null) return null
                if (prop is JsonPrimitive && prop.isString) return prop.contentOrNull
                if (prop is JsonObject) {
                    val path = prop["path"]?.jsonPrimitive?.contentOrNull
                    if (path != null) {
                        val resolved = dataModel[path] ?: dataModel[path.removePrefix("/")] ?: dataModel[path.substringAfterLast("/")]
                        if (resolved is JsonPrimitive && resolved.isString) {
                            return resolved.contentOrNull
                        }
                    }
                    val literal = prop["literalString"]?.jsonPrimitive?.contentOrNull
                    if (literal != null) return literal
                }
                return null
            }

            fun resolveBoolean(prop: JsonElement?): Boolean {
                if (prop == null) return false
                if (prop is JsonPrimitive) return prop.booleanOrNull ?: false
                if (prop is JsonObject) {
                    val path = prop["path"]?.jsonPrimitive?.contentOrNull
                    if (path != null) {
                        val resolved = dataModel[path] ?: dataModel[path.removePrefix("/")] ?: dataModel[path.substringAfterLast("/")]
                        if (resolved is JsonPrimitive) {
                            return resolved.booleanOrNull ?: false
                        }
                    }
                }
                return false
            }

            for (comp in components) {
                val type = comp["type"]?.jsonPrimitive?.contentOrNull
                    ?: comp["component"]?.jsonPrimitive?.contentOrNull
                    ?: continue
                val props = comp["properties"]?.jsonObject ?: comp

                when {
                    type.equals("SpotCard", ignoreCase = true) || type.equals("NeonSpotCard", ignoreCase = true) -> {
                        val lat = props["latitude"]?.jsonPrimitive?.doubleOrNull
                        val lon = props["longitude"]?.jsonPrimitive?.doubleOrNull
                        val title = resolveString(props["title"]) ?: "Mural Spot"
                        val isStarred = resolveBoolean(props["isStarred"])

                        if (lat != null && lon != null) {
                            _uiState.update { state ->
                                val markers = state.mapMarkers.toMutableList()
                                val existingIndex = markers.indexOfFirst { it.latitude == lat && it.longitude == lon }
                                if (existingIndex >= 0) {
                                    markers[existingIndex] = markers[existingIndex].copy(
                                        title = title,
                                        isStarred = isStarred
                                    )
                                } else {
                                    markers.add(
                                        MapMarker(
                                            id = (markers.size + 1).toLong(),
                                            latitude = lat,
                                            longitude = lon,
                                            category = "Mural",
                                            status = "Public",
                                            title = title,
                                            isStarred = isStarred,
                                            onClick = {}
                                        )
                                    )
                                }
                                state.copy(
                                    mapCenterLat = lat,
                                    mapCenterLng = lon,
                                    focusedTitle = title,
                                    mapMarkers = markers
                                )
                            }
                        }
                    }

                    type.equals("TourCard", ignoreCase = true) || type.equals("NeonTourCard", ignoreCase = true) -> {
                        val tourTitle = resolveString(props["tourTitle"]) ?: "Street Art Tour"
                        val waypoints: JsonArray = props["waypoints"]?.jsonArray ?: JsonArray(emptyList<JsonElement>())

                        if (waypoints.isNotEmpty()) {
                            val firstWp = waypoints.first().jsonObject
                            val firstLat = firstWp["latitude"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                            val firstLon = firstWp["longitude"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                            val firstTitle = resolveString(firstWp["title"]) ?: "Stop #1"

                            _uiState.update { state ->
                                val markers = state.mapMarkers.toMutableList()
                                waypoints.forEachIndexed { idx, wpElem ->
                                    val wp = wpElem.jsonObject
                                    val wpLat = wp["latitude"]?.jsonPrimitive?.doubleOrNull ?: return@forEachIndexed
                                    val wpLon = wp["longitude"]?.jsonPrimitive?.doubleOrNull ?: return@forEachIndexed
                                    val wpTitle = resolveString(wp["title"]) ?: "Stop #${idx + 1}"
                                    val completed = wp["completed"]?.jsonPrimitive?.booleanOrNull ?: false

                                    val existingIndex = markers.indexOfFirst { it.latitude == wpLat && it.longitude == wpLon }
                                    if (existingIndex >= 0) {
                                        markers[existingIndex] = markers[existingIndex].copy(
                                            title = wpTitle,
                                            category = "Tour Stop #${idx + 1}",
                                            status = if (completed) "Completed" else "Upcoming"
                                        )
                                    } else {
                                        markers.add(
                                            MapMarker(
                                                id = (markers.size + 1).toLong(),
                                                latitude = wpLat,
                                                longitude = wpLon,
                                                category = "Tour Stop #${idx + 1}",
                                                status = if (completed) "Completed" else "Upcoming",
                                                title = wpTitle,
                                                onClick = {}
                                            )
                                        )
                                    }
                                }
                                state.copy(
                                    mapCenterLat = if (firstLat != 0.0) firstLat else state.mapCenterLat,
                                    mapCenterLng = if (firstLon != 0.0) firstLon else state.mapCenterLng,
                                    focusedTitle = "$tourTitle (Stop #1: $firstTitle)",
                                    mapMarkers = markers
                                )
                            }
                        }
                    }

                    type.equals("MapPinOverlayWidget", ignoreCase = true) ||
                    type.equals("NeonMapPinOverlayWidget", ignoreCase = true) ||
                    type.equals("MapPinOverlay", ignoreCase = true) -> {
                        val centerLat = props["centerLatitude"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                        val centerLon = props["centerLongitude"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                        val zoom = props["zoomLevel"]?.jsonPrimitive?.doubleOrNull ?: 14.0
                        val overlayTitle = props["title"]?.jsonPrimitive?.contentOrNull ?: "Art Map Overlay"
                        val pins: JsonArray = props["pins"]?.jsonArray ?: JsonArray(emptyList<JsonElement>())

                        _uiState.update { state ->
                            val markers = state.mapMarkers.toMutableList()
                            pins.forEach { pinElem ->
                                val pin = pinElem.jsonObject
                                val pLat = pin["latitude"]?.jsonPrimitive?.doubleOrNull ?: return@forEach
                                val pLon = pin["longitude"]?.jsonPrimitive?.doubleOrNull ?: return@forEach
                                val pTitle = pin["title"]?.jsonPrimitive?.contentOrNull ?: "Art Pin"
                                val pCat = pin["category"]?.jsonPrimitive?.contentOrNull ?: "Art"

                                val existingIndex = markers.indexOfFirst { it.latitude == pLat && it.longitude == pLon }
                                if (existingIndex >= 0) {
                                    markers[existingIndex] = markers[existingIndex].copy(
                                        title = pTitle,
                                        category = pCat
                                    )
                                } else {
                                    markers.add(
                                        MapMarker(
                                            id = (markers.size + 1).toLong(),
                                            latitude = pLat,
                                            longitude = pLon,
                                            category = pCat,
                                            status = "Active",
                                            title = pTitle,
                                            onClick = {}
                                        )
                                    )
                                }
                            }
                            state.copy(
                                mapCenterLat = if (centerLat != 0.0) centerLat else state.mapCenterLat,
                                mapCenterLng = if (centerLon != 0.0) centerLon else state.mapCenterLng,
                                mapZoom = zoom,
                                focusedTitle = overlayTitle,
                                mapMarkers = markers
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun ChatMessage.toUiMessage(targetEngine: A2UIRendererEngine? = engine): TagAlongMessage {
        val payload = a2uiPayloadJson
        val surfaceIds = if (!payload.isNullOrBlank() && targetEngine != null) {
            try {
                targetEngine.processProtocolMessage(payload)
            } catch (_: Exception) {
                emptyList()
            }
        } else emptyList()

        return TagAlongMessage(
            id = id.toString(),
            sender = if (sender == "user") TagAlongSender.USER else TagAlongSender.AGENT,
            text = text.ifBlank { null },
            a2uiPayload = a2uiPayloadJson,
            surfaceIds = surfaceIds,
            photoUri = imagePath,
            timestamp = timestamp
        )
    }

    companion object {
        const val ADK_USER_ID = "tagalong-user"
        private const val DEFAULT_WELCOME_TEXT =
            "Hey! I'm **TagAlong**, your Street Art Companion. Ask me for nearby murals, generate a curated walking tour, test your graffiti style knowledge, or select a mock card below!"

        fun defaultBerlinMarkers(): List<MapMarker> = listOf(
            MapMarker(
                id = 1L,
                latitude = 52.4972,
                longitude = 13.2415,
                category = "Mural",
                status = "Public",
                title = "Cosmic Chameleon (Teufelsberg)",
                isStarred = true,
                onClick = {}
            ),
            MapMarker(
                id = 2L,
                latitude = 52.5251,
                longitude = 13.4021,
                category = "Stencil",
                status = "Public",
                title = "The Cyber Samurai (Mitte)",
                isStarred = false,
                onClick = {}
            ),
            MapMarker(
                id = 3L,
                latitude = 52.5015,
                longitude = 13.4190,
                category = "Mural",
                status = "Public",
                title = "Astronaut Cosmonaut (Kreuzberg)",
                isStarred = true,
                onClick = {}
            )
        )
    }
}
