package net.maiatoday.tagspotter.core.location

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val locationModule = module {
    single<LocationProvider> { AndroidLocationProvider(androidContext()) }
    single<GeofenceService> { AndroidGeofenceService(androidContext()) }
}
