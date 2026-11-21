package net.maiatoday.tagspotter.core.a2ui.engine

import android.content.Context
import androidx.a2ui.compose.runtime.A2uiComponentProperties
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.a2ui.model.catalog.A2uiFunction
import androidx.a2ui.model.catalog.A2uiFunctionDefinition
import androidx.a2ui.model.catalog.A2uiFunctionReturnType
import androidx.a2ui.model.protocol.A2uiExecutionContext
import androidx.a2ui.model.schema.A2uiNumberSchema
import androidx.a2ui.model.schema.A2uiObjectSchema
import androidx.a2ui.model.schema.A2uiSchema
import androidx.a2ui.model.schema.A2uiStringSchema
import androidx.compose.material3.a2ui.catalog.MaterialA2uiBasicCatalogV1Defaults
import androidx.compose.material3.a2ui.catalog.materialA2uiBasicCatalogV1
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import coil3.compose.AsyncImage
import net.maiatoday.tagspotter.core.ui.components.NeonArtistBioCard
import net.maiatoday.tagspotter.core.ui.components.NeonArtworkThumbnail
import net.maiatoday.tagspotter.core.ui.components.NeonMapPin
import net.maiatoday.tagspotter.core.ui.components.NeonMapPinOverlayWidget
import net.maiatoday.tagspotter.core.ui.components.NeonSpotCard
import net.maiatoday.tagspotter.core.ui.components.NeonTourCard
import net.maiatoday.tagspotter.core.ui.components.NeonTourWaypoint

const val JETPACK_HYBRID_CATALOG_ID = "net.maiatoday.tagspotter.hybrid"
const val JETPACK_HYBRID_CATALOG_URI = "https://tagspotter.maiatoday.net/a2ui/neon_hybrid_catalog/v1/catalog.json"

/**
 * Creates the Android Jetpack A2UI Hybrid Catalog combining Material 3 Basic Catalog V1
 * (Card, Column, Row, Text, Button, ChoicePicker, etc.) with custom domain cards
 * (SpotCard, TourCard, ArtistBioCard, MapPinOverlay) and domain functions (focusMap, openSpotDetail).
 */
fun createJetpackHybridCatalog(
    context: Context? = null,
    catalogId: String = JETPACK_HYBRID_CATALOG_ID,
    onFocusMap: ((Double, Double, String?) -> Unit)? = null,
    onOpenSpotDetail: ((String) -> Unit)? = null
): A2uiCatalog {
    val basicCatalog = materialA2uiBasicCatalogV1(
        image = MaterialA2uiBasicCatalogV1Defaults.image { url, contentDescription, contentScale, modifier, onError ->
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier,
                onError = { state -> onError(state.result.throwable) }
            )
        },
        video = MaterialA2uiBasicCatalogV1Defaults.video { _, _, _ -> },
        audioPlayer = MaterialA2uiBasicCatalogV1Defaults.audioPlayer { _, _, _, _ -> },
        functions = if (context != null) {
            A2uiBasicCatalogV1.Functions(context)
        } else {
            object : A2uiBasicCatalogV1.Functions {
                override fun openUrl(url: String, executionContext: A2uiExecutionContext) {}
            }
        }
    )

    val customComponents = listOf(
        SpotCardA2uiComponent,
        TourCardA2uiComponent,
        ArtistBioCardA2uiComponent,
        MapPinOverlayA2uiComponent
    )

    val focusMapFunction = object : A2uiFunction {
        override val definition: A2uiFunctionDefinition = object : A2uiFunctionDefinition {
            override val name: String = "focusMap"
            override val description: String = "Focus the map on given coordinates"
            override val returnType: A2uiFunctionReturnType = A2uiFunctionReturnType.VOID
            override val argumentSchema: A2uiSchema = A2uiObjectSchema(
                properties = mapOf(
                    "latitude" to A2uiNumberSchema(description = "Target latitude"),
                    "longitude" to A2uiNumberSchema(description = "Target longitude"),
                    "title" to A2uiStringSchema(description = "Optional title")
                ),
                required = setOf("latitude", "longitude")
            )
        }

        override fun execute(args: Map<String, Any>, executionContext: A2uiExecutionContext): Any? {
            val lat = (args["latitude"] as? Number)?.toDouble() ?: 0.0
            val lon = (args["longitude"] as? Number)?.toDouble() ?: 0.0
            val title = args["title"] as? String
            onFocusMap?.invoke(lat, lon, title)
            return null
        }
    }

    val openSpotDetailFunction = object : A2uiFunction {
        override val definition: A2uiFunctionDefinition = object : A2uiFunctionDefinition {
            override val name: String = "openSpotDetail"
            override val description: String = "Open the detail view for a specific spot"
            override val returnType: A2uiFunctionReturnType = A2uiFunctionReturnType.VOID
            override val argumentSchema: A2uiSchema = A2uiObjectSchema(
                properties = mapOf(
                    "spotId" to A2uiStringSchema(description = "Unique ID of the spot")
                ),
                required = setOf("spotId")
            )
        }

        override fun execute(args: Map<String, Any>, executionContext: A2uiExecutionContext): Any? {
            val spotId = args["spotId"] as? String ?: ""
            onOpenSpotDetail?.invoke(spotId)
            return null
        }
    }

    val customFunctions = listOf(focusMapFunction, openSpotDetailFunction)

    return A2uiCatalog(
        catalogId = catalogId,
        components = basicCatalog.components + customComponents,
        functions = basicCatalog.functions + customFunctions,
        isInline = true
    )
}

object SpotCardA2uiComponent : A2uiComponent {
    override val name: String = "SpotCard"
    override val description: String = "Neon cyberpunk street art spot card"

    val spotIdProp = A2uiProperty.string("spotId", description = "Unique ID of the spot")
    val titleProp = A2uiProperty.dynamicString("title", required = true, description = "Title of the spot")
    val artistProp = A2uiProperty.dynamicString("artist", description = "Artist name")
    val categoryProp = A2uiProperty.dynamicString("category", description = "Spot category (e.g. graffiti, sculpture)")
    val imageUrlProp = A2uiProperty.dynamicString("imageUrl", description = "Spot photo URL")
    val tagsProp = A2uiProperty.stringList("tags", description = "List of tags")
    val distanceMetersProp = A2uiProperty.dynamicNumber("distanceMeters", description = "Distance in meters")
    val latitudeProp = A2uiProperty.dynamicNumber("latitude", description = "Latitude")
    val longitudeProp = A2uiProperty.dynamicNumber("longitude", description = "Longitude")
    val isStarredProp = A2uiProperty.dynamicBoolean("isStarred", description = "Whether spot is starred")
    val isSavedProp = A2uiProperty.dynamicBoolean("isSaved", description = "Whether spot is saved to local collection")

    override val properties: List<A2uiProperty<*>> = listOf(
        spotIdProp, titleProp, artistProp, categoryProp, imageUrlProp, tagsProp,
        distanceMetersProp, latitudeProp, longitudeProp, isStarredProp, isSavedProp
    )

    @Composable
    override fun A2uiComponentScope.Content(properties: A2uiComponentProperties, modifier: Modifier) {
        val title = properties.bind(titleProp) ?: ""
        val spotId = properties[spotIdProp] ?: ""
        val artist = properties.bind(artistProp)
        val category = properties.bind(categoryProp) ?: "graffiti"
        val imageUrl = properties.bind(imageUrlProp)
        val tags = properties[tagsProp] ?: emptyList()
        val distanceMeters = properties.bind(distanceMetersProp)?.toDouble()
        val latitude = properties.bind(latitudeProp)?.toDouble()
        val longitude = properties.bind(longitudeProp)?.toDouble()
        val isStarred = properties.bind(isStarredProp) ?: false
        val starUpdater = properties.bindUpdater(isStarredProp)
        val isSaved = properties.bind(isSavedProp) ?: false
        val savedUpdater = properties.bindUpdater(isSavedProp)

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
                starUpdater?.invoke(!isStarred)
                dispatchAction(
                    mapOf(
                        "event" to mapOf(
                            "name" to "toggle_star",
                            "context" to mapOf(
                                "spotId" to spotId,
                                "isStarred" to !isStarred
                            )
                        )
                    )
                )
            },
            isSaved = isSaved,
            onSaveSpot = {
                savedUpdater?.invoke(true)
                val contextMap = buildMap<String, Any> {
                    put("spotId", spotId)
                    put("title", title)
                    if (!artist.isNullOrBlank()) put("artist", artist)
                    put("category", category)
                    if (latitude != null) put("latitude", latitude)
                    if (longitude != null) put("longitude", longitude)
                    if (!imageUrl.isNullOrBlank()) put("imageUrl", imageUrl)
                    put("tags", tags)
                }
                dispatchAction(
                    mapOf(
                        "event" to mapOf(
                            "name" to "save_spot",
                            "context" to contextMap
                        )
                    )
                )
            },
            onViewOnMap = { lat, lon, t ->
                dispatchAction(
                    mapOf(
                        "event" to mapOf(
                            "name" to "view_on_map",
                            "context" to mapOf(
                                "latitude" to lat,
                                "longitude" to lon,
                                "title" to t,
                                "spotId" to spotId
                            )
                        )
                    )
                )
            },
            onCardClick = {
                dispatchAction(
                    mapOf(
                        "event" to mapOf(
                            "name" to "open_spot_detail",
                            "context" to mapOf(
                                "spotId" to spotId
                            )
                        )
                    )
                )
            },
            modifier = modifier
        )
    }
}

object TourCardA2uiComponent : A2uiComponent {
    override val name: String = "TourCard"
    override val description: String = "Guided graffiti & street art tour card"

    val tourTitleProp = A2uiProperty.dynamicString("tourTitle", description = "Tour title")
    val titleProp = A2uiProperty.dynamicString("title", description = "Alternative tour title")
    val descriptionProp = A2uiProperty.dynamicString("description", description = "Tour description")
    val estimatedMinutesProp = A2uiProperty.dynamicNumber("estimatedMinutes", description = "Estimated duration in minutes")
    val totalDistanceKmProp = A2uiProperty.dynamicNumber("totalDistanceKm", description = "Total distance in km")
    val waypointsProp = A2uiProperty.anyList("waypoints", description = "List of tour stops/waypoints")

    override val properties: List<A2uiProperty<*>> = listOf(
        tourTitleProp, titleProp, descriptionProp, estimatedMinutesProp, totalDistanceKmProp, waypointsProp
    )

    @Composable
    override fun A2uiComponentScope.Content(properties: A2uiComponentProperties, modifier: Modifier) {
        val tourTitle = properties.bind(tourTitleProp) ?: properties.bind(titleProp) ?: ""
        val description = properties.bind(descriptionProp) ?: ""
        val estimatedMinutes = properties.bind(estimatedMinutesProp)?.toInt() ?: 0
        val totalDistanceKm = properties.bind(totalDistanceKmProp)?.toDouble() ?: 0.0

        val rawWaypoints = properties[waypointsProp]
        val waypoints = (rawWaypoints as? List<*>)?.mapIndexedNotNull { idx, item ->
            val map = item as? Map<*, *> ?: return@mapIndexedNotNull null
            val lat = (map["latitude"] as? Number)?.toDouble() ?: 0.0
            val lon = (map["longitude"] as? Number)?.toDouble() ?: 0.0
            val wpTitle = map["title"]?.toString() ?: "Stop ${idx + 1}"
            val wpAddress = map["address"]?.toString()
            val wpImage = map["imageUrl"]?.toString()
            val wpCompleted = (map["completed"] as? Boolean) ?: false
            val wpIndex = (map["index"] as? Number)?.toInt() ?: (idx + 1)
            NeonTourWaypoint(
                index = wpIndex,
                title = wpTitle,
                address = wpAddress,
                latitude = lat,
                longitude = lon,
                imageUrl = wpImage,
                completed = wpCompleted
            )
        } ?: emptyList()

        NeonTourCard(
            tourTitle = tourTitle,
            description = description,
            estimatedMinutes = estimatedMinutes,
            totalDistanceKm = totalDistanceKm,
            waypoints = waypoints,
            onNavigateToStop = { stopIndex, lat, lon, title ->
                dispatchAction(
                    mapOf(
                        "event" to mapOf(
                            "name" to "navigate_to_stop",
                            "context" to mapOf(
                                "stopIndex" to stopIndex,
                                "latitude" to lat,
                                "longitude" to lon,
                                "title" to title
                            )
                        )
                    )
                )
            },
            modifier = modifier
        )
    }
}

object ArtistBioCardA2uiComponent : A2uiComponent {
    override val name: String = "ArtistBioCard"
    override val description: String = "Street artist profile card"

    val artistNameProp = A2uiProperty.dynamicString("artistName", description = "Artist name")
    val nameProp = A2uiProperty.dynamicString("name", description = "Alternative artist name")
    val bioProp = A2uiProperty.dynamicString("bio", description = "Short biography")
    val avatarUrlProp = A2uiProperty.dynamicString("avatarUrl", description = "Avatar image URL")
    val activeYearsProp = A2uiProperty.dynamicString("activeYears", description = "Years active")
    val stylesProp = A2uiProperty.stringList("styles", description = "List of art styles")
    val artworksProp = A2uiProperty.anyList("artworks", description = "List of featured artworks")

    override val properties: List<A2uiProperty<*>> = listOf(
        artistNameProp, nameProp, bioProp, avatarUrlProp, activeYearsProp, stylesProp, artworksProp
    )

    @Composable
    override fun A2uiComponentScope.Content(properties: A2uiComponentProperties, modifier: Modifier) {
        val artistName = properties.bind(artistNameProp) ?: properties.bind(nameProp) ?: ""
        val bio = properties.bind(bioProp) ?: ""
        val avatarUrl = properties.bind(avatarUrlProp)
        val activeYears = properties.bind(activeYearsProp)
        val styles = properties[stylesProp] ?: emptyList()

        val rawArtworks = properties[artworksProp]
        val artworks = (rawArtworks as? List<*>)?.mapIndexedNotNull { idx, item ->
            val map = item as? Map<*, *> ?: return@mapIndexedNotNull null
            val id = map["id"]?.toString() ?: "art-$idx"
            val title = map["title"]?.toString() ?: ""
            val image = map["imageUrl"]?.toString() ?: ""
            NeonArtworkThumbnail(id = id, title = title, imageUrl = image)
        } ?: emptyList()

        NeonArtistBioCard(
            artistName = artistName,
            bio = bio,
            avatarUrl = avatarUrl,
            activeYears = activeYears,
            styles = styles,
            artworks = artworks,
            onArtworkClick = { artId, title ->
                dispatchAction(
                    mapOf(
                        "event" to mapOf(
                            "name" to "open_spot_detail",
                            "context" to mapOf(
                                "spotId" to artId,
                                "title" to title
                            )
                        )
                    )
                )
            },
            onStyleClick = { style ->
                dispatchAction(
                    mapOf(
                        "event" to mapOf(
                            "name" to "filter_by_style",
                            "context" to mapOf(
                                "style" to style
                            )
                        )
                    )
                )
            },
            modifier = modifier
        )
    }
}

object MapPinOverlayA2uiComponent : A2uiComponent {
    override val name: String = "MapPinOverlay"
    override val description: String = "Interactive map pin cluster overlay"

    val titleProp = A2uiProperty.dynamicString("title", description = "Overlay title")
    val centerLatitudeProp = A2uiProperty.dynamicNumber("centerLatitude", description = "Center latitude")
    val latitudeProp = A2uiProperty.dynamicNumber("latitude", description = "Alternative latitude")
    val centerLongitudeProp = A2uiProperty.dynamicNumber("centerLongitude", description = "Center longitude")
    val longitudeProp = A2uiProperty.dynamicNumber("longitude", description = "Alternative longitude")
    val zoomLevelProp = A2uiProperty.dynamicNumber("zoomLevel", description = "Map zoom level")
    val pinsProp = A2uiProperty.anyList("pins", description = "Map pins")

    override val properties: List<A2uiProperty<*>> = listOf(
        titleProp, centerLatitudeProp, latitudeProp, centerLongitudeProp, longitudeProp, zoomLevelProp, pinsProp
    )

    @Composable
    override fun A2uiComponentScope.Content(properties: A2uiComponentProperties, modifier: Modifier) {
        val title = properties.bind(titleProp) ?: ""
        val centerLat = properties.bind(centerLatitudeProp)?.toDouble() ?: properties.bind(latitudeProp)?.toDouble() ?: 0.0
        val centerLon = properties.bind(centerLongitudeProp)?.toDouble() ?: properties.bind(longitudeProp)?.toDouble() ?: 0.0
        val zoomLevel = properties.bind(zoomLevelProp)?.toFloat() ?: 14f

        val rawPins = properties[pinsProp]
        val pins = (rawPins as? List<*>)?.mapIndexedNotNull { idx, item ->
            val map = item as? Map<*, *> ?: return@mapIndexedNotNull null
            val id = map["id"]?.toString() ?: "pin-$idx"
            val pinTitle = map["title"]?.toString() ?: "Pin ${idx + 1}"
            val lat = (map["latitude"] as? Number)?.toDouble() ?: 0.0
            val lon = (map["longitude"] as? Number)?.toDouble() ?: 0.0
            val cat = map["category"]?.toString()
            NeonMapPin(id = id, title = pinTitle, latitude = lat, longitude = lon, category = cat)
        } ?: emptyList()

        NeonMapPinOverlayWidget(
            title = title,
            centerLatitude = centerLat,
            centerLongitude = centerLon,
            zoomLevel = zoomLevel,
            pins = pins,
            onPinClick = { pinId, lat, lon, pinTitle ->
                dispatchAction(
                    mapOf(
                        "event" to mapOf(
                            "name" to "view_on_map",
                            "context" to mapOf(
                                "spotId" to pinId,
                                "latitude" to lat,
                                "longitude" to lon,
                                "title" to pinTitle
                            )
                        )
                    )
                )
            },
            onFocusCluster = { lat, lon, clusterTitle ->
                dispatchAction(
                    mapOf(
                        "event" to mapOf(
                            "name" to "view_on_map",
                            "context" to mapOf(
                                "latitude" to lat,
                                "longitude" to lon,
                                "title" to clusterTitle
                            )
                        )
                    )
                )
            },
            modifier = modifier
        )
    }
}
