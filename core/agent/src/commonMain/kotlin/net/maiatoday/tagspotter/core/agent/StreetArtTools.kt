package net.maiatoday.tagspotter.core.agent

import com.google.adk.kt.annotations.Param
import com.google.adk.kt.annotations.Tool
import com.google.adk.kt.tools.BaseTool
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.maiatoday.tagspotter.core.location.LocationProvider
import net.maiatoday.tagspotter.core.sync.HybridSpotRepository

@Serializable
data class SpotSummaryDto(
    val uuid: String,
    val title: String,
    val artist: String,
    val latitude: Double,
    val longitude: Double,
    val category: String,
    val tags: List<String>,
    val description: String,
    val imageUrl: String = ""
)

@Serializable
data class TourStopDto(
    val stopOrder: Int,
    val spotId: String,
    val title: String,
    val artist: String,
    val latitude: Double,
    val longitude: Double,
    val estimatedMinutes: Int,
    val description: String,
    val imageUrl: String = ""
)

@Serializable
data class TourPlanDto(
    val tourTitle: String,
    val estimatedDurationMinutes: Int,
    val totalDistanceMeters: Int,
    val stops: List<TourStopDto>
)

@Serializable
data class ArtistBioDto(
    val artistName: String,
    val knownStyles: List<String>,
    val muralCount: Int,
    val notableMurals: List<String>,
    val bioSummary: String
)

@Serializable
data class MuralIdentificationDto(
    val matched: Boolean,
    val spotId: String = "",
    val title: String = "",
    val artist: String = "",
    val confidenceScore: Double = 0.0,
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val imageUrl: String = ""
)

class StreetArtTools(
    private val hybridSpotRepository: HybridSpotRepository,
    private val locationProvider: LocationProvider? = null,
    private val json: Json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }
) {

    @Tool(
        name = "getNearbySpots",
        description = "Searches for urban street art, graffiti, murals, and tags near a specific GPS location."
    )
    suspend fun getNearbySpots(
        @Param(description = "Latitude of the center location") latitude: Double,
        @Param(description = "Longitude of the center location") longitude: Double,
        @Param(description = "Radius in meters to search within, defaults to 500") radiusMeters: Double? = null
    ): String {
        val effectiveRadiusMeters = radiusMeters ?: 500.0
        val radiusKm = effectiveRadiusMeters / 1000.0
        val spots = hybridSpotRepository.getNearbySpots(latitude, longitude, radiusKm)
        val summaries = spots.map { detail ->
            val primaryArtist = detail.spot.artists.firstOrNull()?.ifBlank { "Unknown Artist" } ?: "Unknown Artist"
            val derivedTitle = detail.spot.description.lines().firstOrNull()?.trim()?.take(40)?.ifBlank { "Untitled Mural" } ?: "Untitled Mural"
            SpotSummaryDto(
                uuid = detail.spot.uuid,
                title = derivedTitle,
                artist = primaryArtist,
                latitude = detail.spot.latitude,
                longitude = detail.spot.longitude,
                category = detail.spot.category,
                tags = detail.spot.tags,
                description = detail.spot.description,
                imageUrl = detail.images.firstOrNull()?.thumbnailPath ?: ""
            )
        }
        return json.encodeToString(summaries)
    }

    @Tool(
        name = "generateWalkingTour",
        description = "Generates a structured walking tour sequence of street art murals starting from a GPS location."
    )
    suspend fun generateWalkingTour(
        @Param(description = "Starting latitude coordinate") latitude: Double,
        @Param(description = "Starting longitude coordinate") longitude: Double,
        @Param(description = "Maximum number of mural stops, default 4") maxStops: Int? = null,
        @Param(description = "Target tour duration in minutes, default 45") maxMinutes: Int? = null
    ): String {
        val effectiveMaxStops = (maxStops ?: 4).coerceIn(1, 10)
        val effectiveMaxMinutes = (maxMinutes ?: 45).coerceIn(10, 180)
        val spots = hybridSpotRepository.getNearbySpots(latitude, longitude, radiusKm = 3.0)
            .take(effectiveMaxStops)
        val stops = spots.mapIndexed { index, detail ->
            val stopMinutes = ((index + 1) * (effectiveMaxMinutes / spots.size.coerceAtLeast(1))).coerceAtLeast(5)
            val primaryArtist = detail.spot.artists.firstOrNull()?.ifBlank { "Local Artist" } ?: "Local Artist"
            val derivedTitle = detail.spot.description.lines().firstOrNull()?.trim()?.take(40)?.ifBlank { "Mural Stop #${index + 1}" } ?: "Mural Stop #${index + 1}"
            TourStopDto(
                stopOrder = index + 1,
                spotId = detail.spot.uuid,
                title = derivedTitle,
                artist = primaryArtist,
                latitude = detail.spot.latitude,
                longitude = detail.spot.longitude,
                estimatedMinutes = stopMinutes,
                description = detail.spot.description.ifBlank { "Striking street art piece" },
                imageUrl = detail.images.firstOrNull()?.thumbnailPath ?: ""
            )
        }
        return json.encodeToString(
            TourPlanDto(
                tourTitle = "Urban Street Art Discovery",
                estimatedDurationMinutes = effectiveMaxMinutes,
                totalDistanceMeters = (stops.size * 350).coerceAtLeast(400),
                stops = stops
            )
        )
    }

    @Tool(
        name = "getArtistBio",
        description = "Retrieves background, artistic style, notable works, and bio for a specific street artist."
    )
    suspend fun getArtistBio(
        @Param(description = "Name of the artist or graffiti crew") artistName: String
    ): String {
        val allSpots = hybridSpotRepository.getAllSpots()
        val artistSpots = allSpots.filter { detail ->
            detail.spot.artists.any { it.contains(artistName, ignoreCase = true) }
        }
        val tags = artistSpots.flatMap { it.spot.tags }.distinct()
        val titles = artistSpots.map { detail ->
            detail.spot.description.lines().firstOrNull()?.trim()?.take(40)?.ifBlank { "Untitled Mural" } ?: "Untitled Mural"
        }.distinct()

        val bio = ArtistBioDto(
            artistName = artistName,
            knownStyles = if (tags.isNotEmpty()) tags else listOf("Contemporary Muralism", "Graffiti Art"),
            muralCount = artistSpots.size,
            notableMurals = titles.take(5),
            bioSummary = if (artistSpots.isNotEmpty()) {
                "$artistName is an urban artist with ${artistSpots.size} cataloged piece(s) in this city, known for styles including ${tags.take(3).joinToString(", ")}."
            } else {
                "$artistName is an active urban street artist known for impactful outdoor pieces."
            }
        )
        return json.encodeToString(bio)
    }

    @Tool(
        name = "identifyMuralByPhoto",
        description = "Matches and identifies a mural using visual descriptions, style tags, or nearby coordinates."
    )
    suspend fun identifyMuralByPhoto(
        @Param(description = "Visual description, distinctive features, colors, or subjects in the photo") photoDescription: String,
        @Param(description = "Optional latitude coordinate where the photo was taken") latitude: Double? = null,
        @Param(description = "Optional longitude coordinate where the photo was taken") longitude: Double? = null
    ): String {
        val lat = latitude ?: 0.0
        val lon = longitude ?: 0.0
        val candidates = if (lat != 0.0 && lon != 0.0) {
            hybridSpotRepository.getNearbySpots(lat, lon, radiusKm = 1.0)
        } else {
            hybridSpotRepository.getAllSpots()
        }

        val keywords = photoDescription.lowercase().split(Regex("[\\s,;.]+")).filter { it.length > 2 }
        val scored = candidates.map { spot ->
            var score = 0
            val text = "${spot.spot.description} ${spot.spot.artists.joinToString(" ")} ${spot.spot.tags.joinToString(" ")}".lowercase()
            keywords.forEach { kw ->
                if (text.contains(kw)) score += 1
            }
            spot to score
        }.sortedByDescending { it.second }

        val bestCandidate = scored.firstOrNull()
        return if (bestCandidate != null && bestCandidate.second > 0) {
            val bestMatch = bestCandidate.first
            val primaryArtist = bestMatch.spot.artists.firstOrNull()?.ifBlank { "Unknown Artist" } ?: "Unknown Artist"
            val derivedTitle = bestMatch.spot.description.lines().firstOrNull()?.trim()?.take(40)?.ifBlank { "Identified Mural" } ?: "Identified Mural"
            json.encodeToString(
                MuralIdentificationDto(
                    matched = true,
                    spotId = bestMatch.spot.uuid,
                    title = derivedTitle,
                    artist = primaryArtist,
                    confidenceScore = 0.85,
                    description = bestMatch.spot.description,
                    latitude = bestMatch.spot.latitude,
                    longitude = bestMatch.spot.longitude,
                    imageUrl = bestMatch.images.firstOrNull()?.thumbnailPath ?: ""
                )
            )
        } else {
            json.encodeToString(
                MuralIdentificationDto(
                    matched = false,
                    confidenceScore = 0.0,
                    description = "No matching mural found for description: $photoDescription"
                )
            )
        }
    }
}

expect fun StreetArtTools.toTools(): List<BaseTool>
