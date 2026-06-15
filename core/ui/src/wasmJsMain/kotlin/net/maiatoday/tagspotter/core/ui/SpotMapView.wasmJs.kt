package net.maiatoday.tagspotter.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import kotlinx.browser.window

@Composable
actual fun SpotMapView(
    latitude: Double,
    longitude: Double,
    zoomLevel: Double,
    markers: List<MapMarker>,
    useDarkMap: Boolean,
    modifier: Modifier,
    radiusCircleCenterLatitude: Double?,
    radiusCircleCenterLongitude: Double?,
    radiusCircleMeters: Double,
    onMapClick: ((Double, Double) -> Unit)?,
    onMapReady: (() -> Unit)?
) {
    // Construct Yandex Static Map URL
    val markerListStr = markers.joinToString("~") { "${it.longitude},${it.latitude},pm2rdm" }
    val staticMapUrl = buildString {
        append("https://static-maps.yandex.ru/1.x/?ll=")
        append(longitude)
        append(",")
        append(latitude)
        append("&z=")
        append(zoomLevel.toInt().coerceIn(0, 17))
        append("&l=map&size=600,450")
        if (markerListStr.isNotEmpty()) {
            append("&pt=")
            append(markerListStr)
        }
    }

    Box(
        modifier = modifier.clickable {
            val osmUrl = "https://www.openstreetmap.org/?mlat=$latitude&mlon=$longitude#map=${zoomLevel.toInt()}/$latitude/$longitude"
            window.open(osmUrl, "_blank")
        }
    ) {
        AsyncImage(
            model = staticMapUrl,
            contentDescription = "Static Map (Click to open full map in browser)",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            onSuccess = {
                onMapReady?.invoke()
            }
        )
    }
}
