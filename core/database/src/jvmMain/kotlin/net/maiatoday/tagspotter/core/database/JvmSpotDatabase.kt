package net.maiatoday.tagspotter.core.database

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

actual fun getDatabaseBuilder(ctx: Any?): RoomDatabase.Builder<SpotDatabase> {
    val dbFile = File(System.getProperty("java.io.tmpdir"), "spot_database.db")
    return Room.databaseBuilder<SpotDatabase>(
        name = dbFile.absolutePath
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

actual fun epochMillis(): Long = System.currentTimeMillis()
