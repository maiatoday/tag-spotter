package net.maiatoday.tagspotter.core.settings

import net.maiatoday.tagspotter.BuildConfig

class AndroidSecretsProvider : SecretsProvider {
    override fun getGeminiApiKey(): String {
        return BuildConfig.GEMINI_API_KEY
    }
}