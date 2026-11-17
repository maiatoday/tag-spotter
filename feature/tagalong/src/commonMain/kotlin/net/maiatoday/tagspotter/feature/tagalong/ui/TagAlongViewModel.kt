package net.maiatoday.tagspotter.feature.tagalong.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.adk.kt.runners.Runner
import com.google.adk.kt.types.Content
import com.google.adk.kt.types.Part
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.maiatoday.tagspotter.core.a2ui.A2UIAction
import net.maiatoday.tagspotter.core.a2ui.A2UIRendererEngine
import net.maiatoday.tagspotter.core.a2ui.model.MapPinOverlayData
import net.maiatoday.tagspotter.core.a2ui.model.SpotCardData
import net.maiatoday.tagspotter.core.a2ui.model.TourCardData
import net.maiatoday.tagspotter.core.a2ui.parser.A2UIPayloadParser
import net.maiatoday.tagspotter.core.database.ChatMessage
import net.maiatoday.tagspotter.core.database.ChatRepository
import net.maiatoday.tagspotter.core.database.ChatSession
import net.maiatoday.tagspotter.core.database.epochMillis
import net.maiatoday.tagspotter.core.location.LocationProvider
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

class TagAlongViewModel(
    private val runner: Runner? = null,
    private val chatRepository: ChatRepository,
    private val locationProvider: LocationProvider? = null,
    private val hybridSpotRepository: HybridSpotRepository? = null,
    private val initialSessionId: String = "session-default"
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TagAlongUiState(
            sessionId = initialSessionId,
            mapMarkers = defaultBerlinMarkers()
        )
    )
    val uiState: StateFlow<TagAlongUiState> = _uiState.asStateFlow()

    var engine: A2UIRendererEngine? = null

    val activeSessionId: String
        get() = _uiState.value.sessionId

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
            val welcomeText = "Hey! I'm **TagAlong**, your Street Art Companion. Ask me for nearby murals, generate a curated walking tour, test your graffiti style knowledge, or select a mock card below!"
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

    init {
        viewModelScope.launch {
            loadSessionMessages()
            initMapFromSources()
        }
    }

    private suspend fun loadSessionMessages() {
        val existing = chatRepository.getMessagesListForSession(initialSessionId)
        if (existing.isNotEmpty()) {
            _uiState.update { it.copy(messages = existing.map { m -> m.toUiMessage() }) }
        } else {
            chatRepository.createOrUpdateSession(
                ChatSession(
                    sessionId = initialSessionId,
                    title = "Street Art Exploration",
                    createdAt = epochMillis(),
                    lastActiveAt = epochMillis()
                )
            )
            val welcomeText = "Hey! I'm **TagAlong**, your Street Art Companion. Ask me for nearby murals, generate a curated walking tour, test your graffiti style knowledge, or select a mock card below!"
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

        viewModelScope.launch {
            chatRepository.insertMessage(
                ChatMessage(
                    sessionId = _uiState.value.sessionId,
                    sender = "user",
                    text = trimmed,
                    imagePath = currentPhoto,
                    timestamp = userTimestamp
                )
            )
        }

        if (runner == null) {
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
                chatRepository.insertMessage(
                    ChatMessage(
                        sessionId = _uiState.value.sessionId,
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
            var accumulated = ""
            try {
                val parts = mutableListOf<Part>()
                if (!currentPhoto.isNullOrBlank()) {
                    parts.add(Part(text = "User attached photo URI: $currentPhoto"))
                }
                if (trimmed.isNotBlank()) {
                    parts.add(Part(text = trimmed))
                }
                val content = Content(role = "user", parts = parts)

                runner.runAsync(
                    "tagalong-user",
                    _uiState.value.sessionId,
                    "inv-${epochMillis()}",
                    content,
                    emptyMap()
                ).collect { event ->
                    val chunk = event.content?.parts?.mapNotNull { it.text }?.joinToString("") ?: ""
                    if (chunk.isNotEmpty()) {
                        accumulated += chunk
                        val (prose, a2uiPayload) = A2UIPayloadParser.extractTextAndA2UIPayload(accumulated)
                        _uiState.update { state ->
                            val updated = state.messages.map { msg ->
                                if (msg.id == agentMsgId) {
                                    msg.copy(
                                        text = prose.ifBlank { null },
                                        a2uiPayload = a2uiPayload
                                    )
                                } else msg
                            }
                            state.copy(messages = updated)
                        }
                        if (a2uiPayload != null) {
                            syncMapWithA2UI(a2uiPayload)
                        }
                    }
                }

                val (finalProse, finalPayload) = A2UIPayloadParser.extractTextAndA2UIPayload(accumulated)
                val effectiveText = if (finalProse.isBlank() && finalPayload == null) {
                    "I searched the area, but couldn't find additional details for that request."
                } else {
                    finalProse
                }

                _uiState.update { state ->
                    val updated = state.messages.map { msg ->
                        if (msg.id == agentMsgId) {
                            msg.copy(
                                text = effectiveText.ifBlank { null },
                                a2uiPayload = finalPayload
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
        val msg = TagAlongMessage(
            id = fixtureMsgId,
            sender = TagAlongSender.AGENT,
            text = introText,
            a2uiPayload = fixtureJson,
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
                if (action.latitude != 0.0 && action.longitude != 0.0) {
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
            }
            is A2UIAction.AnswerQuiz -> {
                val feedbackText = if (action.isCorrect) {
                    "🎯 Spot on! That's the correct technique."
                } else {
                    "❌ Not quite! Check the explanation on the card."
                }
                val agentMsg = TagAlongMessage(
                    id = "msg-${epochMillis()}",
                    sender = TagAlongSender.AGENT,
                    text = feedbackText,
                    timestamp = epochMillis()
                )
                _uiState.update { it.copy(messages = it.messages + agentMsg) }
                viewModelScope.launch {
                    chatRepository.insertMessage(
                        ChatMessage(
                            sessionId = _uiState.value.sessionId,
                            sender = "agent",
                            text = feedbackText,
                            timestamp = epochMillis()
                        )
                    )
                }
            }
            is A2UIAction.VoteOption -> {
                val voteText = "🗳️ Vote recorded for '${action.optionId}'! Thanks for curating the city's street art."
                val agentMsg = TagAlongMessage(
                    id = "msg-${epochMillis()}",
                    sender = TagAlongSender.AGENT,
                    text = voteText,
                    timestamp = epochMillis()
                )
                _uiState.update { it.copy(messages = it.messages + agentMsg) }
                viewModelScope.launch {
                    chatRepository.insertMessage(
                        ChatMessage(
                            sessionId = _uiState.value.sessionId,
                            sender = "agent",
                            text = voteText,
                            timestamp = epochMillis()
                        )
                    )
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
            is A2UIAction.Custom -> {
                // Custom action hook
            }
        }
    }

    private fun syncMapWithA2UI(a2uiJson: String) {
        try {
            when (val component = A2UIPayloadParser.parse(a2uiJson)) {
                is SpotCardData -> {
                    if (component.latitude != null && component.longitude != null) {
                        val lat = component.latitude!!
                        val lon = component.longitude!!
                        _uiState.update { state ->
                            val markers = state.mapMarkers.toMutableList()
                            val existingIndex = markers.indexOfFirst { it.latitude == lat && it.longitude == lon }
                            if (existingIndex >= 0) {
                                markers[existingIndex] = markers[existingIndex].copy(
                                    title = component.title,
                                    isStarred = component.isStarred
                                )
                            } else {
                                markers.add(
                                    MapMarker(
                                        id = (markers.size + 1).toLong(),
                                        latitude = lat,
                                        longitude = lon,
                                        category = "Mural",
                                        status = "Public",
                                        title = component.title,
                                        isStarred = component.isStarred,
                                        onClick = {}
                                    )
                                )
                            }
                            state.copy(
                                mapCenterLat = lat,
                                mapCenterLng = lon,
                                focusedTitle = component.title,
                                mapMarkers = markers
                            )
                        }
                    }
                }
                is TourCardData -> {
                    if (component.waypoints.isNotEmpty()) {
                        val first = component.waypoints.first()
                        _uiState.update { state ->
                            val markers = state.mapMarkers.toMutableList()
                            component.waypoints.forEach { wp ->
                                val existingIndex = markers.indexOfFirst { it.latitude == wp.latitude && it.longitude == wp.longitude }
                                if (existingIndex >= 0) {
                                    markers[existingIndex] = markers[existingIndex].copy(
                                        title = wp.title,
                                        category = "Tour Stop #${wp.index}",
                                        status = if (wp.completed) "Completed" else "Upcoming"
                                    )
                                } else {
                                    markers.add(
                                        MapMarker(
                                            id = (markers.size + 1).toLong(),
                                            latitude = wp.latitude,
                                            longitude = wp.longitude,
                                            category = "Tour Stop #${wp.index}",
                                            status = if (wp.completed) "Completed" else "Upcoming",
                                            title = wp.title,
                                            onClick = {}
                                        )
                                    )
                                }
                            }
                            state.copy(
                                mapCenterLat = first.latitude,
                                mapCenterLng = first.longitude,
                                focusedTitle = "${component.tourTitle} (Stop #1: ${first.title})",
                                mapMarkers = markers
                            )
                        }
                    }
                }
                is MapPinOverlayData -> {
                    _uiState.update { state ->
                        val markers = state.mapMarkers.toMutableList()
                        component.pins.forEach { pin ->
                            val existingIndex = markers.indexOfFirst { it.latitude == pin.latitude && it.longitude == pin.longitude }
                            if (existingIndex >= 0) {
                                markers[existingIndex] = markers[existingIndex].copy(
                                    title = pin.title,
                                    category = pin.category ?: markers[existingIndex].category
                                )
                            } else {
                                markers.add(
                                    MapMarker(
                                        id = (markers.size + 1).toLong(),
                                        latitude = pin.latitude,
                                        longitude = pin.longitude,
                                        category = pin.category ?: "Art",
                                        status = "Active",
                                        title = pin.title,
                                        onClick = {}
                                    )
                                )
                            }
                        }
                        state.copy(
                            mapCenterLat = component.centerLatitude,
                            mapCenterLng = component.centerLongitude,
                            mapZoom = component.zoomLevel.toDouble(),
                            focusedTitle = component.title,
                            mapMarkers = markers
                        )
                    }
                }
                else -> {}
            }
        } catch (_: Exception) {}
    }

    private fun ChatMessage.toUiMessage(): TagAlongMessage {
        return TagAlongMessage(
            id = id.toString(),
            sender = if (sender == "user") TagAlongSender.USER else TagAlongSender.AGENT,
            text = text.ifBlank { null },
            a2uiPayload = a2uiPayloadJson,
            photoUri = imagePath,
            timestamp = timestamp
        )
    }

    companion object {
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
