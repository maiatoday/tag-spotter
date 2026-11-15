package net.maiatoday.tagspotter.core.agent

import com.google.adk.kt.agents.Instruction
import com.google.adk.kt.agents.LlmAgent
import com.google.adk.kt.models.Model
import com.google.adk.kt.runners.InMemoryRunner
import com.google.adk.kt.runners.Runner
import com.google.adk.kt.tools.BaseTool
import com.google.adk.kt.types.GenerateContentConfig

object TagAlongAgentFactory {
    const val AGENT_NAME = "TagAlongAgent"
    const val DEFAULT_MODEL_NAME = "gemini-2.5-flash"

    fun createAgent(
        model: Model,
        tools: List<BaseTool> = emptyList(),
        systemInstruction: String = TagAlongPrompts.SYSTEM_INSTRUCTION
    ): LlmAgent {
        return LlmAgent(
            name = AGENT_NAME,
            model = model,
            description = "Urban street art and walking tour companion",
            instruction = Instruction(systemInstruction),
            tools = tools,
            generateContentConfig = GenerateContentConfig(
                temperature = 0.7f
            )
        )
    }

    fun createRunner(agent: LlmAgent): Runner {
        return InMemoryRunner(agent = agent, appName = "TagAlongRunner")
    }

    fun createRunner(
        model: Model,
        streetArtTools: StreetArtTools,
        systemInstruction: String = TagAlongPrompts.SYSTEM_INSTRUCTION
    ): Runner {
        val agent = createAgent(
            model = model,
            tools = streetArtTools.toTools(),
            systemInstruction = systemInstruction
        )
        return createRunner(agent)
    }
}
