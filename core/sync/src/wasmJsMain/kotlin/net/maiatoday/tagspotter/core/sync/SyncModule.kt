package net.maiatoday.tagspotter.core.sync

import org.koin.core.module.Module
import org.koin.dsl.module

actual val syncModule: Module = module {
    single<AuthService> { WasmAuthService() }
    single<SyncManager> { WasmSyncManager(get()) }
}
