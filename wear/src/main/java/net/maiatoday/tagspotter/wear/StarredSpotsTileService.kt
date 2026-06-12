package net.maiatoday.tagspotter.wear

import android.content.Context
import android.util.Log
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.Chip
import androidx.wear.protolayout.material.CompactChip
import androidx.wear.protolayout.material.Text
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import net.maiatoday.tagspotter.core.model.SpotDetails
import net.maiatoday.tagspotter.wear.R

class StarredSpotsTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        Log.d("StarredSpotsTileService", "onTileRequest: lastClickableId=${requestParams.currentState.lastClickableId}")
        
        // Handle refresh button click
        if (requestParams.currentState.lastClickableId == "refresh_button") {
            requestNearbySpots()
        }

        // Read spots from cache
        val spots = loadCachedSpots()

        // If the cache is completely empty, trigger a refresh to populate it
        if (spots.isEmpty()) {
            requestNearbySpots()
        }

        val deviceParams = requestParams.deviceConfiguration
        val layout = createLayout(this, deviceParams, spots)

        val tile = TileBuilders.Tile.Builder()
            .setResourcesVersion("1")
            .setTileTimeline(
                TimelineBuilders.Timeline.Builder()
                    .addTimelineEntry(
                        TimelineBuilders.TimelineEntry.Builder()
                            .setLayout(
                                LayoutElementBuilders.Layout.Builder()
                                    .setRoot(layout)
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()

        return Futures.immediateFuture(tile)
    }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        val resources = ResourceBuilders.Resources.Builder()
            .setVersion("1")
            .build()
        return Futures.immediateFuture(resources)
    }

    private fun loadCachedSpots(): List<SpotDetails> {
        try {
            val sharedPref = getSharedPreferences("tagspotter_wear_prefs", MODE_PRIVATE)
            val json = sharedPref.getString("cached_spots_json", null)
            if (json != null) {
                return Json.decodeFromString<List<SpotDetails>>(json)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return emptyList()
    }

    private fun requestNearbySpots() {
        serviceScope.launch {
            try {
                val nodeClient = Wearable.getNodeClient(this@StarredSpotsTileService)
                val nodes = Tasks.await(nodeClient.connectedNodes)
                if (nodes.isEmpty()) {
                    Log.d("StarredSpotsTileService", "No connected nodes found to query spots.")
                    return@launch
                }
                for (node in nodes) {
                    Wearable.getMessageClient(this@StarredSpotsTileService)
                        .sendMessage(node.id, "/query_nearby_spots", null)
                    Log.d("StarredSpotsTileService", "Sent query_nearby_spots to node: ${node.id}")
                }
            } catch (e: Exception) {
                Log.e("StarredSpotsTileService", "Error sending query_nearby_spots", e)
            }
        }
    }

    private fun createLayout(
        context: Context,
        deviceParams: DeviceParametersBuilders.DeviceParameters,
        spots: List<SpotDetails>
    ): LayoutElementBuilders.LayoutElement {
        val columnBuilder = LayoutElementBuilders.Column.Builder()
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)

        // Title text
        columnBuilder.addContent(
            Text.Builder(context, context.getString(R.string.starred_spots_tile_title))
                .setTypography(androidx.wear.protolayout.material.Typography.TYPOGRAPHY_TITLE3)
                .setColor(ColorBuilders.argb(0xFF00FFCC.toInt())) // Neon cyan
                .build()
        )

        columnBuilder.addContent(
            LayoutElementBuilders.Spacer.Builder()
                .setHeight(DimensionBuilders.dp(8f))
                .build()
        )

        if (spots.isEmpty()) {
            columnBuilder.addContent(
                Text.Builder(context, context.getString(R.string.no_spots_cached_tile))
                    .setTypography(androidx.wear.protolayout.material.Typography.TYPOGRAPHY_BODY2)
                    .setColor(ColorBuilders.argb(0xFF888888.toInt()))
                    .build()
            )
        } else {
            // Display up to 3 spots
            val spotsToShow = spots.take(3)
            for (spotDetails in spotsToShow) {
                val jsonStr = Json.encodeToString(SpotDetails.serializer(), spotDetails)

                val launchAction = ActionBuilders.AndroidActivity.Builder()
                    .setPackageName("net.maiatoday.tagspotter")
                    .setClassName("net.maiatoday.tagspotter.wear.WearMainActivity")
                    .addKeyToExtraMapping("EXTRA_SPOT_DETAILS_JSON", ActionBuilders.stringExtra(jsonStr))
                    .build()

                val clickable = ModifiersBuilders.Clickable.Builder()
                    .setId("spot_${spotDetails.spot.id}")
                    .setOnClick(
                        ActionBuilders.LaunchAction.Builder()
                            .setAndroidActivity(launchAction)
                            .build()
                    )
                    .build()

                val chip = Chip.Builder(context, clickable, deviceParams)
                    .setPrimaryLabelContent(spotDetails.spot.description)
                    .setSecondaryLabelContent(spotDetails.spot.category.uppercase())
                    .setWidth(DimensionBuilders.dp(140f))
                    .build()

                columnBuilder.addContent(chip)

                columnBuilder.addContent(
                    LayoutElementBuilders.Spacer.Builder()
                        .setHeight(DimensionBuilders.dp(4f))
                        .build()
                )
            }
        }

        columnBuilder.addContent(
            LayoutElementBuilders.Spacer.Builder()
                .setHeight(DimensionBuilders.dp(4f))
                .build()
        )

        // Refresh Button
        val refreshClickable = ModifiersBuilders.Clickable.Builder()
            .setId("refresh_button")
            .setOnClick(
                ActionBuilders.LoadAction.Builder().build()
            )
            .build()

        val refreshChip = CompactChip.Builder(context, context.getString(R.string.refresh_btn), refreshClickable, deviceParams)
            .build()

        columnBuilder.addContent(refreshChip)

        return columnBuilder.build()
    }
}
