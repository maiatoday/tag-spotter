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

    override suspend fun identifyArtist(
        imagePath: String,
        apiKey: String,
        category: String,
        currentArtist: String?,
        currentTitle: String?,
        thumbnailPath: String?
    ): AiSuggestion {
        // 1. Load and downscale image using PhotoProcessor
        var bitmap = photoProcessor.decodeScaledBitmap(imagePath, 1024)
        if (bitmap == null && !thumbnailPath.isNullOrEmpty()) {
            bitmap = photoProcessor.decodeScaledBitmap(thumbnailPath, 1024)
        }
        if (bitmap == null) {
            throw IllegalArgumentException("Failed to load image.")
        }

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

        val tagSuggestions = when (category) {
            "sculpture" -> "statue, bronze, marble, installation, public-art, classical, modern"
            "architecture" -> "building, bridge, historical, modern, skyscraper, facade, monument"
            "nature" -> "tree, flower, plant, forest, geological, park, trail, botanical"
            "public_place" -> "plaza, park, square, fountain, playground, street-furniture, monument"
            "food" -> "cafe, restaurant, bakery, street-food, dessert, cuisine, specialty"
            else -> "mural, stencil, throwup, pasteup, sticker, wildstyle, tags"
        }

        val specificInstructions = when (category) {
            "nature" -> """
                - For the "title" field, try to identify the specific plant, flower, tree species, or geological/natural feature visible in the image (e.g. "Coast Redwood", "Monstera Deliciosa", "Granite Boulder").
                - For the "artist" field, set to null unless there is a known landscape designer, gardener, or photographer who created/documented it.
            """.trimIndent()
            "food" -> """
                - For the "title" field, identify the dish, baked good, coffee, or specialty food item (e.g. "Croissant", "Sourdough", "Cappuccino").
                - For the "artist" field, identify the chef, baker, barista, or restaurant name (e.g. "Chef Gordon Ramsay", "Duomo Bakery").
            """.trimIndent()
            "architecture" -> """
                - For the "title" field, identify the name of the building or structural landmark (e.g. "Flatiron Building", "Gothic Archway").
                - For the "artist" field, identify the architect, designer, or architectural firm (e.g. "Frank Lloyd Wright").
            """.trimIndent()
            "sculpture" -> """
                - For the "title" field, identify the title of the statue, monument, or public art installation (e.g. "The Thinker").
                - For the "artist" field, identify the sculptor or designer (e.g. "Auguste Rodin").
            """.trimIndent()
            "public_place" -> """
                - For the "title" field, identify the name of the plaza, park, street, or public space (e.g. "Duomo Plaza", "High Line Park").
                - For the "artist" field, identify the urban planner, designer, or city department if known, otherwise set to null.
            """.trimIndent()
            else -> """
                - For the "title" field, suggest a title for the graffiti, mural, or street artwork.
                - For the "artist" field, identify the street artist, graffiti writer, crew, or painter (e.g. "Banksy", "1UP Crew").
            """.trimIndent()
        }

        val knownInfoPrompt = buildString {
            if (!currentArtist.isNullOrBlank()) {
                append("- Already known creator/artist/architect/chef: \"$currentArtist\"\n")
            }
            if (!currentTitle.isNullOrBlank()) {
                append("- Already known title/description: \"$currentTitle\"\n")
            }
        }

        val prompt = """
            Analyze this image of a spot in the category: "$category".
            Identify the $artistRoleDescription (if known), suggest a title, and suggest tags.
            
            ${if (knownInfoPrompt.isNotEmpty()) "We already have some information about this spot:\n$knownInfoPrompt\nUse this information as context to improve your identification or confirm it." else ""}
            
            Suggested tags for this category include (but are not limited to): $tagSuggestions.
            
            Specifically:
            $specificInstructions
            
            Return the response in strict JSON format using exactly these keys:
            {
              "artist": "Name or null",
              "title": "Suggested Title or null",
              "tags": ["tag1", "tag2"]
            }
            If you do not know the creator/artist/architect/chef, set the "artist" field to null. If you cannot suggest a title, set the "title" field to null.
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

    override suspend fun searchWikipediaForSpot(title: String, category: String, artists: List<String>, apiKey: String): String? {
        // Initialize Gemini with responseSchema
        val model = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = apiKey,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = Schema(
                    name = "WikiSuggestion",
                    description = "Wikipedia page or artist website URL suggestion",
                    type = FunctionType.OBJECT,
                    properties = mapOf(
                        "url" to Schema(
                            name = "url",
                            description = "Most relevant Wikipedia page URL or official artist/creator/architect/chef website URL, or null if none exists",
                            nullable = true,
                            type = FunctionType.STRING
                        )
                    ),
                    required = listOf("url")
                )
            }
        )

        val categoryContext = when (category) {
            "nature" -> "This is a natural feature (e.g. plant, flower, tree species, park, geological formation). Find the Wikipedia page for the species, genus, common name, or location."
            "food" -> "This is a food place, dish, or specialty food item. Find the Wikipedia page for the dish, style, chef, or notable restaurant."
            "architecture" -> "This is a building, monument, or architectural structure. Find the Wikipedia page for the building or the architect."
            "sculpture" -> "This is a sculpture or public art installation. Find the Wikipedia page for the sculpture or the sculptor."
            "graffiti" -> "This is a graffiti or street art piece. Find the Wikipedia page for the artwork or the street artist/crew."
            else -> "This is a spot/landmark."
        }

        val prompt = """
            You are an assistant that finds the most relevant, official Wikipedia page URL or official artist/creator website URL for a given subject.
            
            We are looking for a Wikipedia page or official website related to the following spot:
            - Title/Description: "$title"
            - Category: "$category"
            ${if (artists.isNotEmpty()) "- Creator/Artist/Architect/Chef: \"${artists.joinToString(", ")}\"" else ""}
            
            Category Context: $categoryContext
            
            Instructions:
            1. Find the Wikipedia page for this subject.
            2. If the creator/artist is known, you should also look for their official website or portfolio.
            3. If a direct Wikipedia page exists for the specific spot/artwork/building/species, you can return that Wikipedia page.
            4. If no specific Wikipedia page exists for the spot, or if the artist's official website is more relevant or informative, return the artist's official website URL instead.
            
            Format requirements:
            - Return the URL formatted as a markdown link: [website name](url), where "website name" is a concise, relevant title/description for the page or site (e.g. "Frank Gehry - Wikipedia" or "Banksy Official Website").
            - If no relevant page or website exists, return null.
            
            Return the response in strict JSON format using exactly this schema:
            {
              "url": "[website name](url)"
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
