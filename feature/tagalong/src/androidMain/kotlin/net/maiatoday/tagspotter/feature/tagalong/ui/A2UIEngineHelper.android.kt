package net.maiatoday.tagspotter.feature.tagalong.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import net.maiatoday.tagspotter.core.a2ui.A2UIRendererEngine
import net.maiatoday.tagspotter.core.a2ui.engine.JetpackA2UIEngine
import net.maiatoday.tagspotter.core.a2ui.engine.KmpA2UIEngine

@Composable
actual fun rememberAvailableEngines(): List<A2UIRendererEngine> {
    val context = LocalContext.current
    val jetpackEngine = remember(context) { JetpackA2UIEngine(context) }
    val kmpEngine = remember { KmpA2UIEngine() }
    return remember(jetpackEngine, kmpEngine) {
        listOf(jetpackEngine, kmpEngine)
    }
}
