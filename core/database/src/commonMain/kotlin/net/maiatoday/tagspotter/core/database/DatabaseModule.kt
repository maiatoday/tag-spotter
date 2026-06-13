package net.maiatoday.tagspotter.core.database

import org.koin.core.module.Module
import org.koin.dsl.module

val databaseModule = module {
    includes(platformDatabaseModule)
}

expect val platformDatabaseModule: Module

expect fun epochMillis(): Long
