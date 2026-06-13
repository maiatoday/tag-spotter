package net.maiatoday.tagspotter.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class OsmMarker(
    val id: Long,
    val latitude: Double,
    val longitude: Double,
    val category: String,
    val status: String,
    val title: String,
    val isStarred: Boolean = false,
    val onClick: () -> Unit
)

@Composable
expect fun OsmMapView(
    latitude: Double,
    longitude: Double,
    zoomLevel: Double,
    markers: List<OsmMarker>,
    useDarkMap: Boolean,
    modifier: Modifier = Modifier,
    radiusCircleCenterLatitude: Double? = null,
    radiusCircleCenterLongitude: Double? = null,
    radiusCircleMeters: Double = 0.0,
    onMapClick: ((Double, Double) -> Unit)? = null,
    onMapReady: (() -> Unit)? = null
)
