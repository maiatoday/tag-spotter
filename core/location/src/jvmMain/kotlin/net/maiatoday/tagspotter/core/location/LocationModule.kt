package net.maiatoday.tagspotter.core.location

import org.koin.dsl.module
import org.koin.core.module.Module

actual val locationModule: Module = module {
    single<LocationProvider> { JvmLocationProvider() }
    single<WearSyncManager> { NoOpWearSyncManager() }
}
