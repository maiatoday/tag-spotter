package net.maiatoday.spotcache.core.settings

interface SecretsProvider {
    fun getGeminiApiKey(): String
}