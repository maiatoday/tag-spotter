package net.maiatoday.tagspotter.data.service

import net.maiatoday.tagspotter.BuildConfig
import net.maiatoday.tagspotter.domain.SecretsProvider

class AndroidSecretsProvider : SecretsProvider {
    override fun getGeminiApiKey(): String {
        return BuildConfig.GEMINI_API_KEY
    }
}
