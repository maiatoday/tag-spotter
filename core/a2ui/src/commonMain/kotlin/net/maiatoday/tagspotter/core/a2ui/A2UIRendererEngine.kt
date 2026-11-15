package net.maiatoday.tagspotter.core.a2ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.maiatoday.tagspotter.core.a2ui.catalog.NeonA2UICatalog

enum class A2UIEngineType {
    A2UI_KMP_BINARYTAPE,
    ANDROIDX_JETPACK_OFFICIAL
}

interface A2UIRendererEngine {
    val engineType: A2UIEngineType

    @Composable
    fun RenderSurface(
        payloadJson: String,
        catalog: NeonA2UICatalog,
        onAction: (A2UIAction) -> Unit,
        modifier: Modifier
    )
}
