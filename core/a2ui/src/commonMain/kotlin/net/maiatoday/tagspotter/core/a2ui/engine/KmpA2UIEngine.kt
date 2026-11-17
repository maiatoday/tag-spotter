package net.maiatoday.tagspotter.core.a2ui.engine

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.contextable.a2ui4k.agent.A2UiRenderTool
import com.contextable.a2ui4k.data.DataModel
import com.contextable.a2ui4k.model.ActionEvent
import com.contextable.a2ui4k.model.ClientError
import com.contextable.a2ui4k.model.DataChangeEvent
import com.contextable.a2ui4k.model.UiEvent
import com.contextable.a2ui4k.model.ValidationError
import com.contextable.a2ui4k.render.A2UISurface
import com.contextable.a2ui4k.state.SurfaceStateManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import net.maiatoday.tagspotter.core.a2ui.A2UIAction
import net.maiatoday.tagspotter.core.a2ui.A2UIEngineType
import net.maiatoday.tagspotter.core.a2ui.A2UIRendererEngine

/**
 * KMP implementation of [A2UIRendererEngine] backed by com.contextable:a2ui-4k.
 */
class KmpA2UIEngine : A2UIRendererEngine {
    override val engineType: A2UIEngineType = A2UIEngineType.A2UI_KMP_4K

    val stateManager = SurfaceStateManager()
    val catalog = createKmpHybridCatalog()

    private val _outboundEvents = MutableSharedFlow<A2UIAction>(extraBufferCapacity = 64)
    override val outboundEvents: Flow<A2UIAction> = _outboundEvents.asSharedFlow()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    override fun getCatalogSchemaJson(): String {
        return try {
            A2UiRenderTool(stateManager, catalog.id ?: "net.maiatoday.tagspotter.hybrid").parameters.toString()
        } catch (_: Exception) {
            """{"type":"object","catalogId":"${catalog.id ?: "net.maiatoday.tagspotter.hybrid"}"}"""
        }
    }

    override fun clearSurfaces() {
        stateManager.clear()
    }

    override fun processProtocolMessage(json: String): List<String> {
        val affectedSurfaces = mutableListOf<String>()
        try {
            val root = this.json.parseToJsonElement(json)
            when (root) {
                is JsonArray -> {
                    for (elem in root) {
                        if (elem is JsonObject) {
                            val id = processSingleMessage(elem)
                            if (id != null && !affectedSurfaces.contains(id)) {
                                affectedSurfaces.add(id)
                            }
                        }
                    }
                }
                is JsonObject -> {
                    val id = processSingleMessage(root)
                    if (id != null) {
                        affectedSurfaces.add(id)
                    }
                }
                else -> {}
            }
        } catch (e: Exception) {
            _outboundEvents.tryEmit(
                A2UIAction.ClientError(
                    code = "PARSE_ERROR",
                    surfaceId = "",
                    message = e.message ?: "Failed to parse A2UI JSON payload"
                )
            )
        }
        return affectedSurfaces
    }

    private fun processSingleMessage(obj: JsonObject): String? {
        val surfaceId = extractSurfaceId(obj)
        val normalized = normalizeMessageForKmp(obj)
        stateManager.processMessage(normalized)
        return surfaceId
    }

    private fun normalizeMessageForKmp(obj: JsonObject): JsonObject {
        val hasVersion = obj.containsKey("version")
        var current = if (!hasVersion) {
            buildJsonObject {
                put("version", "v0.9")
                for ((k, v) in obj) {
                    put(k, v)
                }
            }
        } else {
            obj
        }

        if (current.containsKey("createSurface")) {
            val cs = current["createSurface"]?.jsonObject
            if (cs != null && !cs.containsKey("rootComponentId")) {
                val normalizedCs = buildJsonObject {
                    for ((k, v) in cs) {
                        put(k, v)
                    }
                    put("rootComponentId", "root")
                }
                current = buildJsonObject {
                    for ((k, v) in current) {
                        if (k == "createSurface") {
                            put("createSurface", normalizedCs)
                        } else {
                            put(k, v)
                        }
                    }
                }
            }
        }

        if (current.containsKey("updateComponents")) {
            val uc = current["updateComponents"]?.jsonObject
            val componentsArray = uc?.get("components")?.jsonArray
            if (uc != null && componentsArray != null) {
                val normalizedComponents = JsonArray(componentsArray.map { elem ->
                    if (elem is JsonObject) {
                        val compType = elem["component"]?.jsonPrimitive?.contentOrNull
                            ?: elem["type"]?.jsonPrimitive?.contentOrNull
                            ?: "Box"
                        val props = elem["properties"]?.jsonObject

                        buildJsonObject {
                            for ((k, v) in elem) {
                                put(k, v)
                            }
                            put("component", compType)
                            if (props != null) {
                                for ((pk, pv) in props) {
                                    put(pk, pv)
                                }
                            }
                        }
                    } else {
                        elem
                    }
                })

                val normalizedUc = buildJsonObject {
                    for ((k, v) in uc) {
                        if (k == "components") {
                            put("components", normalizedComponents)
                        } else {
                            put(k, v)
                        }
                    }
                }

                current = buildJsonObject {
                    for ((k, v) in current) {
                        if (k == "updateComponents") {
                            put("updateComponents", normalizedUc)
                        } else {
                            put(k, v)
                        }
                    }
                }
            }
        }

        return current
    }

    private fun extractSurfaceId(obj: JsonObject): String? {
        return obj["createSurface"]?.jsonObject?.get("surfaceId")?.jsonPrimitive?.contentOrNull
            ?: obj["updateComponents"]?.jsonObject?.get("surfaceId")?.jsonPrimitive?.contentOrNull
            ?: obj["updateDataModel"]?.jsonObject?.get("surfaceId")?.jsonPrimitive?.contentOrNull
            ?: obj["deleteSurface"]?.jsonObject?.get("surfaceId")?.jsonPrimitive?.contentOrNull
            ?: obj["surfaceId"]?.jsonPrimitive?.contentOrNull
    }

    override fun updateDataModel(surfaceId: String, path: String, value: Any?) {
        try {
            val model = stateManager.getDataModel(surfaceId)
            model?.update(path, value.toJsonElement())
        } catch (e: Exception) {
            _outboundEvents.tryEmit(
                A2UIAction.ClientError(
                    code = "DATA_MODEL_UPDATE_ERROR",
                    surfaceId = surfaceId,
                    message = e.message ?: "Failed to update data model at $path"
                )
            )
        }
    }

    @Composable
    override fun RenderSurface(surfaceId: String, modifier: Modifier) {
        val uiDef = stateManager.getSurface(surfaceId)
        val dataModel = stateManager.getDataModel(surfaceId) ?: remember(surfaceId) { DataModel() }
        if (uiDef != null) {
            A2UISurface(
                definition = uiDef,
                modifier = modifier,
                dataModel = dataModel,
                catalog = catalog,
                onEvent = { event ->
                    val action = mapUiEventToAction(event, surfaceId)
                    if (action != null) {
                        _outboundEvents.tryEmit(action)
                    }
                }
            )
        } else {
            Box(modifier = modifier)
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
            val uiDef = stateManager.getSurface(targetSurfaceId)
            val dataModel = stateManager.getDataModel(targetSurfaceId) ?: remember(targetSurfaceId) { DataModel() }
            if (uiDef != null) {
                A2UISurface(
                    definition = uiDef,
                    modifier = modifier,
                    dataModel = dataModel,
                    catalog = catalog,
                    onEvent = { event ->
                        val action = mapUiEventToAction(event, targetSurfaceId)
                        if (action != null) {
                            onAction(action)
                            _outboundEvents.tryEmit(action)
                        }
                    }
                )
            } else {
                Box(modifier = modifier)
            }
        } else {
            Box(modifier = modifier)
        }
    }

    private fun mapUiEventToAction(event: UiEvent, defaultSurfaceId: String): A2UIAction? {
        val effectiveSurfaceId = if (event.surfaceId.isNotBlank()) event.surfaceId else defaultSurfaceId
        return when (event) {
            is ActionEvent -> {
                val ctx = event.context ?: JsonObject(emptyMap())
                when (event.name) {
                    "view_on_map" -> {
                        val lat = ctx["latitude"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                        val lon = ctx["longitude"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                        val title = ctx["title"]?.jsonPrimitive?.contentOrNull ?: ""
                        val spotId = ctx["spotId"]?.jsonPrimitive?.contentOrNull
                        A2UIAction.ViewOnMap(lat, lon, title, spotId)
                    }
                    "toggle_star" -> {
                        val spotId = ctx["spotId"]?.jsonPrimitive?.contentOrNull ?: ""
                        A2UIAction.StarSpot(spotId)
                    }
                    "navigate_to_stop" -> {
                        val stopIndex = ctx["stopIndex"]?.jsonPrimitive?.intOrNull ?: 0
                        val lat = ctx["latitude"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                        val lon = ctx["longitude"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                        val title = ctx["title"]?.jsonPrimitive?.contentOrNull ?: ""
                        A2UIAction.NavigateToStop(stopIndex, lat, lon, title)
                    }
                    "open_spot_detail" -> {
                        val spotId = ctx["spotId"]?.jsonPrimitive?.contentOrNull ?: ""
                        A2UIAction.OpenSpotDetail(spotId)
                    }
                    "answer_quiz" -> {
                        val quizId = ctx["quizId"]?.jsonPrimitive?.contentOrNull ?: ""
                        val optionId = ctx["optionId"]?.jsonPrimitive?.contentOrNull ?: ""
                        val isCorrect = ctx["isCorrect"]?.jsonPrimitive?.booleanOrNull ?: false
                        A2UIAction.AnswerQuiz(quizId, optionId, isCorrect)
                    }
                    "vote_option" -> {
                        val pollId = ctx["pollId"]?.jsonPrimitive?.contentOrNull ?: ""
                        val optionId = ctx["optionId"]?.jsonPrimitive?.contentOrNull ?: ""
                        A2UIAction.VoteOption(pollId, optionId)
                    }
                    else -> {
                        val map = mutableMapOf<String, Any?>()
                        for ((k, v) in ctx) {
                            map[k] = v.jsonPrimitive.contentOrNull
                        }
                        A2UIAction.ClientEvent(name = event.name, surfaceId = effectiveSurfaceId, context = map)
                    }
                }
            }
            is ValidationError -> {
                A2UIAction.ClientError(
                    code = "VALIDATION_ERROR",
                    surfaceId = effectiveSurfaceId,
                    message = event.message
                )
            }
            is ClientError -> {
                A2UIAction.ClientError(
                    code = event.code,
                    surfaceId = effectiveSurfaceId,
                    message = event.message
                )
            }
            is DataChangeEvent -> null
        }
    }
}

internal fun Any?.toJsonElement(): JsonElement {
    return when (this) {
        null -> JsonNull
        is JsonElement -> this
        is Boolean -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is String -> JsonPrimitive(this)
        is Map<*, *> -> {
            val content = this.entries.associate { (k, v) ->
                k.toString() to v.toJsonElement()
            }
            JsonObject(content)
        }
        is Iterable<*> -> {
            JsonArray(this.map { it.toJsonElement() })
        }
        else -> JsonPrimitive(this.toString())
    }
}
