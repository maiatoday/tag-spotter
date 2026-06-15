package net.maiatoday.tagspotter.feature.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import net.maiatoday.tagspotter.core.ui.SpotMapView
import net.maiatoday.tagspotter.core.ui.MapMarker

@Composable
fun DetailMiniMapCard(
    latitude: Double,
    longitude: Double,
    category: String,
    useDarkMap: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black
        )
    ) {
        val mapMarker = MapMarker(
            id = 0L,
            latitude = latitude,
            longitude = longitude,
            category = category,
            status = "active",
            title = "Spot Location",
            onClick = {}
        )

        SpotMapView(
            latitude = latitude,
            longitude = longitude,
            zoomLevel = 17.0,
            markers = listOf(mapMarker),
            useDarkMap = useDarkMap,
            modifier = Modifier.fillMaxSize(),
            onMapClick = { _, _ -> }
        )
    }
}
