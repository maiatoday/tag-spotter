package net.maiatoday.spotcache.core.location

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val locationModule = module {
    single<LocationProvider> { AndroidLocationProvider(androidContext()) }
}
