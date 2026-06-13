@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package net.maiatoday.tagspotter.core.database

import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDatabaseModule: Module = module {
    single<SpotRepository> { WasmSpotRepository() }
}

private fun jsTime(): Double = js("Date.now()")

actual fun epochMillis(): Long = jsTime().toLong()
