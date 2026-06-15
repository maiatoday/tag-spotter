package net.maiatoday.tagspotter.core.database

object IosZipHelper {
    var unzipCallback: ((zipFilePath: String, destDirPath: String) -> Boolean)? = null
    var zipCallback: ((sourceDirPath: String, destZipPath: String) -> Boolean)? = null
}

actual fun unzip(zipFilePath: String, destDirPath: String) {
    val success = IosZipHelper.unzipCallback?.invoke(zipFilePath, destDirPath) ?: false
    if (!success) {
        throw Exception("Zip extraction failed or unzipCallback not registered on iOS")
    }
}

actual fun zip(sourceDirPath: String, zipFilePath: String) {
    val success = IosZipHelper.zipCallback?.invoke(sourceDirPath, zipFilePath) ?: false
    if (!success) {
        throw Exception("Zip archiving failed or zipCallback not registered on iOS")
    }
}

actual val fileSystem: okio.FileSystem = okio.FileSystem.SYSTEM

