package net.maiatoday.tagspotter.core.photo

import org.koin.dsl.module
import org.koin.core.module.Module

actual val photoModule: Module = module {
    single<PhotoProcessor> { RealPhotoProcessor() }
}
