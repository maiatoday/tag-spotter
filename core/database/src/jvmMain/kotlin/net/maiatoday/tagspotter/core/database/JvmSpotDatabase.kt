package net.maiatoday.tagspotter.core.database

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

actual fun getDatabaseBuilder(ctx: Any?): RoomDatabase.Builder<SpotDatabase> {
    val dir = File(System.getProperty("user.home"), ".tagspotter")
    if (!dir.exists()) {
        dir.mkdirs()
    }
    val dbFile = File(dir, "spot_database.db")
    return Room.databaseBuilder<SpotDatabase>(
        name = dbFile.absolutePath
    )
}

actual fun getTagAlongDatabaseBuilder(ctx: Any?): RoomDatabase.Builder<TagAlongDatabase> {
    val dir = File(System.getProperty("user.home"), ".tagspotter")
    if (!dir.exists()) {
        dir.mkdirs()
    }
    val dbFile = File(dir, "tagalong_database.db")
    return Room.databaseBuilder<TagAlongDatabase>(
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

    single {
        getTagAlongDatabaseBuilder()
            .setDriver(BundledSQLiteDriver())
            .fallbackToDestructiveMigration(true)
            .build()
    }
    single { get<TagAlongDatabase>().chatDao() }
    single<ChatRepository> { RoomChatRepository(get()) }
}

actual fun epochMillis(): Long = System.currentTimeMillis()
