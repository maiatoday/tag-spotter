package net.maiatoday.tagspotter.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlin.math.*

private const val TILE_SIZE = 256

private fun lonToTileX(lon: Double, zoom: Int): Double = (lon + 180.0) / 360.0 * (1 shl zoom)

private fun latToTileY(lat: Double, zoom: Int): Double {
    val rad = lat.coerceIn(-85.05112878, 85.05112878) * (PI / 180.0)
    return (1.0 - ln(tan(rad) + 1.0 / cos(rad)) / PI) / 2.0 * (1 shl zoom)
}

private fun tileXToLon(x: Double, zoom: Int): Double = x / (1 shl zoom) * 360.0 - 180.0

private fun tileYToLat(y: Double, zoom: Int): Double {
    val n = PI - 2.0 * PI * y / (1 shl zoom)
    return atan(sinh(n)) * (180.0 / PI)
}

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
    var centerLat by remember(latitude) { mutableStateOf(latitude) }
    var centerLon by remember(longitude) { mutableStateOf(longitude) }
    var currentZoom by remember(zoomLevel) { mutableStateOf(zoomLevel.coerceIn(2.0, 19.0)) }

    LaunchedEffect(Unit) {
        onMapReady?.invoke()
    }

    val zoomInt = currentZoom.toInt()
    val numTiles = (1 shl zoomInt)

    val tileUrlBase = if (useDarkMap) {
        "https://a.basemaps.cartocdn.com/dark_all"
    } else {
        "https://tile.openstreetmap.org"
    }

    Box(modifier = modifier.fillMaxSize().background(if (useDarkMap) Color(0xFF121212) else Color(0xFFE0E0E0))) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = constraints.maxWidth.toFloat()
            val canvasHeight = constraints.maxHeight.toFloat()
            val screenCenterX = canvasWidth / 2f
            val screenCenterY = canvasHeight / 2f

            val centerTileX = lonToTileX(centerLon, zoomInt)
            val centerTileY = latToTileY(centerLat, zoomInt)

            val minTileX = floor(centerTileX - (screenCenterX / TILE_SIZE) - 1).toInt()
            val maxTileX = ceil(centerTileX + (screenCenterX / TILE_SIZE) + 1).toInt()
            val minTileY = floor(centerTileY - (screenCenterY / TILE_SIZE) - 1).toInt()
            val maxTileY = ceil(centerTileY + (screenCenterY / TILE_SIZE) + 1).toInt()

            // Render Map Tile Images
            for (x in minTileX..maxTileX) {
                for (y in minTileY..maxTileY) {
                    if (y in 0 until numTiles) {
                        val wrappedX = ((x % numTiles) + numTiles) % numTiles
                        val drawX = (screenCenterX + (x - centerTileX).toFloat() * TILE_SIZE).dp
                        val drawY = (screenCenterY + (y - centerTileY).toFloat() * TILE_SIZE).dp
                        val tileUrl = "$tileUrlBase/$zoomInt/$wrappedX/$y.png"

                        AsyncImage(
                            model = tileUrl,
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .offset(x = drawX, y = drawY)
                                .size(TILE_SIZE.dp)
                        )
                    }
                }
            }

            // Canvas Overlay for Touch Gestures, Markers & Radius Circles
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.type == PointerEventType.Scroll) {
                                    val change = event.changes.firstOrNull()
                                    if (change != null) {
                                        val scrollDelta = change.scrollDelta.y
                                        if (scrollDelta != 0f) {
                                            currentZoom = (currentZoom - scrollDelta * 0.25).coerceIn(2.0, 19.0)
                                            change.consume()
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .pointerInput(currentZoom, centerLat, centerLon) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaXPixels = dragAmount.x.toDouble()
                            val deltaYPixels = dragAmount.y.toDouble()
                            
                            val cTileX = lonToTileX(centerLon, zoomInt)
                            val cTileY = latToTileY(centerLat, zoomInt)
                            
                            val newTileX = cTileX - (deltaXPixels / TILE_SIZE)
                            val newTileY = cTileY - (deltaYPixels / TILE_SIZE)
                            
                            centerLon = tileXToLon(newTileX, zoomInt)
                            centerLat = tileYToLat(newTileY, zoomInt)
                        }
                    }
                    .pointerInput(currentZoom, centerLat, centerLon, markers) {
                        detectTapGestures(
                            onDoubleTap = {
                                currentZoom = (currentZoom + 1.0).coerceIn(2.0, 19.0)
                            },
                            onTap = { tapOffset ->
                                val cTileX = lonToTileX(centerLon, zoomInt)
                                val cTileY = latToTileY(centerLat, zoomInt)
                                
                                val cWidth = size.width
                                val cHeight = size.height
                                val sCenterX = cWidth / 2f
                                val sCenterY = cHeight / 2f
                                
                                var markerClicked = false
                                for (marker in markers) {
                                    val mTileX = lonToTileX(marker.longitude, zoomInt)
                                    val mTileY = latToTileY(marker.latitude, zoomInt)
                                    val mPx = sCenterX + (mTileX - cTileX).toFloat() * TILE_SIZE
                                    val mPy = sCenterY + (mTileY - cTileY).toFloat() * TILE_SIZE
                                    
                                    val dist = sqrt((tapOffset.x - mPx).pow(2) + (tapOffset.y - mPy).pow(2))
                                    if (dist <= 24f) {
                                        marker.onClick()
                                        markerClicked = true
                                        break
                                    }
                                }
                                
                                if (!markerClicked) {
                                    val tapTileX = cTileX + (tapOffset.x - sCenterX) / TILE_SIZE
                                    val tapTileY = cTileY + (tapOffset.y - sCenterY) / TILE_SIZE
                                    val clickedLat = tileYToLat(tapTileY, zoomInt)
                                    val clickedLon = tileXToLon(tapTileX, zoomInt)
                                    onMapClick?.invoke(clickedLat, clickedLon)
                                }
                            }
                        )
                    }
            ) {
                clipRect {
                    val cTileX = lonToTileX(centerLon, zoomInt)
                    val cTileY = latToTileY(centerLat, zoomInt)

                    // Draw Radius Circle if provided
                    if (radiusCircleCenterLatitude != null && radiusCircleCenterLongitude != null && radiusCircleMeters > 0) {
                        val rcTileX = lonToTileX(radiusCircleCenterLongitude, zoomInt)
                        val rcTileY = latToTileY(radiusCircleCenterLatitude, zoomInt)
                        val cx = screenCenterX + (rcTileX - cTileX).toFloat() * TILE_SIZE
                        val cy = screenCenterY + (rcTileY - cTileY).toFloat() * TILE_SIZE

                        val metersPerPixel = 156543.03392 * cos(radiusCircleCenterLatitude * (PI / 180.0)) / (1 shl zoomInt)
                        val radiusPx = (radiusCircleMeters / metersPerPixel).toFloat()

                        drawCircle(
                            color = Color(0x332196F3),
                            radius = radiusPx,
                            center = Offset(cx, cy)
                        )
                        drawCircle(
                            color = Color(0xFF2196F3),
                            radius = radiusPx,
                            center = Offset(cx, cy),
                            style = Stroke(width = 2f)
                        )
                    }

                    // Draw Map Markers
                    for (marker in markers) {
                        val mTileX = lonToTileX(marker.longitude, zoomInt)
                        val mTileY = latToTileY(marker.latitude, zoomInt)
                        val mx = screenCenterX + (mTileX - cTileX).toFloat() * TILE_SIZE
                        val my = screenCenterY + (mTileY - cTileY).toFloat() * TILE_SIZE

                        // Shadow
                        drawCircle(
                            color = Color(0x40000000),
                            radius = 12f,
                            center = Offset(mx + 2f, my + 2f)
                        )

                        // Pin Outer
                        val pinColor = if (marker.isStarred) Color(0xFFFFD700) else Color(0xFFE53935)
                        drawCircle(
                            color = pinColor,
                            radius = 12f,
                            center = Offset(mx, my)
                        )

                        // Pin Core
                        drawCircle(
                            color = Color.White,
                            radius = 5f,
                            center = Offset(mx, my)
                        )
                    }
                }
            }
        }

        // Floating Zoom Controls & Attribution Banner
        Box(
            modifier = Modifier.fillMaxSize().padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    onClick = { currentZoom = (currentZoom + 1.0).coerceIn(2.0, 19.0) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (useDarkMap) Color(0xDD333333) else Color(0xDDFFFFFF),
                    contentColor = if (useDarkMap) Color.White else Color.Black,
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Surface(
                    onClick = { currentZoom = (currentZoom - 1.0).coerceIn(2.0, 19.0) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (useDarkMap) Color(0xDD333333) else Color(0xDDFFFFFF),
                    contentColor = if (useDarkMap) Color.White else Color.Black,
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("−", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text(
                text = "© OpenStreetMap contributors",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                color = if (useDarkMap) Color.LightGray else Color.DarkGray,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(if (useDarkMap) Color(0xCC000000) else Color(0xCCFFFFFF))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}
