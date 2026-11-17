package net.maiatoday.tagspotter.feature.tagalong.di

import com.google.adk.kt.runners.Runner
import net.maiatoday.tagspotter.core.location.LocationProvider
import net.maiatoday.tagspotter.core.photo.PhotoProcessor
import net.maiatoday.tagspotter.core.sync.HybridSpotRepository
import net.maiatoday.tagspotter.feature.tagalong.ui.TagAlongViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val tagalongModule: Module = module {
    viewModel {
        TagAlongViewModel(
            runner = getOrNull<Runner>(),
            chatRepository = get(),
            locationProvider = getOrNull<LocationProvider>(),
            hybridSpotRepository = getOrNull<HybridSpotRepository>(),
            photoProcessor = getOrNull<PhotoProcessor>()
        )
    }
}
