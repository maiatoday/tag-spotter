package net.maiatoday.spotcache.feature.detail

import org.junit.jupiter.api.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify

class DetailModuleTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun verifyDetailModule() {
        detailModule.verify(
            extraTypes = listOf(
                net.maiatoday.spotcache.core.database.SpotRepository::class,
                net.maiatoday.spotcache.core.settings.SettingsRepository::class,
                net.maiatoday.spotcache.core.ai.AiRecognitionService::class,
                net.maiatoday.spotcache.core.settings.SecretsProvider::class
            )
        )
    }
}
