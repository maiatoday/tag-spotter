package net.maiatoday.tagspotter.feature.detail

import kotlin.test.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify
import net.maiatoday.tagspotter.core.database.SpotRepository
import net.maiatoday.tagspotter.core.settings.SettingsRepository
import net.maiatoday.tagspotter.core.ai.AiRecognitionService
import net.maiatoday.tagspotter.core.settings.SecretsProvider
import net.maiatoday.tagspotter.core.location.WearSyncManager
import net.maiatoday.tagspotter.core.sync.SyncManager

class DetailModuleTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun verifyDetailModule() {
        detailModule.verify(
            extraTypes = listOf(
                SpotRepository::class,
                SettingsRepository::class,
                AiRecognitionService::class,
                SecretsProvider::class,
                WearSyncManager::class,
                SyncManager::class
            )
        )
    }
}
