package net.maiatoday.spotcache.core.settings

class AndroidSecretsProvider(private val apiKey: String) : SecretsProvider {
    override fun getGeminiApiKey(): String {
        return apiKey
    }
}