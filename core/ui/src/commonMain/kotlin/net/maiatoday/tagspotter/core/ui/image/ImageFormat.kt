package net.maiatoday.tagspotter.core.ui.image

import androidx.compose.runtime.Composable

expect fun formatImageModel(imagePath: String, thumbnailPath: String): Any

@Composable
expect fun rememberPhotoDeletedChecker(): (imagePath: String, callback: (Boolean) -> Unit) -> Unit
