package net.maiatoday.tagspotter.feature.main

import net.maiatoday.tagspotter.core.ai.aiModule
import net.maiatoday.tagspotter.core.database.databaseModule
import net.maiatoday.tagspotter.core.location.locationModule
import net.maiatoday.tagspotter.core.photo.photoModule
import net.maiatoday.tagspotter.core.settings.coreSettingsModule
import net.maiatoday.tagspotter.feature.detail.detailModule
import net.maiatoday.tagspotter.feature.gallery.galleryModule
import net.maiatoday.tagspotter.feature.map.mapModule
import net.maiatoday.tagspotter.feature.settings.settingsModule
import org.koin.core.context.startKoin
import org.koin.core.module.Module

fun initKoin(platformModules: List<Module> = emptyList()) = startKoin {
    modules(
        listOf(
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
        ) + platformModules
    )
}
