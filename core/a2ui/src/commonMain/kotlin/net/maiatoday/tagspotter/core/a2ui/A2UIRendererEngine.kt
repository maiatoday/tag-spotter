package net.maiatoday.tagspotter.core.a2ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.Flow

enum class A2UIEngineType {
    A2UI_KMP_4K,
    ANDROIDX_JETPACK_OFFICIAL
}

interface A2UIRendererEngine {
    val engineType: A2UIEngineType

    /** Returns the JSON Schema (Draft 2020-12) describing all registered catalog components & functions. */
    fun getCatalogSchemaJson(): String

    /** Starts the engine's message collection loop (suspends while active; required by androidx.a2ui). */
    suspend fun runRenderLoop() {}

    /** Clears all active surfaces (called when starting a New Conversation or switching sessions). */
    fun clearSurfaces() {}

    /** Feeds an inbound A2UI v0.9 JSON message or array of messages, returning any created/updated surfaceIds. */
    fun processProtocolMessage(json: String): List<String>

    /** Unified helper accepting raw JSON payload. */
    fun processMessage(jsonPayload: String) {
        processProtocolMessage(jsonPayload)
    }

    /** Mutates a JSON Pointer path in the surface's reactive data model directly. */
    fun updateDataModel(surfaceId: String, path: String, value: Any?)

    /** Outbound stream of client events and validation errors emitted by the engine. */
    val outboundEvents: Flow<A2UIAction>

    /** Renders the active A2UI surface for [surfaceId] inline in the chat list. */
    @Composable
    fun RenderSurface(
        surfaceId: String,
        modifier: Modifier = Modifier
    )

    /** Backward-compatible surface rendering for transition convenience. */
    @Composable
    fun RenderSurface(
        payloadJson: String,
        onAction: (A2UIAction) -> Unit = {},
        modifier: Modifier = Modifier
    )
}
