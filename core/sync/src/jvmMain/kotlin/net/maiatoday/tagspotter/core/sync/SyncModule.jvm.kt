package net.maiatoday.tagspotter.core.sync

import net.maiatoday.tagspotter.core.database.SpotRepository
import net.maiatoday.tagspotter.core.settings.SecureStorage

actual fun getJvmAuthService(secureStorage: SecureStorage): AuthService? {
    return if (JvmFirebaseConfig.hasCredentials()) {
        val client = JvmFirebaseClient(secureStorage)
        JvmAuthService(client, secureStorage)
    } else {
        null
    }
}

actual fun getJvmSyncManager(repository: SpotRepository, secureStorage: SecureStorage): SyncManager? {
    return if (JvmFirebaseConfig.hasCredentials()) {
        val client = JvmFirebaseClient(secureStorage)
        JvmSyncManager(repository, client)
    } else {
        null
    }
}
