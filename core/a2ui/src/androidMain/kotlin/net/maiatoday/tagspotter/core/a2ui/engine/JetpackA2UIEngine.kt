package net.maiatoday.tagspotter.core.a2ui.engine

import android.content.Context
import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.compose.ui.toJsonSchemaString
import androidx.a2ui.model.protocol.A2uiClientErrorMessage
import androidx.a2ui.model.protocol.A2uiClientEventMessage
import androidx.a2ui.model.protocol.A2uiDeleteSurfaceMessage
import androidx.a2ui.model.protocol.A2uiServerToClientMessage
import androidx.a2ui.model.protocol.A2uiUpdateDataModelMessage
import androidx.compose.material3.a2ui.A2uiSurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import net.maiatoday.tagspotter.core.a2ui.A2UIAction
import net.maiatoday.tagspotter.core.a2ui.A2UIEngineType
import net.maiatoday.tagspotter.core.a2ui.A2UIRendererEngine

/**
 * Android implementation of [A2UIRendererEngine] backed by official androidx.a2ui runtime & catalog.
 */
class JetpackA2UIEngine(
    context: Context? = null,
    catalog: A2uiCatalog? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : A2UIRendererEngine {

    override val engineType: A2UIEngineType = A2UIEngineType.ANDROIDX_JETPACK_OFFICIAL

    private val _manualActions = MutableSharedFlow<A2UIAction>(extraBufferCapacity = 64)

    private val onFocusMapCallback: (Double, Double, String?) -> Unit = { lat, lon, title ->
        emitAction(A2UIAction.ViewOnMap(lat, lon, title ?: ""))
    }
    private val onOpenSpotDetailCallback: (String) -> Unit = { spotId ->
        emitAction(A2UIAction.OpenSpotDetail(spotId))
    }

    val catalog: A2uiCatalog = catalog ?: createJetpackHybridCatalog(
        context = context,
        catalogId = JETPACK_HYBRID_CATALOG_ID,
        onFocusMap = onFocusMapCallback,
        onOpenSpotDetail = onOpenSpotDetailCallback
    )

    val processor: androidx.a2ui.model.processor.A2uiMessageProcessor =
        A2uiMessageProcessor(
            listOf(
                this.catalog,
                createJetpackHybridCatalog(
                    context = context,
                    catalogId = JETPACK_HYBRID_CATALOG_URI,
                    onFocusMap = onFocusMapCallback,
                    onOpenSpotDetail = onOpenSpotDetailCallback
                )
            )
        )

    private val parser = A2uiMessageParser()
    private var loopJob: Job? = null

    init {
        ensureRenderLoopStarted()
    }

    private fun ensureRenderLoopStarted() {
        if (loopJob == null || loopJob?.isActive != true) {
            loopJob = scope.launch {
                processor.collectMessages()
            }
        }
    }

    override fun getCatalogSchemaJson(): String = catalog.toJsonSchemaString()

    override suspend fun runRenderLoop() {
        ensureRenderLoopStarted()
        loopJob?.join()
    }

    override fun clearSurfaces() {
        val surfaces = processor.activeSurfaces.value
        for (surface in surfaces) {
            processor.processMessage(A2uiDeleteSurfaceMessage(surface.id))
        }
    }

    private fun logError(message: String, throwable: Throwable? = null) {
        try {
            android.util.Log.e("JetpackA2UIEngine", message, throwable)
        } catch (_: Throwable) {
            System.err.println("JetpackA2UIEngine: $message: ${throwable?.message}")
            throwable?.printStackTrace(System.err)
        }
    }

    override fun processProtocolMessage(json: String): List<String> {
        val affectedSurfaces = mutableListOf<String>()
        val parsedElement = try {
            Json.parseToJsonElement(json)
        } catch (e: Exception) {
            logError("Failed to parse JSON element", e)
            emitAction(
                A2UIAction.ClientError(
                    code = "PARSE_ERROR",
                    surfaceId = "",
                    message = e.message ?: "Failed to parse A2UI JSON payload"
                )
            )
            return emptyList()
        }

        val envelopes = when (parsedElement) {
            is JsonArray -> parsedElement.filterIsInstance<JsonObject>()
            is JsonObject -> listOf(parsedElement)
            else -> emptyList()
        }

        for (envelope in envelopes) {
            val normalizedEnvelope = normalizeEnvelope(envelope)

            try {
                val message: A2uiServerToClientMessage = parser.parse(normalizedEnvelope.toString())
                if (normalizedEnvelope.containsKey("createSurface")) {
                    if (processor.activeSurfaces.value.any { it.id == message.surfaceId }) {
                        processor.processMessage(A2uiDeleteSurfaceMessage(message.surfaceId))
                    }
                }
                processor.processMessage(message)
                if (!affectedSurfaces.contains(message.surfaceId)) {
                    affectedSurfaces.add(message.surfaceId)
                }
            } catch (e: Exception) {
                logError("Failed to parse/process envelope: $normalizedEnvelope", e)
                emitAction(
                    A2UIAction.ClientError(
                        code = "PARSE_ERROR",
                        surfaceId = "",
                        message = e.message ?: "Failed to parse/process A2UI envelope"
                    )
                )
            }
        }

        return affectedSurfaces
    }

    private fun normalizeEnvelope(envelope: JsonObject): JsonObject {
        return buildJsonObject {
            val version = envelope["version"] ?: JsonPrimitive("v0.9")
            put("version", version)

            for ((key, value) in envelope) {
                if (key == "version") continue

                when (key) {
                    "createSurface" -> {
                        if (value is JsonObject) {
                            put("createSurface", buildJsonObject {
                                for ((k, v) in value) {
                                    put(k, v)
                                }
                                if (!value.containsKey("catalogId")) {
                                    put("catalogId", JsonPrimitive(JETPACK_HYBRID_CATALOG_ID))
                                }
                            })
                        } else {
                            put(key, value)
                        }
                    }
                    "updateDataModel" -> {
                        if (value is JsonObject) {
                            put("updateDataModel", buildJsonObject {
                                for ((k, v) in value) {
                                    if (k == "data" && !value.containsKey("value")) {
                                        put("value", v)
                                    } else {
                                        put(k, v)
                                    }
                                }
                                if (!value.containsKey("path")) {
                                    put("path", JsonPrimitive("/"))
                                }
                            })
                        } else {
                            put(key, value)
                        }
                    }
                    "updateComponents" -> {
                        if (value is JsonObject) {
                            put("updateComponents", buildJsonObject {
                                for ((k, v) in value) {
                                    if (k == "components" && v is JsonArray) {
                                        put("components", buildJsonArray {
                                            for (comp in v) {
                                                if (comp is JsonObject) {
                                                    add(normalizeComponent(comp))
                                                } else {
                                                    add(comp)
                                                }
                                            }
                                        })
                                    } else {
                                        put(k, v)
                                    }
                                }
                            })
                        } else {
                            put(key, value)
                        }
                    }
                    else -> put(key, value)
                }
            }
        }
    }

    private fun normalizeComponent(comp: JsonObject): JsonObject {
        val rawType = (comp["component"] ?: comp["type"])?.jsonPrimitive?.contentOrNull
        val normalizedType = rawType?.let { normalizeComponentType(it) }
        val mergedProps = linkedMapOf<String, JsonElement>()

        // Flatten properties if present
        val props = comp["properties"]
        if (props is JsonObject) {
            for ((pk, pv) in props) {
                mergedProps[pk] = pv
            }
        }

        // Copy any top-level properties other than id, component, type, properties
        for ((ck, cv) in comp) {
            if (ck != "id" && ck != "component" && ck != "type" && ck != "properties") {
                mergedProps[ck] = cv
            }
        }

        return buildJsonObject {
            val id = comp["id"]
            if (id != null) put("id", id)
            if (normalizedType != null) {
                put("component", JsonPrimitive(normalizedType))
            } else {
                val componentType = comp["component"] ?: comp["type"]
                if (componentType != null) put("component", componentType)
            }

            for ((pk, pv) in mergedProps) {
                put(pk, normalizeComponentProperty(normalizedType, pk, pv))
            }
        }
    }

    private fun normalizeComponentType(type: String): String = when (type) {
        "MapPinOverlayWidget", "NeonMapPinOverlayWidget" -> "MapPinOverlay"
        else -> type
    }

    private fun normalizeComponentProperty(
        componentType: String?,
        key: String,
        value: JsonElement
    ): JsonElement {
        return when {
            componentType == "Text" && key == "variant" && value is JsonPrimitive && value.isString -> {
                JsonPrimitive(value.content.lowercase())
            }
            componentType == "ChoicePicker" && key == "options" && value is JsonArray -> {
                buildJsonArray {
                    for (item in value) {
                        if (item is JsonObject) {
                            add(buildJsonObject {
                                for ((ok, ov) in item) {
                                    if (ok == "id" && !item.containsKey("value")) {
                                        put("value", ov)
                                    } else {
                                        put(ok, ov)
                                    }
                                }
                            })
                        } else {
                            add(item)
                        }
                    }
                }
            }
            componentType == "Button" && key == "action" && value is JsonObject -> {
                normalizeAction(value)
            }
            else -> value
        }
    }

    private fun normalizeAction(action: JsonObject): JsonObject {
        val fnCall = action["functionCall"] as? JsonObject ?: return action
        val callName = fnCall["call"]?.jsonPrimitive?.contentOrNull
        if (callName != "clientEvent") return action
        val args = fnCall["args"] as? JsonObject ?: return action
        val eventName = args["name"]?.jsonPrimitive?.contentOrNull ?: return action
        return buildJsonObject {
            put("event", buildJsonObject {
                put("name", JsonPrimitive(eventName))
                put("context", buildJsonObject {
                    for ((k, v) in args) {
                        if (k != "name") {
                            put(k, v)
                        }
                    }
                })
            })
        }
    }

    override fun updateDataModel(surfaceId: String, path: String, value: Any?) {
        processor.processMessage(
            A2uiUpdateDataModelMessage(
                surfaceId = surfaceId,
                path = path,
                value = value
            )
        )
    }

    override val outboundEvents: Flow<A2UIAction> = merge(
        processor.outboundEvents.map { msg ->
            when (msg) {
                is A2uiClientEventMessage -> {
                    when (msg.type) {
                        "toggle_star", "star_spot" -> {
                            val spotId = msg.context["spotId"]?.toString() ?: ""
                            A2UIAction.StarSpot(spotId)
                        }
                        "view_on_map" -> {
                            val lat = (msg.context["latitude"] as? Number)?.toDouble() ?: 0.0
                            val lon = (msg.context["longitude"] as? Number)?.toDouble() ?: 0.0
                            val title = msg.context["title"]?.toString() ?: ""
                            val spotId = msg.context["spotId"]?.toString()
                            A2UIAction.ViewOnMap(lat, lon, title, spotId)
                        }
                        "open_spot_detail" -> {
                            val spotId = msg.context["spotId"]?.toString() ?: ""
                            A2UIAction.OpenSpotDetail(spotId)
                        }
                        "navigate_to_stop" -> {
                            val stopIndex = (msg.context["stopIndex"] as? Number)?.toInt() ?: 0
                            val lat = (msg.context["latitude"] as? Number)?.toDouble() ?: 0.0
                            val lon = (msg.context["longitude"] as? Number)?.toDouble() ?: 0.0
                            val title = msg.context["title"]?.toString() ?: ""
                            A2UIAction.NavigateToStop(stopIndex, lat, lon, title)
                        }
                        "vote_option" -> {
                            val pollId = msg.context["pollId"]?.toString() ?: ""
                            val rawOption = msg.context["optionId"] ?: msg.context["selectedOption"]
                            val optionId = when (rawOption) {
                                is List<*> -> rawOption.firstOrNull()?.toString() ?: ""
                                else -> rawOption?.toString() ?: ""
                            }
                            A2UIAction.VoteOption(pollId, optionId)
                        }
                        "answer_quiz" -> {
                            val quizId = msg.context["quizId"]?.toString() ?: ""
                            val rawOption = msg.context["optionId"] ?: msg.context["selectedOption"]
                            val optionId = when (rawOption) {
                                is List<*> -> rawOption.firstOrNull()?.toString() ?: ""
                                else -> rawOption?.toString() ?: ""
                            }
                            val isCorrect = (msg.context["isCorrect"] as? Boolean) ?: false
                            A2UIAction.AnswerQuiz(quizId, optionId, isCorrect)
                        }
                        else -> {
                            A2UIAction.ClientEvent(
                                name = msg.type,
                                surfaceId = msg.surfaceId,
                                context = msg.context
                            )
                        }
                    }
                }
                is A2uiClientErrorMessage -> {
                    A2UIAction.ClientError(
                        code = msg.code,
                        surfaceId = msg.surfaceId,
                        message = msg.message,
                        context = msg.context
                    )
                }
            }
        },
        _manualActions.asSharedFlow()
    )

    fun emitAction(action: A2UIAction) {
        _manualActions.tryEmit(action)
    }

    @Composable
    override fun RenderSurface(
        surfaceId: String,
        modifier: Modifier
    ) {
        val surfaces by processor.activeSurfaces.collectAsState()
        val surface = surfaces.find { it.id == surfaceId }
        if (surface != null) {
            A2uiSurface(
                surfaceModel = surface,
                modifier = modifier
            )
        }
    }

    @Composable
    override fun RenderSurface(
        payloadJson: String,
        onAction: (A2UIAction) -> Unit,
        modifier: Modifier
    ) {
        val surfaceIds = remember(payloadJson) {
            processProtocolMessage(payloadJson)
        }
        val targetSurfaceId = surfaceIds.firstOrNull()
        if (targetSurfaceId != null) {
            RenderSurface(surfaceId = targetSurfaceId, modifier = modifier)
        }
    }
}
