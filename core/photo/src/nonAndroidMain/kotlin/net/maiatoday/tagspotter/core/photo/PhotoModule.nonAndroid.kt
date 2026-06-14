package net.maiatoday.tagspotter.core.photo

import org.koin.dsl.module
import org.koin.core.module.Module

actual val photoModule: Module = module {
    single<PhotoProcessor> { FakePhotoProcessor() }
}

actual fun resolveLocalPath(path: String): String = path

