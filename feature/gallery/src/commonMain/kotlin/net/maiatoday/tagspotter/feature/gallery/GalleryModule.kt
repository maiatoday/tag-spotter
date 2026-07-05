package net.maiatoday.tagspotter.feature.gallery

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val galleryModule = module {
    viewModel<GalleryViewModel> { GalleryViewModel(get(), get(), get(), get(), get(), get()) }
}
