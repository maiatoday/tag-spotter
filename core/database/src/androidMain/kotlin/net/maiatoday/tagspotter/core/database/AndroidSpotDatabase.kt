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

actual val platformDatabaseModule: Module = module {
    single {
        getDatabaseBuilder(androidContext())
            .setDriver(BundledSQLiteDriver())
            .fallbackToDestructiveMigration(true)
            .build()
    }
    single { get<SpotDatabase>().spotDao() }
    single<SpotRepository> { LocalSpotRepository(get(), get()) }
}

actual fun epochMillis(): Long = System.currentTimeMillis()
