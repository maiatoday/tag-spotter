package net.maiatoday.tagspotter.core.database

import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipInputStream

actual fun unzip(zipFilePath: String, destDirPath: String) {
    val destDir = File(destDirPath)
    if (!destDir.exists()) {
        destDir.mkdirs()
    }
    ZipInputStream(FileInputStream(zipFilePath).buffered()).use { zis ->
        var entry = zis.nextEntry
        while (entry != null) {
            val outFile = File(destDir, entry.name)
            if (entry.isDirectory) {
                outFile.mkdirs()
            } else {
                outFile.parentFile?.mkdirs()
                outFile.outputStream().use { output ->
                    zis.copyTo(output)
                }
            }
            entry = zis.nextEntry
        }
    }
}

actual fun zip(sourceDirPath: String, zipFilePath: String) {
    val sourceDir = File(sourceDirPath)
    val zipFile = File(zipFilePath)
    java.io.FileOutputStream(zipFile).use { fos ->
        java.util.zip.ZipOutputStream(fos.buffered()).use { zos ->
            sourceDir.walkTopDown().forEach { file ->
                if (file.absolutePath != sourceDir.absolutePath) {
                    val entryName = file.absolutePath.removePrefix(sourceDir.absolutePath + File.separator)
                    if (file.isDirectory) {
                        zos.putNextEntry(java.util.zip.ZipEntry("$entryName/"))
                        zos.closeEntry()
                    } else {
                        zos.putNextEntry(java.util.zip.ZipEntry(entryName))
                        file.inputStream().use { input ->
                            input.copyTo(zos)
                        }
                        zos.closeEntry()
                    }
                }
            }
        }
    }
}
