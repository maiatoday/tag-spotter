package net.maiatoday.tagspotter.core.agent

import com.google.adk.kt.models.LlmRequest
import com.google.adk.kt.models.LlmResponse
import com.google.adk.kt.models.Model
import com.google.adk.kt.types.Content
import com.google.adk.kt.types.Part
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RunnerCallTest {

    private class FakeModel(private val responseText: String) : Model {
        override val name: String = "fake-model"
        override fun generateContent(request: LlmRequest, stream: Boolean): Flow<LlmResponse> {
            val content = Content(
                role = "model",
                parts = listOf(Part(text = responseText))
            )
            return flowOf(LlmResponse(content = content))
        }
    }

    @Test
    fun testRunnerExecution() = runTest {
        val agent = TagAlongAgentFactory.createAgent(FakeModel("Here is a mural: ```json\n{\"type\":\"SpotCard\",\"data\":{\"title\":\"Test\"}}\n```"))
        val runner = TagAlongAgentFactory.createRunner(agent)
        assertNotNull(runner)

        val events = runner.runAsync(
            "test-user",
            "test-session",
            "inv-1",
            Content(role = "user", parts = listOf(Part(text = "Hello!"))),
            emptyMap()
        ).toList()

        assertTrue(events.isNotEmpty())
        val texts = events.mapNotNull { it.content?.parts?.mapNotNull { p -> p.text }?.joinToString("") }
        assertTrue(texts.any { it.contains("Here is a mural") })
    }
}
