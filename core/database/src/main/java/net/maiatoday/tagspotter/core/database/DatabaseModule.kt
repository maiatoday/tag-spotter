package net.maiatoday.tagspotter.core.database

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single { SpotDatabase.getDatabase(androidContext()) }
    single { get<SpotDatabase>().spotDao() }
    single<SpotRepository> { LocalSpotRepository(get(), get(), get()) }
}
