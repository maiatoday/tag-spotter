package net.maiatoday.tagspotter.feature.tagalong.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.maiatoday.tagspotter.core.a2ui.A2UIRendererEngine

internal const val A2UI_DEBUG_LOG_TAG = "TagAlongA2UI"

private val debugPrettyJson = Json {
    prettyPrint = true
    isLenient = true
    ignoreUnknownKeys = true
}

/**
 * Platform debug logger for A2UI payloads and schemas.
 * On Android this chunks output to avoid Logcat's ~4000 char truncation limit.
 */
expect fun logA2uiDebug(tag: String, message: String)

/**
 * Pretty-prints a raw JSON string for readable debug inspection.
 * Falls back to the original string if the payload is not valid JSON.
 */
fun formatA2uiJsonForDebug(rawJson: String): String {
    val trimmed = rawJson.trim()
    if (trimmed.isEmpty()) return trimmed
    return try {
        val element = debugPrettyJson.parseToJsonElement(trimmed)
        debugPrettyJson.encodeToString(JsonElement.serializer(), element)
    } catch (_: Exception) {
        trimmed
    }
}

/**
 * Extracts and pretty-prints the envelopes belonging to [surfaceId] when [rawPayload]
 * contains multiple surfaces. Falls back to the full pretty-printed payload otherwise.
 */
fun extractSurfaceJsonForDebug(rawPayload: String, surfaceId: String): String {
    val trimmed = rawPayload.trim()
    if (trimmed.isEmpty()) return trimmed
    return try {
        val element = debugPrettyJson.parseToJsonElement(trimmed)
        if (element is JsonArray) {
            val matching = element.filter { item ->
                item is JsonObject && extractEnvelopeSurfaceId(item) == surfaceId
            }
            if (matching.isNotEmpty() && matching.size < element.size) {
                return debugPrettyJson.encodeToString(
                    JsonElement.serializer(),
                    JsonArray(matching)
                )
            }
        }
        debugPrettyJson.encodeToString(JsonElement.serializer(), element)
    } catch (_: Exception) {
        trimmed
    }
}

private fun extractEnvelopeSurfaceId(obj: JsonObject): String? {
    return obj["createSurface"]?.jsonObject?.get("surfaceId")?.jsonPrimitive?.contentOrNull
        ?: obj["updateComponents"]?.jsonObject?.get("surfaceId")?.jsonPrimitive?.contentOrNull
        ?: obj["updateDataModel"]?.jsonObject?.get("surfaceId")?.jsonPrimitive?.contentOrNull
        ?: obj["deleteSurface"]?.jsonObject?.get("surfaceId")?.jsonPrimitive?.contentOrNull
        ?: obj["surfaceId"]?.jsonPrimitive?.contentOrNull
}

/**
 * Logs the active A2UI catalog schema for [engine] to the platform debug log and returns
 * the formatted schema string.
 */
fun logA2uiSchema(engine: A2UIRendererEngine): String {
    val formatted = formatA2uiJsonForDebug(engine.getCatalogSchemaJson())
    logA2uiDebug(
        tag = A2UI_DEBUG_LOG_TAG,
        message = "=== Active A2UI Catalog Schema (${engine.engineType.name}) ===\n$formatted\n=== End A2UI Catalog Schema ==="
    )
    return formatted
}

@Composable
internal fun A2uiDebugBanner(
    engineTypeName: String,
    onViewSchema: () -> Unit,
    onLogSchema: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "A2UI DEBUG · $engineTypeName",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    maxLines = 1
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onLogSchema,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = "Log Schema",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                    )
                }

                OutlinedButton(
                    onClick = onViewSchema,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiaryContainer),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = "View Schema",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
internal fun A2uiDebugJsonCard(
    label: String,
    rawJson: String,
    modifier: Modifier = Modifier
) {
    val formattedJson = remember(rawJson) { formatA2uiJsonForDebug(rawJson) }
    var expanded by remember(rawJson) { mutableStateOf(true) }
    val clipboardManager = LocalClipboardManager.current

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "A2UI JSON",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { clipboardManager.setText(AnnotatedString(formattedJson)) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy A2UI JSON",
                            tint = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse JSON" else "Expand JSON",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (expanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    SelectionContainer {
                        Text(
                            text = formattedJson,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun A2uiSchemaDialog(
    engineTypeName: String,
    schemaJson: String,
    onLogSchema: () -> Unit,
    onDismiss: () -> Unit
) {
    val formattedSchema = remember(schemaJson) { formatA2uiJsonForDebug(schemaJson) }
    val clipboardManager = LocalClipboardManager.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "A2UI CATALOG SCHEMA",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "Engine: $engineTypeName",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { clipboardManager.setText(AnnotatedString(formattedSchema)) }) {
                            Text("Copy")
                        }
                        TextButton(onClick = onLogSchema) {
                            Text("Log")
                        }
                        TextButton(onClick = onDismiss) {
                            Text("Close")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 460.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                        .padding(10.dp)
                ) {
                    SelectionContainer {
                        Text(
                            text = formattedSchema,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
