package net.maiatoday.tagspotter.feature.tagalong.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.adk.kt.runners.Runner
import com.google.adk.kt.types.Blob
import com.google.adk.kt.types.Content
import com.google.adk.kt.types.Part
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.maiatoday.tagspotter.core.a2ui.A2UIAction
import net.maiatoday.tagspotter.core.a2ui.A2UIRendererEngine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
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

class TagAlongViewModel(
    private val runner: Runner? = null,
    private val chatRepository: ChatRepository,
    private val locationProvider: LocationProvider? = null,
    private val hybridSpotRepository: HybridSpotRepository? = null,
    private val photoProcessor: PhotoProcessor? = null,
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
                    var addedBytes = false
                    if (photoProcessor != null) {
                        try {
                            val bytes = photoProcessor.decodeScaledBitmap(currentPhoto, maxDimension = 1024)
                            if (bytes != null && bytes.isNotEmpty()) {
                                parts.add(Part(inlineData = Blob(mimeType = "image/jpeg", data = bytes)))
                                addedBytes = true
                            }
                        } catch (_: Exception) {}
                    }
                    val photoDescriptor = if (addedBytes) {
                        "User attached photo (binary JPEG image included): $currentPhoto"
                    } else {
                        "User attached photo URI: $currentPhoto"
                    }
                    parts.add(Part(text = photoDescriptor))
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
                        val (prose, a2uiPayload) = extractTextAndA2UIPayload(accumulated)
                        val streamSurfaceIds = if (!a2uiPayload.isNullOrBlank() && engine != null) {
                            try {
                                engine!!.processProtocolMessage(a2uiPayload)
                            } catch (_: Exception) {
                                emptyList()
                            }
                        } else emptyList()

                        _uiState.update { state ->
                            val updated = state.messages.map { msg ->
                                if (msg.id == agentMsgId) {
                                    msg.copy(
                                        text = prose.ifBlank { null },
                                        a2uiPayload = a2uiPayload,
                                        surfaceIds = streamSurfaceIds
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

                val (finalProse, finalPayload) = extractTextAndA2UIPayload(accumulated)
                val effectiveText = if (finalProse.isBlank() && finalPayload == null) {
                    "I searched the area, but couldn't find additional details for that request."
                } else {
                    finalProse
                }

                val finalSurfaceIds = if (!finalPayload.isNullOrBlank() && engine != null) {
                    try {
                        engine!!.processProtocolMessage(finalPayload)
                    } catch (_: Exception) {
                        emptyList()
                    }
                } else emptyList()

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
            is A2UIAction.OpenSpotDetail -> {
                val spotTitle = "Spot #${action.spotId}"
                _uiState.update { it.copy(focusedTitle = spotTitle) }
            }
            is A2UIAction.ClientEvent -> {
                if (action.name == "focusMap" || action.name == "viewOnMap") {
                    val lat = (action.context["latitude"] as? Number)?.toDouble() ?: 0.0
                    val lng = (action.context["longitude"] as? Number)?.toDouble() ?: 0.0
                    val title = (action.context["title"] as? String) ?: "Selected Spot"
                    if (lat != 0.0 && lng != 0.0) {
                        _uiState.update { it.copy(mapCenterLat = lat, mapCenterLng = lng, focusedTitle = title) }
                    }
                } else if (action.name == "openSpotDetail") {
                    val spotId = action.context["spotId"]?.toString() ?: ""
                    _uiState.update { it.copy(focusedTitle = "Spot #$spotId") }
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

    private fun ChatMessage.toUiMessage(): TagAlongMessage {
        val payload = a2uiPayloadJson
        val surfaceIds = if (!payload.isNullOrBlank() && engine != null) {
            try {
                engine!!.processProtocolMessage(payload)
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
