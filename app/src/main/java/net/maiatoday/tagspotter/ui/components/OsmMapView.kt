package net.maiatoday.tagspotter.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material3.MaterialTheme
import net.maiatoday.tagspotter.theme.categoryColors
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import net.maiatoday.tagspotter.TagSpotterApplication
import androidx.core.graphics.toColorInt

val DarkMatterTileSource = XYTileSource(
    "CartoDbDarkMatter",
    0, 19, 256, ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/dark_all/",
        "https://b.basemaps.cartocdn.com/dark_all/",
        "https://c.basemaps.cartocdn.com/dark_all/",
        "https://d.basemaps.cartocdn.com/dark_all/"
    ),
    "Map tiles by Carto, under CC BY 3.0. Data by OpenStreetMap, under ODbL."
)

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
fun OsmMapView(
    latitude: Double,
    longitude: Double,
    zoomLevel: Double,
    markers: List<OsmMarker>,
    modifier: Modifier = Modifier,
    radiusCircleCenter: GeoPoint? = null,
    radiusCircleMeters: Double = 0.0,
    onMapClick: ((GeoPoint) -> Unit)? = null,
    onMapReady: ((MapView) -> Unit)? = null
) {
    val context = LocalContext.current
    val categoryColors = MaterialTheme.categoryColors
    val mapView = remember(context) {
        MapView(context)
    }

    val settingsRepository = remember(context) {
        (context.applicationContext as TagSpotterApplication).settingsRepository
    }
    val isSystemDark = isSystemInDarkTheme()
    val darkMapEnabled by settingsRepository.darkMapEnabled.collectAsState(initial = false)
    val useDarkMap = isSystemDark && darkMapEnabled

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = {
            mapView.apply {
                setTileSource(if (useDarkMap) DarkMatterTileSource else TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(zoomLevel)
                controller.setCenter(GeoPoint(latitude, longitude))

                if (onMapClick != null) {
                    val receiver = object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                            if (p != null) {
                                onMapClick(p)
                            }
                            return true
                        }
                        override fun longPressHelper(p: GeoPoint?): Boolean = false
                    }
                    overlays.add(MapEventsOverlay(receiver))
                }
                
                onMapReady?.invoke(this)
            }
        },
        update = { map ->
            val expectedTileSource = if (useDarkMap) DarkMatterTileSource else TileSourceFactory.MAPNIK
            if (map.tileProvider.tileSource?.name() != expectedTileSource.name()) {
                map.setTileSource(expectedTileSource)
            }

            // Clear existing markers
            map.overlays.filterIsInstance<Marker>().forEach { map.overlays.remove(it) }
            
            // Clear existing polygons/circles
            map.overlays.filterIsInstance<Polygon>().forEach { map.overlays.remove(it) }

            // Add circle overlay if active
            if (radiusCircleCenter != null && radiusCircleMeters > 0.0) {
                val circleColor = Color(0xFF00FFCC) // Neon Cyan
                val circle = Polygon(map).apply {
                    points = Polygon.pointsAsCircle(radiusCircleCenter, radiusCircleMeters)
                    fillPaint.color = circleColor.copy(alpha = 0.12f).toArgb()
                    outlinePaint.color = circleColor.toArgb()
                    outlinePaint.strokeWidth = 2.0f * map.context.resources.displayMetrics.density
                }
                map.overlays.add(circle)
            }

            // Add pins
            markers.forEach { osmMarker ->
                val markerColor = if (osmMarker.status == "erased") {
                    Color.Gray.toArgb()
                } else {
                    categoryColors.getColorForCategory(osmMarker.category).toArgb()
                }
                val marker = Marker(map).apply {
                    position = GeoPoint(osmMarker.latitude, osmMarker.longitude)
                    icon = createPinDrawable(map.context, markerColor, osmMarker.isStarred)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = osmMarker.title
                    
                    setOnMarkerClickListener { _, _ ->
                        osmMarker.onClick()
                        true
                    }
                }
                map.overlays.add(marker)
            }

            map.invalidate()
        },
        modifier = modifier
    )
}

private fun createPinDrawable(context: Context, colorSource: Int, isStarred: Boolean): Drawable {
    val density = context.resources.displayMetrics.density
    val size = (36 * density).toInt()
    
    val bitmap = createBitmap(size, size)
    val canvas = Canvas(bitmap)
    
    val paint = Paint().apply {
        isAntiAlias = true
        color = colorSource
        style = Paint.Style.FILL
    }

    val centerX = size / 2f
    val centerY = size / 2.5f
    val radius = size / 3.5f
    canvas.drawCircle(centerX, centerY, radius, paint)

    val path = android.graphics.Path().apply {
        moveTo(centerX - radius * 0.8f, centerY + radius * 0.5f)
        lineTo(centerX, size * 0.9f)
        lineTo(centerX + radius * 0.8f, centerY + radius * 0.5f)
        close()
    }
    canvas.drawPath(path, paint)

    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(centerX, centerY, radius / 2.5f, paint)

    if (isStarred) {
        val borderPaint = Paint().apply {
            isAntiAlias = true
            color = "#FFD700".toColorInt() // Gold
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * density
        }
        canvas.drawCircle(centerX, centerY, radius + 1.25f * density, borderPaint)
    }

    return bitmap.toDrawable(context.resources)
}
