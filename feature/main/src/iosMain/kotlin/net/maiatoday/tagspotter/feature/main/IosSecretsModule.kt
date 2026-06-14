package net.maiatoday.tagspotter.feature.main

import net.maiatoday.tagspotter.core.settings.SecretsProvider
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

class IosSecretsProvider : SecretsProvider {
    override fun getGeminiApiKey(): String {
        return NSUserDefaults.standardUserDefaults.stringForKey("gemini_api_key") ?: ""
    }
}

fun createIosSecretsModule(): Module = module {
    single<SecretsProvider> { IosSecretsProvider() }
}
