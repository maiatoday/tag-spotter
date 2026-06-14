package net.maiatoday.tagspotter.di

import net.maiatoday.tagspotter.BuildConfig
import net.maiatoday.tagspotter.core.settings.SecretsProvider
import net.maiatoday.tagspotter.core.settings.AndroidSecretsProvider
import org.koin.dsl.module

val secretsModule = module {
    single<SecretsProvider> { AndroidSecretsProvider(BuildConfig.GEMINI_API_KEY) }
}
