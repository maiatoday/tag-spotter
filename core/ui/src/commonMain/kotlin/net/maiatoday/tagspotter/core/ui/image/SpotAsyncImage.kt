package net.maiatoday.tagspotter.core.ui.image

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest

/**
 * Shared Coil 3 image composable for Tag Spotter with:
 * - cache keys scoped to the resolved model + [remoteThumbnailUrl] + retry nonce,
 * - error logging and a visible fallback state instead of a silent blank box,
 * - optional tap-to-retry bypassing cached failures.
 */
@Composable
fun SpotAsyncImage(
    model: Any,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    remoteThumbnailUrl: String = "",
    contentScale: ContentScale = ContentScale.Crop,
    allowRetryOnError: Boolean = false
) {
    val platformContext = LocalPlatformContext.current
    var retryNonce by remember(model, remoteThumbnailUrl) { mutableIntStateOf(0) }
    var hasError by remember(model, remoteThumbnailUrl, retryNonce) { mutableStateOf(false) }

    val cacheKey = remember(model, remoteThumbnailUrl, retryNonce) {
        buildString {
            append(model.toString())
            if (remoteThumbnailUrl.isNotBlank()) {
                append("|")
                append(remoteThumbnailUrl)
            }
            if (retryNonce > 0) {
                append("#r")
                append(retryNonce)
            }
        }
    }

    val request = remember(platformContext, model, cacheKey, retryNonce) {
        ImageRequest.Builder(platformContext)
            .data(model)
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
            .apply {
                if (retryNonce > 0) {
                    memoryCachePolicy(CachePolicy.WRITE_ONLY)
                    diskCachePolicy(CachePolicy.WRITE_ONLY)
                }
            }
            .build()
    }

    Box(modifier = modifier) {
        AsyncImage(
            model = request,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
            onSuccess = {
                hasError = false
            },
            onError = { state ->
                hasError = true
                println("SpotAsyncImage failed to load [$model]: ${state.result.throwable.message}")
            }
        )

        if (hasError) {
            val errorModifier = if (allowRetryOnError) {
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { retryNonce++ }
                    .padding(8.dp)
            } else {
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(8.dp)
            }
            Column(
                modifier = errorModifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (allowRetryOnError) Icons.Default.Refresh else Icons.Default.BrokenImage,
                    contentDescription = if (allowRetryOnError) "Tap to retry loading image" else "Image unavailable",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.size(28.dp)
                )
                if (allowRetryOnError) {
                    Text(
                        text = "Tap to retry",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}
