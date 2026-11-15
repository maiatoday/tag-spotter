package net.maiatoday.tagspotter.core.a2ui.parser

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.maiatoday.tagspotter.core.a2ui.model.A2UIComponent
import net.maiatoday.tagspotter.core.a2ui.model.ArtistBioCardData
import net.maiatoday.tagspotter.core.a2ui.model.CommunityVotingData
import net.maiatoday.tagspotter.core.a2ui.model.FallbackComponentData
import net.maiatoday.tagspotter.core.a2ui.model.MapPinOverlayData
import net.maiatoday.tagspotter.core.a2ui.model.SpotCardData
import net.maiatoday.tagspotter.core.a2ui.model.StyleQuizData
import net.maiatoday.tagspotter.core.a2ui.model.TourCardData

object A2UIPayloadParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun parse(rawPayload: String): A2UIComponent {
        val cleanedJson = extractJson(rawPayload)
        return try {
            val rootElement = json.parseToJsonElement(cleanedJson).jsonObject
            val componentType = resolveComponentType(rootElement)

            val payloadObject = if (rootElement.containsKey("data") && rootElement["data"] is JsonObject) {
                rootElement["data"]!!.jsonObject
            } else {
                rootElement
            }

            val normalizedType = componentType.lowercase().replace("_", "").replace("-", "")
            when (normalizedType) {
                "spotcard" -> json.decodeFromJsonElement<SpotCardData>(payloadObject)
                "tourcard" -> json.decodeFromJsonElement<TourCardData>(payloadObject)
                "artistbiocard" -> json.decodeFromJsonElement<ArtistBioCardData>(payloadObject)
                "stylequizwidget", "stylequiz" -> json.decodeFromJsonElement<StyleQuizData>(payloadObject)
                "communityvotingcard", "communityvoting" -> json.decodeFromJsonElement<CommunityVotingData>(payloadObject)
                "mappinoverlaywidget", "mappinoverlay" -> json.decodeFromJsonElement<MapPinOverlayData>(payloadObject)
                else -> FallbackComponentData(
                    title = "Unrecognized A2UI Component: $componentType",
                    description = "The agent produced component type '$componentType' which is not in the Neon Urban Suite catalog.",
                    rawJson = rawPayload
                )
            }
        } catch (e: Exception) {
            FallbackComponentData(
                title = "A2UI Parsing Error",
                description = e.message ?: "Failed to parse JSON payload.",
                rawJson = rawPayload
            )
        }
    }

    fun extractTextAndA2UIPayload(fullText: String): Pair<String, String?> {
        val jsonStart = fullText.indexOf("```json")
        if (jsonStart != -1) {
            val jsonEnd = fullText.indexOf("```", jsonStart + 7)
            if (jsonEnd != -1) {
                val preText = fullText.substring(0, jsonStart).trim()
                val postText = fullText.substring(jsonEnd + 3).trim()
                val jsonPayload = fullText.substring(jsonStart + 7, jsonEnd).trim()
                val combinedText = listOf(preText, postText).filter { it.isNotBlank() }.joinToString("\n\n")
                return combinedText to jsonPayload
            }
        }
        val firstBrace = fullText.indexOf('{')
        val lastBrace = fullText.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace > firstBrace) {
            val candidate = fullText.substring(firstBrace, lastBrace + 1)
            if (candidate.contains("\"type\"") || candidate.contains("\"component\"")) {
                val preText = fullText.substring(0, firstBrace).trim()
                val postText = fullText.substring(lastBrace + 1).trim()
                val combinedText = listOf(preText, postText).filter { it.isNotBlank() }.joinToString("\n\n")
                return combinedText to candidate
            }
        }
        return fullText.trim() to null
    }

    private fun resolveComponentType(root: JsonObject): String {
        return root["type"]?.jsonPrimitive?.content
            ?: root["component"]?.jsonPrimitive?.content
            ?: root["componentType"]?.jsonPrimitive?.content
            ?: root["name"]?.jsonPrimitive?.content
            ?: ""
    }

    private fun extractJson(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.startsWith("```json")) {
            val end = trimmed.lastIndexOf("```")
            if (end > 7) {
                return trimmed.substring(7, end).trim()
            }
        } else if (trimmed.startsWith("```")) {
            val end = trimmed.lastIndexOf("```")
            if (end > 3) {
                return trimmed.substring(3, end).trim()
            }
        }

        val firstBrace = trimmed.indexOf('{')
        val lastBrace = trimmed.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1)
        }

        return trimmed
    }
}
