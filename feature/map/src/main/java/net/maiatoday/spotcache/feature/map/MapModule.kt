package net.maiatoday.spotcache.feature.map

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val mapModule = module {
    viewModel { MapViewModel(get(), get()) }
}
