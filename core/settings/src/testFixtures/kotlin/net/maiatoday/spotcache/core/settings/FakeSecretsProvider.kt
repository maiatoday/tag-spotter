package net.maiatoday.spotcache.core.settings

class FakeSecretsProvider(var apiKey: String = "") : SecretsProvider {
    override fun getGeminiApiKey(): String = apiKey
}
