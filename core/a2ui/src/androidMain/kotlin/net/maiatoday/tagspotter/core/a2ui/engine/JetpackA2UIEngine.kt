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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.maiatoday.tagspotter.core.a2ui.A2UIAction
import net.maiatoday.tagspotter.core.a2ui.A2UIEngineType
import net.maiatoday.tagspotter.core.a2ui.A2UIRendererEngine

/**
 * Android implementation of [A2UIRendererEngine] backed by official androidx.a2ui runtime & catalog.
 */
class JetpackA2UIEngine(
    context: Context? = null,
    val catalog: A2uiCatalog = createJetpackHybridCatalog(context),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : A2UIRendererEngine {

    override val engineType: A2UIEngineType = A2UIEngineType.ANDROIDX_JETPACK_OFFICIAL

    val processor: androidx.a2ui.model.processor.A2uiMessageProcessor =
        A2uiMessageProcessor(listOf(catalog))

    private val parser = A2uiMessageParser()
    private val _manualActions = MutableSharedFlow<A2UIAction>(extraBufferCapacity = 64)
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

    override fun processProtocolMessage(json: String): List<String> {
        val affectedSurfaces = mutableListOf<String>()
        val parsedElement = try {
            Json.parseToJsonElement(json)
        } catch (e: Exception) {
            return emptyList()
        }

        val envelopes = when (parsedElement) {
            is JsonArray -> parsedElement.filterIsInstance<JsonObject>()
            is JsonObject -> listOf(parsedElement)
            else -> emptyList()
        }

        for (envelope in envelopes) {
            val normalizedEnvelope = if (!envelope.containsKey("version")) {
                buildJsonObject {
                    put("version", "v0.9")
                    for ((k, v) in envelope) {
                        put(k, v)
                    }
                }
            } else {
                envelope
            }

            try {
                val message: A2uiServerToClientMessage = parser.parse(normalizedEnvelope.toString())
                processor.processMessage(message)
                if (!affectedSurfaces.contains(message.surfaceId)) {
                    affectedSurfaces.add(message.surfaceId)
                }
            } catch (_: Exception) {
                // If parser fails on this envelope, continue processing remaining envelopes
            }
        }

        return affectedSurfaces
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
                            val optionId = msg.context["optionId"]?.toString() ?: ""
                            A2UIAction.VoteOption(pollId, optionId)
                        }
                        "answer_quiz" -> {
                            val quizId = msg.context["quizId"]?.toString() ?: ""
                            val optionId = msg.context["optionId"]?.toString() ?: ""
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
