package net.maiatoday.tagspotter.core.settings

import com.russhwolf.settings.StorageSettings
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformSettingsModule: Module = module {
    single<SecureStorage> { SettingsSecureStorage(StorageSettings()) }
    single<SettingsRepository> { WasmSettingsRepository(get(), StorageSettings()) }
}
