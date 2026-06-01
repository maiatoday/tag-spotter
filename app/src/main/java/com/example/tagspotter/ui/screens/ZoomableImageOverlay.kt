package com.example.tagspotter.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File

@Composable
fun ZoomableImageOverlay(
    imagePath: String,
    thumbnailPath: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var isOriginalDeleted by remember { mutableStateOf(false) }

    LaunchedEffect(imagePath) {
        if (imagePath.startsWith("android.resource://") || imagePath.startsWith("http")) {
            isOriginalDeleted = false
        } else if (imagePath.startsWith("content://")) {
            val uri = Uri.parse(imagePath)
            try {
                context.contentResolver.openInputStream(uri)?.use { }
            } catch (e: Exception) {
                isOriginalDeleted = true
            }
        } else {
            val file = File(imagePath)
            if (!file.exists()) {
                isOriginalDeleted = true
            }
        }
    }

    val imageModel = remember(imagePath, thumbnailPath, isOriginalDeleted) {
        if (imagePath.startsWith("android.resource://") || imagePath.startsWith("http")) {
            Uri.parse(imagePath)
        } else if (isOriginalDeleted && thumbnailPath.isNotEmpty() && !thumbnailPath.startsWith("android.resource://") && !thumbnailPath.startsWith("http")) {
            File(thumbnailPath)
        } else if (isOriginalDeleted && thumbnailPath.isNotEmpty() && (thumbnailPath.startsWith("android.resource://") || thumbnailPath.startsWith("http"))) {
            Uri.parse(thumbnailPath)
        } else if (imagePath.startsWith("content://")) {
            Uri.parse(imagePath)
        } else {
            File(imagePath)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        if (scale > 1f) {
                            offset += pan
                        } else {
                            offset = Offset.Zero
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 2.5f
                                offset = Offset.Zero
                            }
                        }
                    )
                }
        ) {
            AsyncImage(
                model = imageModel,
                contentDescription = "Zoomable spot image",
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    ),
                contentScale = ContentScale.Fit
            )
        }

        // Warning Badge if Original is Deleted
        if (isOriginalDeleted) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .background(Color.Black.copy(alpha = 0.8f), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.error, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Original photo deleted from gallery",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Close Floating Button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 20.dp)
                .size(48.dp)
                .background(Color.Black.copy(alpha = 0.5f), shape = androidx.compose.foundation.shape.CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close full screen",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
