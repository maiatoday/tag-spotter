package net.maiatoday.tagspotter.core.a2ui.catalog

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.maiatoday.tagspotter.core.a2ui.A2UIAction
import net.maiatoday.tagspotter.core.a2ui.model.A2UIComponent
import net.maiatoday.tagspotter.core.a2ui.model.ArtistBioCardData
import net.maiatoday.tagspotter.core.a2ui.model.CommunityVotingData
import net.maiatoday.tagspotter.core.a2ui.model.FallbackComponentData
import net.maiatoday.tagspotter.core.a2ui.model.MapPinOverlayData
import net.maiatoday.tagspotter.core.a2ui.model.SpotCardData
import net.maiatoday.tagspotter.core.a2ui.model.StyleQuizData
import net.maiatoday.tagspotter.core.a2ui.model.TourCardData
import net.maiatoday.tagspotter.core.a2ui.ui.NeonArtistBioCard
import net.maiatoday.tagspotter.core.a2ui.ui.NeonCommunityVotingCard
import net.maiatoday.tagspotter.core.a2ui.ui.NeonFallbackCard
import net.maiatoday.tagspotter.core.a2ui.ui.NeonMapPinOverlayWidget
import net.maiatoday.tagspotter.core.a2ui.ui.NeonSpotCard
import net.maiatoday.tagspotter.core.a2ui.ui.NeonStyleQuizWidget
import net.maiatoday.tagspotter.core.a2ui.ui.NeonTourCard

class NeonA2UICatalog {

    @Composable
    fun RenderComponent(
        component: A2UIComponent,
        onAction: (A2UIAction) -> Unit,
        modifier: Modifier = Modifier
    ) {
        when (component) {
            is SpotCardData -> NeonSpotCard(data = component, onAction = onAction, modifier = modifier)
            is TourCardData -> NeonTourCard(data = component, onAction = onAction, modifier = modifier)
            is ArtistBioCardData -> NeonArtistBioCard(data = component, onAction = onAction, modifier = modifier)
            is StyleQuizData -> NeonStyleQuizWidget(data = component, onAction = onAction, modifier = modifier)
            is CommunityVotingData -> NeonCommunityVotingCard(data = component, onAction = onAction, modifier = modifier)
            is MapPinOverlayData -> NeonMapPinOverlayWidget(data = component, onAction = onAction, modifier = modifier)
            is FallbackComponentData -> NeonFallbackCard(data = component, onAction = onAction, modifier = modifier)
        }
    }
}
