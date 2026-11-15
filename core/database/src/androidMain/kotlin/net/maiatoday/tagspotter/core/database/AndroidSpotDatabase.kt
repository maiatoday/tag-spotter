package net.maiatoday.tagspotter.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun getDatabaseBuilder(ctx: Any?): RoomDatabase.Builder<SpotDatabase> {
    val context = ctx as? Context ?: throw IllegalArgumentException("Context must be provided on Android")
    val dbFile = context.getDatabasePath("spot_database")
    return Room.databaseBuilder<SpotDatabase>(
        context = context.applicationContext,
        name = dbFile.absolutePath
    )
}

actual fun getTagAlongDatabaseBuilder(ctx: Any?): RoomDatabase.Builder<TagAlongDatabase> {
    val context = ctx as? Context ?: throw IllegalArgumentException("Context must be provided on Android")
    val dbFile = context.getDatabasePath("tagalong_database")
    return Room.databaseBuilder<TagAlongDatabase>(
        context = context.applicationContext,
        name = dbFile.absolutePath
    )
}

actual val platformDatabaseModule: Module = module {
    single {
        getDatabaseBuilder(androidContext())
            .setDriver(BundledSQLiteDriver())
            .configureSpotDatabase()
            .fallbackToDestructiveMigration(true)
            .build()
    }
    single { get<SpotDatabase>().spotDao() }
    single<SpotRepository> { LocalSpotRepository(get(), get()) }

    single {
        getTagAlongDatabaseBuilder(androidContext())
            .setDriver(BundledSQLiteDriver())
            .fallbackToDestructiveMigration(true)
            .build()
    }
    single { get<TagAlongDatabase>().chatDao() }
    single<ChatRepository> { RoomChatRepository(get()) }
}

actual fun epochMillis(): Long = System.currentTimeMillis()
