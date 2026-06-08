package net.maiatoday.tagspotter.core.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.google.ai.client.generativeai.type.Schema
import com.google.ai.client.generativeai.type.FunctionType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.maiatoday.tagspotter.core.photo.PhotoProcessor

@Serializable
internal data class WikiSuggestion(
    val url: String? = null
)

class AndroidAiRecognitionService(
    private val photoProcessor: PhotoProcessor
) : AiRecognitionService {

    override suspend fun identifyArtist(imagePath: String, apiKey: String, category: String): AiSuggestion {
        // 1. Load and downscale image using PhotoProcessor
        val bitmap = photoProcessor.decodeScaledBitmap(imagePath, 1024)
            ?: throw IllegalArgumentException("Failed to load image.")

        // 2. Initialize Gemini with responseSchema
        val model = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = apiKey,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = Schema(
                    name = "AiSuggestion",
                    description = "Artist suggestion details",
                    type = FunctionType.OBJECT,
                    properties = mapOf(
                        "artist" to Schema(
                            name = "artist",
                            description = "Name of the artist or creator if known, or null if unknown",
                            nullable = true,
                            type = FunctionType.STRING
                        ),
                        "title" to Schema(
                            name = "title",
                            description = "Suggested title for the spot/art, or null if unknown",
                            nullable = true,
                            type = FunctionType.STRING
                        ),
                        "tags" to Schema(
                            name = "tags",
                            description = "List of tag suggestions",
                            type = FunctionType.ARRAY,
                            items = Schema(
                                name = "tag",
                                description = "tag name",
                                type = FunctionType.STRING
                            )
                        )
                    ),
                    required = listOf("artist", "title", "tags")
                )
            }
        )

        val artistRoleDescription = when (category) {
            "sculpture" -> "sculptor, artist, designer, or creator"
            "architecture" -> "architect, designer, or builder"
            "nature" -> "landscape artist, gardener, designer, or photographer"
            "public_place" -> "artist, sculptor, architect, designer, or creator"
            "food" -> "chef, cook, food artist, or creator"
            else -> "street art artist, graffiti writer, crew, or painter"
        }

        val prompt = """
            Analyze this image of a spot in the category: "$category".
            Identify the $artistRoleDescription (if known), suggest a title for the art/spot, and suggest tags (from: mural, stencil, throwup, pasteup, sticker, or others appropriate for this category).
            ${if (category == "nature") "Specifically, since the category is \"nature\", for the \"title\" field try to identify the specific plant, flower, tree species, or geological/natural feature visible in the image." else ""}
            Return the response in strict JSON format using exactly these keys:
            {
              "artist": "Name or null",
              "title": "Suggested Title or null",
              "tags": ["tag1", "tag2"]
            }
            If you do not know the artist/creator/architect, set the "artist" field to null. If you cannot suggest a title, set the "title" field to null.
        """.trimIndent()

        val response = model.generateContent(
            content {
                image(bitmap)
                text(prompt)
            }
        )

        val responseText = response.text ?: ""
        if (responseText.isEmpty()) {
            throw IllegalStateException("Empty response from AI model.")
        }

        return Json.decodeFromString<AiSuggestion>(responseText.trim())
    }

    override suspend fun searchWikipediaForSpot(title: String, apiKey: String): String? {
        // Initialize Gemini with responseSchema
        val model = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = apiKey,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = Schema(
                    name = "WikiSuggestion",
                    description = "Wikipedia page URL suggestion",
                    type = FunctionType.OBJECT,
                    properties = mapOf(
                        "url" to Schema(
                            name = "url",
                            description = "Most relevant Wikipedia page URL, or null if none exists",
                            nullable = true,
                            type = FunctionType.STRING
                        )
                    ),
                    required = listOf("url")
                )
            }
        )

        val prompt = """
            You are an assistant that finds the most relevant, official Wikipedia page URL for a given subject.
            Subject: "$title"
            
            Find the Wikipedia page for this subject.
            If a relevant Wikipedia page exists, return the URL formatted as a markdown link: [Anchor Text](URL), where Anchor Text is a concise, relevant title for the page (e.g. "Banksy - Wikipedia").
            If no relevant page exists on Wikipedia, return null.
            
            Return the response in strict JSON format using exactly this schema:
            {
              "url": "[Anchor Text](https://en.wikipedia.org/wiki/...)"
            }
            If no page is found, set "url" to null.
        """.trimIndent()

        val response = model.generateContent(prompt)
        val responseText = response.text ?: ""
        if (responseText.isEmpty()) {
            throw IllegalStateException("Empty response from AI model.")
        }

        val suggestion = Json.decodeFromString<WikiSuggestion>(responseText.trim())
        return suggestion.url
    }
}
