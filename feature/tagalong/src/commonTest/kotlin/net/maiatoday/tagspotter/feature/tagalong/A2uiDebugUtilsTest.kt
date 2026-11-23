package net.maiatoday.tagspotter.feature.tagalong

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.maiatoday.tagspotter.core.a2ui.A2UIAction
import net.maiatoday.tagspotter.core.a2ui.A2UIEngineType
import net.maiatoday.tagspotter.core.a2ui.A2UIRendererEngine
import net.maiatoday.tagspotter.core.database.FakeChatRepository
import net.maiatoday.tagspotter.feature.tagalong.ui.TagAlongViewModel
import net.maiatoday.tagspotter.feature.tagalong.ui.extractSurfaceJsonForDebug
import net.maiatoday.tagspotter.feature.tagalong.ui.formatA2uiJsonForDebug
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class A2uiDebugUtilsTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeSchemaEngine(
        private val schemaJson: String = """{"type":"object","catalogId":"test.catalog"}"""
    ) : A2UIRendererEngine {
        var schemaReadCount = 0
        override val engineType: A2UIEngineType = A2UIEngineType.A2UI_KMP_4K
        override val outboundEvents: Flow<A2UIAction> = emptyFlow()

        override fun getCatalogSchemaJson(): String {
            schemaReadCount++
            return schemaJson
        }

        override fun processProtocolMessage(json: String): List<String> = emptyList()
        override fun updateDataModel(surfaceId: String, path: String, value: Any?) {}

        @Composable
        override fun RenderSurface(surfaceId: String, modifier: Modifier) {}

        @Composable
        override fun RenderSurface(payloadJson: String, onAction: (A2UIAction) -> Unit, modifier: Modifier) {}
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun formatA2uiJsonForDebug_prettyPrintsValidJson() {
        val compact = """{"version":"v0.9","createSurface":{"surfaceId":"s1"}}"""
        val formatted = formatA2uiJsonForDebug(compact)

        assertTrue(formatted.contains("\n"), "Expected formatted JSON to contain newlines")
        assertTrue(formatted.contains("\"surfaceId\": \"s1\""))
    }

    @Test
    fun formatA2uiJsonForDebug_fallsBackToRawOnInvalidJson() {
        val malformed = """{"version": "v0.9", broken"""
        val formatted = formatA2uiJsonForDebug(malformed)

        assertEquals(malformed, formatted)
    }

    @Test
    fun extractSurfaceJsonForDebug_filtersEnvelopesForMultiSurfacePayload() {
        val multiSurface = """
            [
              {"version":"v0.9","createSurface":{"surfaceId":"s1"}},
              {"version":"v0.9","updateComponents":{"surfaceId":"s1","components":[]}},
              {"version":"v0.9","createSurface":{"surfaceId":"s2"}},
              {"version":"v0.9","updateComponents":{"surfaceId":"s2","components":[]}}
            ]
        """.trimIndent()

        val s1Json = extractSurfaceJsonForDebug(multiSurface, "s1")
        assertTrue(s1Json.contains("\"surfaceId\": \"s1\""))
        assertFalse(s1Json.contains("\"surfaceId\": \"s2\""))

        val s2Json = extractSurfaceJsonForDebug(multiSurface, "s2")
        assertTrue(s2Json.contains("\"surfaceId\": \"s2\""))
        assertFalse(s2Json.contains("\"surfaceId\": \"s1\""))
    }

    @Test
    fun toggleA2uiDebugMode_togglesStateAndLogsSchemaWhenEnabled() = runTest(testDispatcher) {
        val fakeEngine = FakeSchemaEngine()
        val viewModel = TagAlongViewModel(
            runner = null,
            chatRepository = FakeChatRepository(),
            initialSessionId = "debug-test-session"
        )
        advanceUntilIdle()

        viewModel.attachEngine(fakeEngine)
        assertFalse(viewModel.uiState.value.isA2uiDebugMode)
        assertEquals(0, fakeEngine.schemaReadCount)

        // Toggle ON -> should read/log schema
        viewModel.toggleA2uiDebugMode()
        assertTrue(viewModel.uiState.value.isA2uiDebugMode)
        assertEquals(1, fakeEngine.schemaReadCount)

        // Toggle OFF -> should not log schema again
        viewModel.toggleA2uiDebugMode()
        assertFalse(viewModel.uiState.value.isA2uiDebugMode)
        assertEquals(1, fakeEngine.schemaReadCount)
    }

    @Test
    fun switchEngineWhileDebugModeOn_logsNewEngineSchema() = runTest(testDispatcher) {
        val engine1 = FakeSchemaEngine("""{"catalogId":"engine-1"}""")
        val engine2 = FakeSchemaEngine("""{"catalogId":"engine-2"}""")
        val viewModel = TagAlongViewModel(
            runner = null,
            chatRepository = FakeChatRepository(),
            initialSessionId = "debug-switch-session"
        )
        advanceUntilIdle()

        viewModel.attachEngine(engine1)
        viewModel.toggleA2uiDebugMode()
        assertEquals(1, engine1.schemaReadCount)
        assertEquals(0, engine2.schemaReadCount)

        viewModel.switchEngine(engine2)
        assertEquals(1, engine2.schemaReadCount)

        val logged = viewModel.logActiveA2uiSchema()
        assertNotNull(logged)
        assertTrue(logged.contains("\"catalogId\": \"engine-2\""))
    }

    @Test
    fun logActiveA2uiSchema_returnsNullWhenNoEngineAttached() = runTest(testDispatcher) {
        val viewModel = TagAlongViewModel(
            runner = null,
            chatRepository = FakeChatRepository(),
            initialSessionId = "debug-no-engine"
        )
        advanceUntilIdle()

        assertNull(viewModel.logActiveA2uiSchema())
    }
}
