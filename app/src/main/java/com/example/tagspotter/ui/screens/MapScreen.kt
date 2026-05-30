package com.example.tagspotter.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.tagspotter.TagSpotterApplication
import com.example.tagspotter.data.SpotDetails
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MapScreen(
    selectedCategory: String,
    onSpotClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as TagSpotterApplication
    val repository = app.repository

    // Fetch spots by category to map pins
    val spots by repository.getSpotsByCategory(selectedCategory).collectAsState(initial = emptyList())
    var selectedSpot by remember { mutableStateOf<SpotDetails?>(null) }
    var mapView: MapView? by remember { mutableStateOf(null) }

    // Constants for marker icons
    val colorGraffiti = MaterialTheme.colorScheme.tertiary.toArgb()     // Neon Hot Pink
    val colorSculpture = MaterialTheme.colorScheme.secondary.toArgb()  // Neon Cyan
    val colorTree = MaterialTheme.colorScheme.primary.toArgb()         // Neon Green
    val colorArchitecture = Color(0xFFBF5AF2).toArgb()                 // Neon Purple
    val colorPublicPlace = Color(0xFFFF9F0A).toArgb()                  // Neon Orange
    val colorErased = Color.Gray.toArgb()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    
                    // Default centering on City Center (approximate)
                    controller.setZoom(14.0)
                    controller.setCenter(GeoPoint(45.0, 9.0)) // Fallback center

                    val receiver = object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                            selectedSpot = null
                            return true
                        }
                        override fun longPressHelper(p: GeoPoint?): Boolean = false
                    }
                    overlays.add(MapEventsOverlay(receiver))
                    mapView = this
                }
            },
            update = { map ->
                map.overlays.filterIsInstance<Marker>().forEach { map.overlays.remove(it) }

                // Add pins for all spots
                spots.forEach { spotDetails ->
                    val isErased = spotDetails.spot.status == "erased"
                    val markerColor = when {
                        isErased -> colorErased
                        spotDetails.spot.category == "graffiti" -> colorGraffiti
                        spotDetails.spot.category == "sculpture" -> colorSculpture
                        spotDetails.spot.category == "tree" -> colorTree
                        spotDetails.spot.category == "architecture" -> colorArchitecture
                        spotDetails.spot.category == "public_place" -> colorPublicPlace
                        else -> colorGraffiti
                    }

                    val marker = Marker(map).apply {
                        position = GeoPoint(spotDetails.spot.latitude, spotDetails.spot.longitude)
                        icon = createPinDrawable(context, markerColor)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = spotDetails.spot.category.capitalize()
                        
                        setOnMarkerClickListener { _, _ ->
                            selectedSpot = spotDetails
                            map.controller.animateTo(position)
                            true
                        }
                    }
                    map.overlays.add(marker)
                }

                // If spots are loaded, auto-center on the latest spot
                if (spots.isNotEmpty() && selectedSpot == null) {
                    val latest = spots.first().spot
                    map.controller.setCenter(GeoPoint(latest.latitude, latest.longitude))
                }
                map.invalidate()
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Slide-up card for Selected Pin details
        AnimatedVisibility(
            visible = selectedSpot != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            val spot = selectedSpot
            if (spot != null) {
                val latestImage = spot.images.maxByOrNull { it.timestamp }
                val isErased = spot.spot.status == "erased"

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (isErased) Color.DarkGray else MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(12.dp)
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Thumbnail Preview
                            if (latestImage != null) {
                                AsyncImage(
                                    model = File(latestImage.imagePath),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.DarkGray),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            // Detail Info Column
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = spot.spot.category.replace("_", " ").capitalize(),
                                        color = if (isErased) Color.Gray else MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isErased) {
                                        Text(
                                            text = "(Gone)",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    text = spot.spot.description.ifEmpty { "No description added." },
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                // Display tags
                                if (spot.spot.tags.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        spot.spot.tags.take(3).forEach { tag ->
                                            Text(
                                                text = "#$tag",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = { onSpotClick(spot.spot.id) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isErased) Color.DarkGray else MaterialTheme.colorScheme.secondary,
                                        contentColor = MaterialTheme.colorScheme.background
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Text("View History & Notes", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        // Close icon overlay
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close details overlay",
                            tint = Color.Gray,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(20.dp)
                                .clickable { selectedSpot = null }
                        )
                    }
                }
            }
        }
    }
}

// Programmatic Pin drawing helper to avoid precompiled drawable assets
private fun createPinDrawable(context: Context, colorArgb: Int): Drawable {
    val density = context.resources.displayMetrics.density
    val size = (36 * density).toInt()
    
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    val paint = Paint().apply {
        isAntiAlias = true
        color = colorArgb
        style = Paint.Style.FILL
    }

    // Draw main pin bubble (circle)
    val centerX = size / 2f
    val centerY = size / 2.5f
    val radius = size / 3.5f
    canvas.drawCircle(centerX, centerY, radius, paint)

    // Draw bottom arrow point
    val path = android.graphics.Path().apply {
        moveTo(centerX - radius * 0.8f, centerY + radius * 0.5f)
        lineTo(centerX, size * 0.9f)
        lineTo(centerX + radius * 0.8f, centerY + radius * 0.5f)
        close()
    }
    canvas.drawPath(path, paint)

    // Draw white center dot
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(centerX, centerY, radius / 2.5f, paint)

    return BitmapDrawable(context.resources, bitmap)
}
