package net.maiatoday.tagspotter.core.sync

import net.maiatoday.tagspotter.core.database.SpotRepository
import net.maiatoday.tagspotter.core.settings.SecureStorage
import org.koin.core.module.Module
import org.koin.dsl.module

expect fun getJvmAuthService(secureStorage: SecureStorage): AuthService?
expect fun getJvmSyncManager(repository: SpotRepository, secureStorage: SecureStorage): SyncManager?

actual val syncModule: Module = module {
    single<AuthService> {
        val secureStorage = get<SecureStorage>()
        getJvmAuthService(secureStorage) ?: NonWebAuthService()
    }
    single<SyncManager> {
        val repository = get<SpotRepository>()
        val secureStorage = get<SecureStorage>()
        getJvmSyncManager(repository, secureStorage) ?: NonWebSyncManager(repository)
    }
}
