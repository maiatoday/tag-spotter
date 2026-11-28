package net.maiatoday.tagspotter.core.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.maiatoday.tagspotter.core.model.SharedPack
import net.maiatoday.tagspotter.core.model.SpotDetails

private val DefaultSyncErrorFlow: StateFlow<String?> = MutableStateFlow<String?>(null).asStateFlow()

data class BackupResult(
    val uploaded: Int = 0,
    val refreshedUrls: Int = 0,
    val failed: Int = 0,
    val skippedNoLocalFile: Int = 0,
    val attempted: Boolean = true
) {
    operator fun plus(other: BackupResult): BackupResult = BackupResult(
        uploaded = uploaded + other.uploaded,
        refreshedUrls = refreshedUrls + other.refreshedUrls,
        failed = failed + other.failed,
        skippedNoLocalFile = skippedNoLocalFile + other.skippedNoLocalFile,
        attempted = attempted && other.attempted
    )

    fun toUserMessage(): String = when {
        !attempted -> "Backup unavailable right now; please try again"
        uploaded > 0 && failed > 0 -> "Uploaded $uploaded photo(s); $failed failed"
        uploaded > 0 -> "Uploaded $uploaded photo(s) to cloud"
        refreshedUrls > 0 && failed == 0 -> "Refreshed $refreshedUrls cloud photo link(s)"
        failed > 0 -> "Failed to upload $failed photo(s)"
        skippedNoLocalFile > 0 -> "No local photo files found on this device to upload"
        else -> "Photos already backed up"
    }
}

interface SyncManager {
    val isSyncing: StateFlow<Boolean>
    val syncError: StateFlow<String?> get() = DefaultSyncErrorFlow
    fun clearSyncError() {}
    suspend fun syncNow()
    fun startRealtimeSync(userId: String)
    fun stopRealtimeSync()
    suspend fun deleteSpot(uuid: String)

    suspend fun forceBackupAllImages(): BackupResult = BackupResult()
    suspend fun forceBackupSpot(uuid: String): BackupResult = BackupResult()

    suspend fun sharePack(title: String, description: String, authorName: String, spots: List<SpotDetails>): String
    suspend fun importPackByCode(code: String): SharedPack
    suspend fun saveImportedPack(sharedPack: SharedPack)

    suspend fun fetchPublicSpots(
        minLat: Double = -90.0,
        maxLat: Double = 90.0,
        minLon: Double = -180.0,
        maxLon: Double = 180.0
    ): List<SpotDetails> = emptyList()

    suspend fun syncPublicSpots() {}
}
