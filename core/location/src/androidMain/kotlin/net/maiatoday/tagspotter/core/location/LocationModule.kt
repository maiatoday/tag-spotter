package net.maiatoday.tagspotter.core.location

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.koin.core.module.Module

actual val locationModule: Module = module {
    single<LocationProvider> { AndroidLocationProvider(androidContext()) }
    single<WearSyncManager> { AndroidWearSyncManager(androidContext()) }
}
