package net.maiatoday.tagspotter.core.database

expect fun unzip(zipFilePath: String, destDirPath: String)
expect fun zip(sourceDirPath: String, zipFilePath: String)
