package net.maiatoday.tagspotter.core.agent

import com.google.adk.kt.models.Gemini
import com.google.adk.kt.runners.Runner
import com.google.adk.kt.tools.BaseTool
import com.google.genai.Client
import net.maiatoday.tagspotter.core.location.LocationProvider
import net.maiatoday.tagspotter.core.sync.HybridSpotRepository

actual fun StreetArtTools.toTools(): List<BaseTool> = generatedTools()

fun createDesktopGeminiModel(
    apiKey: String,
    modelName: String = TagAlongAgentFactory.DEFAULT_MODEL_NAME
): Gemini {
    val client = Client.builder().apiKey(apiKey).build()
    return Gemini(client = client, name = modelName)
}

fun createDesktopTagAlongRunner(
    apiKey: String,
    hybridSpotRepository: HybridSpotRepository,
    locationProvider: LocationProvider? = null,
    catalogSchemaJson: String? = null,
    modelName: String = TagAlongAgentFactory.DEFAULT_MODEL_NAME,
    systemInstruction: String = TagAlongPrompts.buildSystemInstruction(catalogSchemaJson)
): Runner {
    val model = createDesktopGeminiModel(apiKey, modelName)
    val tools = StreetArtTools(hybridSpotRepository, locationProvider)
    return TagAlongAgentFactory.createRunner(
        model = model,
        streetArtTools = tools,
        catalogSchemaJson = catalogSchemaJson,
        systemInstruction = systemInstruction
    )
}
