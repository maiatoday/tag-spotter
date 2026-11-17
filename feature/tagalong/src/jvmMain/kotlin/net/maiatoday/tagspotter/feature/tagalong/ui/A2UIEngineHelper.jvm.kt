package net.maiatoday.tagspotter.feature.tagalong.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import net.maiatoday.tagspotter.core.a2ui.A2UIRendererEngine
import net.maiatoday.tagspotter.core.a2ui.engine.KmpA2UIEngine

@Composable
actual fun rememberAvailableEngines(): List<A2UIRendererEngine> {
    val kmpEngine = remember { KmpA2UIEngine() }
    return remember(kmpEngine) { listOf(kmpEngine) }
}
