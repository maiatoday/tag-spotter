package net.maiatoday.tagspotter.core.settings

interface SecretsProvider {
    fun getGeminiApiKey(): String
}