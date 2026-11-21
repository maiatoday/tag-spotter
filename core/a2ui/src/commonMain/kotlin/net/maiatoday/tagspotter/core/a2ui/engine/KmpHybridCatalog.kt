package net.maiatoday.tagspotter.core.a2ui.engine

import androidx.compose.runtime.Composable
import com.contextable.a2ui4k.catalog.CoreCatalogItems
import com.contextable.a2ui4k.model.ActionEvent
import com.contextable.a2ui4k.model.Catalog
import com.contextable.a2ui4k.model.CatalogItem
import com.contextable.a2ui4k.model.DataContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import net.maiatoday.tagspotter.core.ui.components.NeonArtistBioCard
import net.maiatoday.tagspotter.core.ui.components.NeonArtworkThumbnail
import net.maiatoday.tagspotter.core.ui.components.NeonMapPin
import net.maiatoday.tagspotter.core.ui.components.NeonMapPinOverlayWidget
import net.maiatoday.tagspotter.core.ui.components.NeonSpotCard
import net.maiatoday.tagspotter.core.ui.components.NeonTourCard
import net.maiatoday.tagspotter.core.ui.components.NeonTourWaypoint

const val NEON_HYBRID_CATALOG_ID = "https://tagspotter.maiatoday.net/a2ui/neon_hybrid_catalog/v1/catalog.json"

/**
 * Creates the KMP Hybrid Catalog combining standard CoreCatalog primitives
 * (Card, Column, Text, ChoicePicker, Button, etc.) with the 4 custom domain components
 * (SpotCard, TourCard, ArtistBioCard, MapPinOverlay) backed by :core:ui composables.
 */
fun createKmpHybridCatalog(): Catalog {
    val coreCatalog = CoreCatalogItems.asCatalog()

    val spotCardItem = CatalogItem(
        name = "SpotCard",
        compose = { componentId, data, _, dataContext, onEvent ->
            val title = data["title"].asString(dataContext) ?: ""
            val spotId = data["spotId"].asString(dataContext) ?: componentId
            val artist = data["artist"].asString(dataContext)
            val category = data["category"].asString(dataContext) ?: "graffiti"
            val imageUrl = data["imageUrl"].asString(dataContext)
            val tags = data["tags"].asStringList(dataContext) ?: emptyList()
            val distanceMeters = data["distanceMeters"].asDouble(dataContext)
            val latitude = data["latitude"].asDouble(dataContext)
            val longitude = data["longitude"].asDouble(dataContext)
            val isStarred = data["isStarred"].asBoolean(dataContext) ?: false
            val isSaved = data["isSaved"].asBoolean(dataContext) ?: false

            NeonSpotCard(
                title = title,
                spotId = spotId,
                artist = artist,
                imageUrl = imageUrl,
                tags = tags,
                distanceMeters = distanceMeters,
                latitude = latitude,
                longitude = longitude,
                isStarred = isStarred,
                onStarToggle = {
                    val path = data["isStarred"].asPath()
                    if (path != null) {
                        dataContext.update(path, !isStarred)
                    }
                    onEvent(
                        ActionEvent(
                            name = "toggle_star",
                            surfaceId = "",
                            sourceComponentId = componentId,
                            timestamp = "",
                            context = buildJsonObject {
                                put("spotId", spotId)
                                put("isStarred", !isStarred)
                            }
                        )
                    )
                },
                isSaved = isSaved,
                onSaveSpot = {
                    val path = data["isSaved"].asPath()
                    if (path != null) {
                        dataContext.update(path, true)
                    }
                    onEvent(
                        ActionEvent(
                            name = "save_spot",
                            surfaceId = "",
                            sourceComponentId = componentId,
                            timestamp = "",
                            context = buildJsonObject {
                                put("spotId", spotId)
                                put("title", title)
                                if (!artist.isNullOrBlank()) put("artist", artist)
                                put("category", category)
                                if (latitude != null) put("latitude", latitude)
                                if (longitude != null) put("longitude", longitude)
                                if (!imageUrl.isNullOrBlank()) put("imageUrl", imageUrl)
                                put("tags", JsonArray(tags.map { JsonPrimitive(it) }))
                            }
                        )
                    )
                },
                onViewOnMap = { lat, lon, t ->
                    onEvent(
                        ActionEvent(
                            name = "view_on_map",
                            surfaceId = "",
                            sourceComponentId = componentId,
                            timestamp = "",
                            context = buildJsonObject {
                                put("latitude", lat)
                                put("longitude", lon)
                                put("title", t)
                                put("spotId", spotId)
                            }
                        )
                    )
                },
                onCardClick = {
                    onEvent(
                        ActionEvent(
                            name = "open_spot_detail",
                            surfaceId = "",
                            sourceComponentId = componentId,
                            timestamp = "",
                            context = buildJsonObject {
                                put("spotId", spotId)
                            }
                        )
                    )
                }
            )
        }
    )

    val tourCardItem = CatalogItem(
        name = "TourCard",
        compose = { componentId, data, _, dataContext, onEvent ->
            val tourTitle = data["tourTitle"].asString(dataContext)
                ?: data["title"].asString(dataContext)
                ?: ""
            val description = data["description"].asString(dataContext) ?: ""
            val estimatedMinutes = data["estimatedMinutes"].asDouble(dataContext)?.toInt() ?: 0
            val totalDistanceKm = data["totalDistanceKm"].asDouble(dataContext) ?: 0.0

            val rawWaypoints = data["waypoints"]
            val waypoints = when (rawWaypoints) {
                is JsonArray -> rawWaypoints.mapIndexed { idx, element ->
                    val obj = element as? JsonObject
                    val lat = obj?.get("latitude")?.jsonPrimitive?.doubleOrNull ?: 0.0
                    val lon = obj?.get("longitude")?.jsonPrimitive?.doubleOrNull ?: 0.0
                    val wpTitle = obj?.get("title")?.jsonPrimitive?.contentOrNull ?: "Stop ${idx + 1}"
                    val wpAddress = obj?.get("address")?.jsonPrimitive?.contentOrNull
                    val wpImage = obj?.get("imageUrl")?.jsonPrimitive?.contentOrNull
                    val wpCompleted = obj?.get("completed")?.jsonPrimitive?.booleanOrNull ?: false
                    val wpIndex = obj?.get("index")?.jsonPrimitive?.intOrNull ?: (idx + 1)
                    NeonTourWaypoint(
                        index = wpIndex,
                        title = wpTitle,
                        address = wpAddress,
                        latitude = lat,
                        longitude = lon,
                        imageUrl = wpImage,
                        completed = wpCompleted
                    )
                }
                else -> emptyList()
            }

            NeonTourCard(
                tourTitle = tourTitle,
                description = description,
                estimatedMinutes = estimatedMinutes,
                totalDistanceKm = totalDistanceKm,
                waypoints = waypoints,
                onNavigateToStop = { stopIndex, lat, lon, title ->
                    onEvent(
                        ActionEvent(
                            name = "navigate_to_stop",
                            surfaceId = "",
                            sourceComponentId = componentId,
                            timestamp = "",
                            context = buildJsonObject {
                                put("stopIndex", stopIndex)
                                put("latitude", lat)
                                put("longitude", lon)
                                put("title", title)
                            }
                        )
                    )
                }
            )
        }
    )

    val artistBioCardItem = CatalogItem(
        name = "ArtistBioCard",
        compose = { componentId, data, _, dataContext, onEvent ->
            val artistName = data["artistName"].asString(dataContext)
                ?: data["name"].asString(dataContext)
                ?: ""
            val bio = data["bio"].asString(dataContext) ?: ""
            val avatarUrl = data["avatarUrl"].asString(dataContext)
            val activeYears = data["activeYears"].asString(dataContext)
            val styles = data["styles"].asStringList(dataContext) ?: emptyList()

            val rawArtworks = data["artworks"]
            val artworks = when (rawArtworks) {
                is JsonArray -> rawArtworks.mapIndexed { idx, elem ->
                    val obj = elem as? JsonObject
                    val artId = obj?.get("id")?.jsonPrimitive?.contentOrNull ?: "art-$idx"
                    val artTitle = obj?.get("title")?.jsonPrimitive?.contentOrNull ?: ""
                    val artImage = obj?.get("imageUrl")?.jsonPrimitive?.contentOrNull ?: ""
                    NeonArtworkThumbnail(id = artId, title = artTitle, imageUrl = artImage)
                }
                else -> emptyList()
            }

            NeonArtistBioCard(
                artistName = artistName,
                bio = bio,
                avatarUrl = avatarUrl,
                activeYears = activeYears,
                styles = styles,
                artworks = artworks,
                onArtworkClick = { artId, title ->
                    onEvent(
                        ActionEvent(
                            name = "open_spot_detail",
                            surfaceId = "",
                            sourceComponentId = componentId,
                            timestamp = "",
                            context = buildJsonObject {
                                put("spotId", artId)
                                put("title", title)
                            }
                        )
                    )
                },
                onStyleClick = { style ->
                    onEvent(
                        ActionEvent(
                            name = "filter_by_style",
                            surfaceId = "",
                            sourceComponentId = componentId,
                            timestamp = "",
                            context = buildJsonObject {
                                put("style", style)
                            }
                        )
                    )
                }
            )
        }
    )

    val mapPinOverlayItem = CatalogItem(
        name = "MapPinOverlay",
        compose = { componentId, data, _, dataContext, onEvent ->
            val title = data["title"].asString(dataContext) ?: ""
            val centerLat = data["centerLatitude"].asDouble(dataContext)
                ?: data["latitude"].asDouble(dataContext)
                ?: 0.0
            val centerLon = data["centerLongitude"].asDouble(dataContext)
                ?: data["longitude"].asDouble(dataContext)
                ?: 0.0
            val zoomLevel = data["zoomLevel"].asDouble(dataContext)?.toFloat() ?: 14f

            val rawPins = data["pins"]
            val pins = when (rawPins) {
                is JsonArray -> rawPins.mapIndexed { idx, elem ->
                    val obj = elem as? JsonObject
                    val pId = obj?.get("id")?.jsonPrimitive?.contentOrNull ?: "pin-$idx"
                    val pTitle = obj?.get("title")?.jsonPrimitive?.contentOrNull ?: "Pin ${idx + 1}"
                    val pLat = obj?.get("latitude")?.jsonPrimitive?.doubleOrNull ?: 0.0
                    val pLon = obj?.get("longitude")?.jsonPrimitive?.doubleOrNull ?: 0.0
                    val pCategory = obj?.get("category")?.jsonPrimitive?.contentOrNull
                    NeonMapPin(id = pId, title = pTitle, latitude = pLat, longitude = pLon, category = pCategory)
                }
                else -> emptyList()
            }

            NeonMapPinOverlayWidget(
                title = title,
                centerLatitude = centerLat,
                centerLongitude = centerLon,
                zoomLevel = zoomLevel,
                pins = pins,
                onPinClick = { pinId, lat, lon, pinTitle ->
                    onEvent(
                        ActionEvent(
                            name = "view_on_map",
                            surfaceId = "",
                            sourceComponentId = componentId,
                            timestamp = "",
                            context = buildJsonObject {
                                put("spotId", pinId)
                                put("latitude", lat)
                                put("longitude", lon)
                                put("title", pinTitle)
                            }
                        )
                    )
                },
                onFocusCluster = { lat, lon, clusterTitle ->
                    onEvent(
                        ActionEvent(
                            name = "view_on_map",
                            surfaceId = "",
                            sourceComponentId = componentId,
                            timestamp = "",
                            context = buildJsonObject {
                                put("latitude", lat)
                                put("longitude", lon)
                                put("title", clusterTitle)
                            }
                        )
                    )
                }
            )
        }
    )

    return coreCatalog + Catalog.of(
        NEON_HYBRID_CATALOG_ID,
        spotCardItem,
        tourCardItem,
        artistBioCardItem,
        mapPinOverlayItem
    )
}

private fun JsonElement?.asString(dataContext: DataContext): String? = when (this) {
    null -> null
    is JsonPrimitive -> contentOrNull
    is JsonObject -> get("path")?.jsonPrimitive?.contentOrNull?.let { dataContext.getString(it) }
    else -> null
}

private fun JsonElement?.asDouble(dataContext: DataContext): Double? = when (this) {
    null -> null
    is JsonPrimitive -> doubleOrNull ?: contentOrNull?.toDoubleOrNull()
    is JsonObject -> get("path")?.jsonPrimitive?.contentOrNull?.let { dataContext.getNumber(it) }
    else -> null
}

private fun JsonElement?.asBoolean(dataContext: DataContext): Boolean? = when (this) {
    null -> null
    is JsonPrimitive -> booleanOrNull ?: (contentOrNull == "true")
    is JsonObject -> get("path")?.jsonPrimitive?.contentOrNull?.let { dataContext.getBoolean(it) }
    else -> null
}

private fun JsonElement?.asStringList(dataContext: DataContext): List<String>? = when (this) {
    null -> null
    is JsonArray -> mapNotNull { it.jsonPrimitive.contentOrNull }
    is JsonObject -> get("path")?.jsonPrimitive?.contentOrNull?.let { dataContext.getStringList(it) }
    else -> null
}

private fun JsonElement?.asPath(): String? = when (this) {
    is JsonObject -> get("path")?.jsonPrimitive?.contentOrNull
    else -> null
}
