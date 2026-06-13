package net.maiatoday.tagspotter.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun OsmMapView(
    latitude: Double,
    longitude: Double,
    zoomLevel: Double,
    markers: List<OsmMarker>,
    useDarkMap: Boolean,
    modifier: Modifier,
    radiusCircleCenterLatitude: Double?,
    radiusCircleCenterLongitude: Double?,
    radiusCircleMeters: Double,
    onMapClick: ((Double, Double) -> Unit)?,
    onMapReady: (() -> Unit)?
) {
    Box(modifier = modifier) {
        Text("Map at: $latitude, $longitude")
    }
}
