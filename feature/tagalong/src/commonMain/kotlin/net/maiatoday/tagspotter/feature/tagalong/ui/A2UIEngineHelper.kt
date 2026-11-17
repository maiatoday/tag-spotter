package net.maiatoday.tagspotter.feature.tagalong.ui

import androidx.compose.runtime.Composable
import net.maiatoday.tagspotter.core.a2ui.A2UIRendererEngine

@Composable
expect fun rememberAvailableEngines(): List<A2UIRendererEngine>
