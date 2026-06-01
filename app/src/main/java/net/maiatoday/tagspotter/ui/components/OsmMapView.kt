package net.maiatoday.tagspotter.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

data class OsmMarker(
    val id: Long,
    val latitude: Double,
    val longitude: Double,
    val category: String,
    val status: String,
    val title: String,
    val onClick: () -> Unit
)

@Composable
fun OsmMapView(
    latitude: Double,
    longitude: Double,
    zoomLevel: Double,
    markers: List<OsmMarker>,
    modifier: Modifier = Modifier,
    onMapClick: ((GeoPoint) -> Unit)? = null,
    onMapReady: ((MapView) -> Unit)? = null
) {
    val context = LocalContext.current
    val mapView = remember(context) {
        MapView(context)
    }

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
                setTileSource(TileSourceFactory.MAPNIK)
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
            // Clear existing markers
            map.overlays.filterIsInstance<Marker>().forEach { map.overlays.remove(it) }

            // Add pins
            markers.forEach { osmMarker ->
                val markerColor = getMarkerColor(osmMarker.category, osmMarker.status)
                val marker = Marker(map).apply {
                    position = GeoPoint(osmMarker.latitude, osmMarker.longitude)
                    icon = createPinDrawable(map.context, markerColor)
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

private fun getMarkerColor(category: String, status: String): Int {
    if (status == "erased") {
        return Color.Gray.toArgb()
    }
    return when (category) {
        "graffiti" -> Color(0xFFFF2D55).toArgb() // Neon Pink
        "sculpture" -> Color(0xFF00C7BE).toArgb() // Neon Cyan
        "tree" -> Color(0xFF34C759).toArgb() // Neon Green
        "architecture" -> Color(0xFFBF5AF2).toArgb() // Neon Purple
        "public_place" -> Color(0xFFFF9F0A).toArgb() // Neon Orange
        else -> Color(0xFFFF2D55).toArgb()
    }
}

private fun createPinDrawable(context: Context, colorSource: Int): Drawable {
    val density = context.resources.displayMetrics.density
    val size = (36 * density).toInt()
    
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
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

    return BitmapDrawable(context.resources, bitmap)
}
