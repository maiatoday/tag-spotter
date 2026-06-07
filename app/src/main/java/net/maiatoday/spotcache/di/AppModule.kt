package net.maiatoday.spotcache.di

import net.maiatoday.spotcache.BuildConfig
import net.maiatoday.spotcache.core.ai.aiModule
import net.maiatoday.spotcache.core.database.databaseModule
import net.maiatoday.spotcache.core.location.locationModule
import net.maiatoday.spotcache.core.photo.photoModule
import net.maiatoday.spotcache.core.settings.coreSettingsModule
import net.maiatoday.spotcache.core.settings.SecretsProvider
import net.maiatoday.spotcache.core.settings.AndroidSecretsProvider
import net.maiatoday.spotcache.feature.detail.detailModule
import net.maiatoday.spotcache.feature.gallery.galleryModule
import net.maiatoday.spotcache.feature.main.mainModule
import net.maiatoday.spotcache.feature.map.mapModule
import net.maiatoday.spotcache.feature.settings.settingsModule
import org.koin.dsl.module

val secretsModule = module {
    single<SecretsProvider> { AndroidSecretsProvider(BuildConfig.GEMINI_API_KEY) }
}

val appModule = module {
    includes(
        secretsModule,
        databaseModule,
        coreSettingsModule,
        locationModule,
        photoModule,
        aiModule,
        mainModule,
        galleryModule,
        mapModule,
        detailModule,
        settingsModule
    )
}
