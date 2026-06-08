package net.maiatoday.tagspotter.di

import net.maiatoday.tagspotter.BuildConfig
import net.maiatoday.tagspotter.core.ai.aiModule
import net.maiatoday.tagspotter.core.database.databaseModule
import net.maiatoday.tagspotter.core.location.locationModule
import net.maiatoday.tagspotter.core.photo.photoModule
import net.maiatoday.tagspotter.core.settings.coreSettingsModule
import net.maiatoday.tagspotter.core.settings.SecretsProvider
import net.maiatoday.tagspotter.core.settings.AndroidSecretsProvider
import net.maiatoday.tagspotter.feature.detail.detailModule
import net.maiatoday.tagspotter.feature.gallery.galleryModule
import net.maiatoday.tagspotter.feature.main.mainModule
import net.maiatoday.tagspotter.feature.map.mapModule
import net.maiatoday.tagspotter.feature.settings.settingsModule
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
