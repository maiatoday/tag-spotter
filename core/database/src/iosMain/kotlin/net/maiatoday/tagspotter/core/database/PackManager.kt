package net.maiatoday.tagspotter.core.database

import net.maiatoday.tagspotter.core.photo.PhotoProcessor
import net.maiatoday.tagspotter.core.model.SpotDetails

object PackManager {

    fun exportPack(spots: List<SpotDetails>, minRating: Int = 0) {
        // iOS export placeholder
    }

    suspend fun importPack(
        repository: SpotRepository,
        photoProcessor: PhotoProcessor,
        packFilePath: String,
        filesDir: String,
        cacheDir: String,
        currentPhotographerName: String
    ): Int {
        return MultiplatformPackImporter.importPack(
            repository = repository,
            packFilePath = packFilePath,
            filesDir = filesDir,
            cacheDir = cacheDir,
            currentPhotographerName = currentPhotographerName,
            createThumbnail = { path ->
                photoProcessor.createThumbnailFromFile(path)
            }
        )
    }
}
