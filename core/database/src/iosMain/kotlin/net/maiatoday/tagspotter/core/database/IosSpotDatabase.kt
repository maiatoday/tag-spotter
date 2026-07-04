@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package net.maiatoday.tagspotter.core.database

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual fun getDatabaseBuilder(ctx: Any?): RoomDatabase.Builder<SpotDatabase> {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null
    )
    val dbFilePath = documentDirectory?.path + "/spot_database.db"
    return Room.databaseBuilder<SpotDatabase>(
        name = dbFilePath
    )
}

actual val platformDatabaseModule: Module = module {
    single {
        getDatabaseBuilder()
            .setDriver(BundledSQLiteDriver())
            .configureSpotDatabase()
            .fallbackToDestructiveMigration(true)
            .build()
    }
    single { get<SpotDatabase>().spotDao() }
    single<SpotRepository> { LocalSpotRepository(get(), get()) }
}

actual fun epochMillis(): Long = platform.posix.time(null) * 1000L
