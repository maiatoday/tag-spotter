package net.maiatoday.tagspotter.core.agent

import com.google.adk.firebase.models.Firebase
import com.google.adk.kt.runners.Runner
import com.google.adk.kt.tools.BaseTool
import com.google.firebase.Firebase as FirebaseApp
import com.google.firebase.ai.ai
import net.maiatoday.tagspotter.core.location.LocationProvider
import net.maiatoday.tagspotter.core.sync.HybridSpotRepository

actual fun StreetArtTools.toTools(): List<BaseTool> = generatedTools()

fun createAndroidFirebaseModel(
    modelName: String = TagAlongAgentFactory.DEFAULT_MODEL_NAME
): Firebase {
    return Firebase.create(modelName, FirebaseApp.ai)
}

fun createAndroidTagAlongRunner(
    hybridSpotRepository: HybridSpotRepository,
    locationProvider: LocationProvider? = null,
    modelName: String = TagAlongAgentFactory.DEFAULT_MODEL_NAME,
    systemInstruction: String = TagAlongPrompts.SYSTEM_INSTRUCTION
): Runner {
    val model = createAndroidFirebaseModel(modelName)
    val tools = StreetArtTools(hybridSpotRepository, locationProvider)
    return TagAlongAgentFactory.createRunner(
        model = model,
        streetArtTools = tools,
        systemInstruction = systemInstruction
    )
}
