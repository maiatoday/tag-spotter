package net.maiatoday.tagspotter.core.a2ui.engine

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import net.maiatoday.tagspotter.core.a2ui.A2UIAction
import net.maiatoday.tagspotter.core.a2ui.A2UIEngineType
import net.maiatoday.tagspotter.core.a2ui.A2UIRendererEngine
import net.maiatoday.tagspotter.core.a2ui.catalog.NeonA2UICatalog
import net.maiatoday.tagspotter.core.a2ui.parser.A2UIPayloadParser

class KmpA2UIEngine : A2UIRendererEngine {
    override val engineType: A2UIEngineType = A2UIEngineType.A2UI_KMP_BINARYTAPE

    @Composable
    override fun RenderSurface(
        payloadJson: String,
        catalog: NeonA2UICatalog,
        onAction: (A2UIAction) -> Unit,
        modifier: Modifier
    ) {
        val component = remember(payloadJson) {
            A2UIPayloadParser.parse(payloadJson)
        }
        catalog.RenderComponent(
            component = component,
            onAction = onAction,
            modifier = modifier
        )
    }
}
