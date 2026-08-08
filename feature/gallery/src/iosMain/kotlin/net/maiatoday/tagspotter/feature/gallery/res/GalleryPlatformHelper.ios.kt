package net.maiatoday.tagspotter.feature.gallery.res

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import net.maiatoday.tagspotter.core.model.SpotDetails
import net.maiatoday.tagspotter.core.database.MultiplatformPackExporter
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import platform.UIKit.*
import platform.UniformTypeIdentifiers.UTType
import platform.darwin.NSObject
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
class IosGalleryPlatformHelper : GalleryPlatformHelper {
    override fun showToast(message: String) {
        println("Toast: $message")
    }

    private class DocumentPickerDelegate(
        private val onPicked: (String) -> Unit
    ) : NSObject(), UIDocumentPickerDelegateProtocol {
        override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
            val url = didPickDocumentsAtURLs.firstOrNull() as? platform.Foundation.NSURL
            url?.path?.let { onPicked(it) }
        }
    }

    @Composable
    override fun rememberLauncher(
        onExportReady: (uriString: String) -> Unit
    ): () -> Unit {
        return remember {
            {
                val tempDir = getCacheDir()
                val tempFile = "$tempDir/spots_export.ts_pack"
                onExportReady(tempFile)
            }
        }
    }

    override fun exportPack(
        uriString: String,
        spots: List<SpotDetails>,
        minRating: Int,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        try {
            MultiplatformPackExporter.exportPack(
                spots = spots,
                destZipFilePath = uriString,
                cacheDir = getCacheDir(),
                minRating = minRating
            )
            
            val url = platform.Foundation.NSURL.fileURLWithPath(uriString)
            val activityController = UIActivityViewController(
                activityItems = listOf(url),
                applicationActivities = null
            )
            val window = UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow
            val rootViewController = window?.rootViewController
            
            activityController.popoverPresentationController?.sourceView = rootViewController?.view
            
            rootViewController?.presentViewController(activityController, animated = true, completion = null)
            
            onSuccess()
        } catch (e: Exception) {
            onError(e)
        }
    }

    override fun getRoute(spots: List<SpotDetails>) {
        println("Routing on iOS. Spot count: ${spots.size}")
    }

    override fun shareKml(spots: List<SpotDetails>) {
        println("Sharing KML on iOS. Spot count: ${spots.size}")
    }

    @Composable
    override fun rememberImportLauncher(
        onPackPicked: (pathString: String) -> Unit
    ): () -> Unit {
        val delegate = remember {
            DocumentPickerDelegate { path ->
                onPackPicked(path)
            }
        }
        return remember {
            {
                val picker = UIDocumentPickerViewController(
                    forOpeningContentTypes = listOfNotNull(
                        UTType.typeWithIdentifier("public.zip-archive"),
                        UTType.typeWithFilenameExtension("ts_pack")
                    ),
                    asCopy = true
                )
                picker.delegate = delegate
                val window = UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow
                val rootViewController = window?.rootViewController
                rootViewController?.presentViewController(picker, animated = true, completion = null)
            }
        }
    }

    override fun getFilesDir(): String {
        val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null
        )
        return documentDirectory?.path ?: ""
    }

    override fun getCacheDir(): String {
        val cacheDirectory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSCachesDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null
        )
        return cacheDirectory?.path ?: ""
    }
}

@Composable
actual fun rememberGalleryPlatformHelper(): GalleryPlatformHelper {
    return remember { IosGalleryPlatformHelper() }
}
