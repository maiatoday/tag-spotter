package net.maiatoday.tagspotter.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO
import kotlin.math.*

private const val TILE_SIZE = 256
private val tileCache = ConcurrentHashMap<String, ImageBitmap>()

private fun getCacheDir(): File {
    val dir = File(System.getProperty("user.home"), ".tagspotter/map_cache")
    if (!dir.exists()) dir.mkdirs()
    return dir
}

private fun lonToTileX(lon: Double, zoom: Int): Double = (lon + 180.0) / 360.0 * (1 shl zoom)

private fun latToTileY(lat: Double, zoom: Int): Double {
    val rad = Math.toRadians(lat.coerceIn(-85.05112878, 85.05112878))
    return (1.0 - ln(tan(rad) + 1.0 / cos(rad)) / Math.PI) / 2.0 * (1 shl zoom)
}

private fun tileXToLon(x: Double, zoom: Int): Double = x / (1 shl zoom) * 360.0 - 180.0

private fun tileYToLat(y: Double, zoom: Int): Double {
    val n = Math.PI - 2.0 * Math.PI * y / (1 shl zoom)
    return Math.toDegrees(atan(sinh(n)))
}

private suspend fun fetchTile(zoom: Int, x: Int, y: Int): ImageBitmap? = withContext(Dispatchers.IO) {
    val maxTile = (1 shl zoom) - 1
    if (x < 0 || x > maxTile || y < 0 || y > maxTile) return@withContext null

    val cacheKey = "$zoom/$x/$y"
    tileCache[cacheKey]?.let { return@withContext it }

    val localFile = File(getCacheDir(), "$zoom-$x-$y.png")
    if (localFile.exists() && localFile.length() > 0) {
        try {
            val bufferedImage = ImageIO.read(localFile)
            if (bufferedImage != null) {
                val bitmap = bufferedImage.toComposeImageBitmap()
                tileCache[cacheKey] = bitmap
                return@withContext bitmap
            }
        } catch (_: Exception) {
            localFile.delete()
        }
    }

    try {
        val url = URL("https://tile.openstreetmap.org/$zoom/$x/$y.png")
        val connection = url.openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", "TagSpotterDesktop/1.0 (net.maiatoday.tagspotter)")
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.connect()

        if (connection.responseCode == 200) {
            val bytes = connection.inputStream.readBytes()
            localFile.writeBytes(bytes)
            val bufferedImage = ImageIO.read(localFile)
            if (bufferedImage != null) {
                val bitmap = bufferedImage.toComposeImageBitmap()
                tileCache[cacheKey] = bitmap
                return@withContext bitmap
            }
        }
    } catch (e: Exception) {
        // Tile fetch retry on next render pass
    }
    null
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

    val coroutines = rememberCoroutineScope()
    var retriggerCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        onMapReady?.invoke()
    }

    val darkColorFilter = remember(useDarkMap) {
        if (useDarkMap) {
            val colorMatrix = ColorMatrix(
                floatArrayOf(
                    -0.8f, 0f, 0f, 0f, 255f,
                    0f, -0.8f, 0f, 0f, 255f,
                    0f, 0f, -0.8f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            ColorFilter.colorMatrix(colorMatrix)
        } else null
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFFE0E0E0))) {
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
                                        val newZoom = (currentZoom - scrollDelta * 0.25).coerceIn(2.0, 19.0)
                                        currentZoom = newZoom
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
                        val zoomInt = currentZoom.toInt()
                        val scaleFactor = (1 shl zoomInt).toDouble()
                        
                        val deltaXPixels = dragAmount.x.toDouble()
                        val deltaYPixels = dragAmount.y.toDouble()
                        
                        val centerTileX = lonToTileX(centerLon, zoomInt)
                        val centerTileY = latToTileY(centerLat, zoomInt)
                        
                        val newTileX = centerTileX - (deltaXPixels / TILE_SIZE)
                        val newTileY = centerTileY - (deltaYPixels / TILE_SIZE)
                        
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
                            val zoomInt = currentZoom.toInt()
                            val centerTileX = lonToTileX(centerLon, zoomInt)
                            val centerTileY = latToTileY(centerLat, zoomInt)
                            
                            val canvasWidth = size.width
                            val canvasHeight = size.height
                            val screenCenterX = canvasWidth / 2f
                            val screenCenterY = canvasHeight / 2f
                            
                            // Check if tapped on a marker pin
                            var markerClicked = false
                            for (marker in markers) {
                                val mTileX = lonToTileX(marker.longitude, zoomInt)
                                val mTileY = latToTileY(marker.latitude, zoomInt)
                                val mPx = screenCenterX + (mTileX - centerTileX).toFloat() * TILE_SIZE
                                val mPy = screenCenterY + (mTileY - centerTileY).toFloat() * TILE_SIZE
                                
                                val dist = sqrt((tapOffset.x - mPx).pow(2) + (tapOffset.y - mPy).pow(2))
                                if (dist <= 24f) {
                                    marker.onClick()
                                    markerClicked = true
                                    break
                                }
                            }
                            
                            if (!markerClicked) {
                                val tapTileX = centerTileX + (tapOffset.x - screenCenterX) / TILE_SIZE
                                val tapTileY = centerTileY + (tapOffset.y - screenCenterY) / TILE_SIZE
                                val clickedLat = tileYToLat(tapTileY, zoomInt)
                                val clickedLon = tileXToLon(tapTileX, zoomInt)
                                onMapClick?.invoke(clickedLat, clickedLon)
                            }
                        }
                    )
                }
        ) {
            clipRect {
                val canvasWidth = size.width
                val canvasHeight = size.height
            val screenCenterX = canvasWidth / 2f
            val screenCenterY = canvasHeight / 2f

            val zoomInt = currentZoom.toInt()
            val centerTileX = lonToTileX(centerLon, zoomInt)
            val centerTileY = latToTileY(centerLat, zoomInt)

            val minTileX = floor(centerTileX - (screenCenterX / TILE_SIZE) - 1).toInt()
            val maxTileX = ceil(centerTileX + (screenCenterX / TILE_SIZE) + 1).toInt()
            val minTileY = floor(centerTileY - (screenCenterY / TILE_SIZE) - 1).toInt()
            val maxTileY = ceil(centerTileY + (screenCenterY / TILE_SIZE) + 1).toInt()

            val numTiles = (1 shl zoomInt)

            for (x in minTileX..maxTileX) {
                for (y in minTileY..maxTileY) {
                    if (y in 0 until numTiles) {
                        val wrappedX = Math.floorMod(x, numTiles)
                        val cacheKey = "$zoomInt/$wrappedX/$y"
                        val bitmap = tileCache[cacheKey]

                        val drawX = screenCenterX + (x - centerTileX).toFloat() * TILE_SIZE
                        val drawY = screenCenterY + (y - centerTileY).toFloat() * TILE_SIZE

                        if (bitmap != null) {
                            drawImage(
                                image = bitmap,
                                topLeft = Offset(drawX, drawY),
                                colorFilter = darkColorFilter
                            )
                        } else {
                            // Draw grid placeholder
                            drawRect(
                                color = if (useDarkMap) Color(0xFF2A2A2A) else Color(0xFFE8E8E8),
                                topLeft = Offset(drawX, drawY),
                                size = Size(TILE_SIZE.toFloat(), TILE_SIZE.toFloat())
                            )
                            drawRect(
                                color = if (useDarkMap) Color(0xFF333333) else Color(0xFFD0D0D0),
                                topLeft = Offset(drawX, drawY),
                                size = Size(TILE_SIZE.toFloat(), TILE_SIZE.toFloat()),
                                style = Stroke(width = 1f)
                            )
                            
                            // Trigger background tile download
                            coroutines.launch {
                                if (fetchTile(zoomInt, wrappedX, y) != null) {
                                    retriggerCount++
                                }
                            }
                        }
                    }
                }
            }

            // Draw Radius Circle if provided
            if (radiusCircleCenterLatitude != null && radiusCircleCenterLongitude != null && radiusCircleMeters > 0) {
                val cTileX = lonToTileX(radiusCircleCenterLongitude, zoomInt)
                val cTileY = latToTileY(radiusCircleCenterLatitude, zoomInt)
                val cx = screenCenterX + (cTileX - centerTileX).toFloat() * TILE_SIZE
                val cy = screenCenterY + (cTileY - centerTileY).toFloat() * TILE_SIZE

                // Approximate meters to pixels at equator
                val metersPerPixel = 156543.03392 * cos(Math.toRadians(radiusCircleCenterLatitude)) / (1 shl zoomInt)
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
                val mx = screenCenterX + (mTileX - centerTileX).toFloat() * TILE_SIZE
                val my = screenCenterY + (mTileY - centerTileY).toFloat() * TILE_SIZE

                // Marker shadow
                drawCircle(
                    color = Color(0x40000000),
                    radius = 12f,
                    center = Offset(mx + 2f, my + 2f)
                )

                // Marker Pin Outer
                val pinColor = if (marker.isStarred) Color(0xFFFFD700) else Color(0xFFE53935)
                drawCircle(
                    color = pinColor,
                    radius = 12f,
                    center = Offset(mx, my)
                )

                // Marker Pin Core
                drawCircle(
                    color = Color.White,
                    radius = 5f,
                    center = Offset(mx, my)
                )
            }
        }
    }

        // OpenStreetMap Attribution Banner & Zoom Controls
        Box(
            modifier = Modifier.fillMaxSize().padding(12.dp)
        ) {
            // Floating Zoom Controls (+ / -)
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

            // Attribution text
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
