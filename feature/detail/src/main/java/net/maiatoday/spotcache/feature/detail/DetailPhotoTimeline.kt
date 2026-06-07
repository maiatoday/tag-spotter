package net.maiatoday.spotcache.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import net.maiatoday.spotcache.core.model.SpotImage

@Composable
fun DetailPhotoTimeline(
    sortedImages: List<SpotImage>,
    onAddPhotoClick: () -> Unit,
    onImageClick: (SpotImage) -> Unit,
    onHeartClick: (SpotImage) -> Unit,
    onDeleteClick: (SpotImage) -> Unit,
    onRatingChange: (SpotImage, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "PHOTO TIMELINE",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = onAddPhotoClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = "Add image",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(end = 64.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(sortedImages, key = { it.id }) { image ->
                SpotTimelineCard(
                    image = image,
                    isMain = image.isMain,
                    onHeartClick = { onHeartClick(image) },
                    onClick = { onImageClick(image) },
                    onDeleteClick = { onDeleteClick(image) },
                    onRatingChange = { rating -> onRatingChange(image, rating) }
                )
            }
        }
    }
}

@Composable
fun SpotTimelineCard(
    image: SpotImage,
    isMain: Boolean,
    onHeartClick: () -> Unit,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRatingChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .height(180.dp)
            .clickable { onClick() }
            .border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            val imageModel = remember(image.imagePath, image.thumbnailPath) {
                if (image.thumbnailPath.isNotEmpty() && !image.thumbnailPath.startsWith("android.resource://") && !image.thumbnailPath.startsWith("http")) {
                    File(image.thumbnailPath)
                } else if (image.thumbnailPath.isNotEmpty() && (image.thumbnailPath.startsWith("android.resource://") || image.thumbnailPath.startsWith("http"))) {
                    image.thumbnailPath.toUri()
                } else if (image.imagePath.startsWith("content://") || image.imagePath.startsWith("android.resource://") || image.imagePath.startsWith("http")) {
                    image.imagePath.toUri()
                } else {
                    File(image.imagePath)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.4f), shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete image",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onHeartClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.4f), shape = CircleShape)
                ) {
                    Icon(
                        imageVector = if (isMain) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Main thumbnail",
                        tint = if (isMain) Color(0xFFF43F5E) else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val formattedDate = sdf.format(Date(image.timestamp))
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..5) {
                        val isStarred = i <= image.rating
                        Icon(
                            imageVector = if (isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Star $i",
                            tint = if (isStarred) Color(0xFFFFD700) else Color.Gray,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable {
                                    val newRating = if (image.rating == i) 0 else i
                                    onRatingChange(newRating)
                                }
                        )
                    }
                }
            }
        }
    }
}
