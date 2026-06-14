package net.maiatoday.tagspotter.core.photo

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val photoModule = module {
    single<PhotoProcessor> { AndroidPhotoProcessor(androidContext()) }
}
