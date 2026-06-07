package net.maiatoday.spotcache.feature.main

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val mainModule = module {
    viewModel { MainViewModel(get(), get(), get(), get()) }
}
