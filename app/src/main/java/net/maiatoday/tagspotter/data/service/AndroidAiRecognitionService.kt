package net.maiatoday.tagspotter.data.service

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.maiatoday.tagspotter.domain.AiRecognitionService
import net.maiatoday.tagspotter.domain.AiSuggestion
import net.maiatoday.tagspotter.domain.PhotoProcessor

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

        // 2. Initialize Gemini
        val model = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = apiKey
        )

        val artistRoleDescription = when (category) {
            "sculpture" -> "sculptor, artist, designer, or creator"
            "architecture" -> "architect, designer, or builder"
            "nature" -> "landscape artist, gardener, designer, or photographer"
            "public_place" -> "artist, sculptor, architect, designer, or creator"
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
            Do not add markdown formatting or backticks around the JSON. Return only the raw JSON.
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

        // Clean the JSON string (in case markdown backticks were returned)
        val cleanJson = if (responseText.contains("```")) {
            responseText
                .substringAfter("```json")
                .substringAfter("```")
                .substringBefore("```")
                .trim()
        } else {
            responseText.trim()
        }

        return Json.decodeFromString<AiSuggestion>(cleanJson)
    }

    override suspend fun searchWikipediaForSpot(title: String, apiKey: String): String? {
        // Initialize Gemini
        val model = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = apiKey
        )

        val prompt = """
            You are an assistant that finds the most relevant, official Wikipedia page URL for a given subject.
            Subject: "$title"
            
            Find the Wikipedia page for this subject.
            If a relevant Wikipedia page exists, return the URL.
            If no relevant page exists on Wikipedia, return null.
            
            Return the response in strict JSON format using exactly this schema:
            {
              "url": "https://en.wikipedia.org/wiki/..." 
            }
            If no page is found, set "url" to null.
            Do not include any markdown styling, backticks, or extra text. Return only the raw JSON.
        """.trimIndent()

        val response = model.generateContent(prompt)
        val responseText = response.text ?: ""
        if (responseText.isEmpty()) {
            throw IllegalStateException("Empty response from AI model.")
        }

        // Clean the JSON string (in case markdown backticks were returned)
        val cleanJson = if (responseText.contains("```")) {
            responseText
                .substringAfter("```json")
                .substringAfter("```")
                .substringBefore("```")
                .trim()
        } else {
            responseText.trim()
        }

        val suggestion = Json.decodeFromString<WikiSuggestion>(cleanJson)
        return suggestion.url
    }
}
