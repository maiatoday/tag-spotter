package net.maiatoday.tagspotter.core.sync

import net.maiatoday.tagspotter.core.database.SpotRepository
import net.maiatoday.tagspotter.core.settings.SecureStorage

actual fun getJvmAuthService(secureStorage: SecureStorage): AuthService? = null
actual fun getJvmSyncManager(repository: SpotRepository, secureStorage: SecureStorage): SyncManager? = null
