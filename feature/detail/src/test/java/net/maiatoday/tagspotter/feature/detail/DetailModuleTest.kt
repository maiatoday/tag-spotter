package net.maiatoday.tagspotter.feature.detail

import org.junit.jupiter.api.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify

class DetailModuleTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun verifyDetailModule() {
        detailModule.verify(
            extraTypes = listOf(
                net.maiatoday.tagspotter.core.database.SpotRepository::class,
                net.maiatoday.tagspotter.core.settings.SettingsRepository::class,
                net.maiatoday.tagspotter.core.ai.AiRecognitionService::class,
                net.maiatoday.tagspotter.core.settings.SecretsProvider::class,
                net.maiatoday.tagspotter.core.location.WearSyncManager::class
            )
        )
    }
}
