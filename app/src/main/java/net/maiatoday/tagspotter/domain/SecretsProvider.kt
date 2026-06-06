package net.maiatoday.tagspotter.domain

interface SecretsProvider {
    fun getGeminiApiKey(): String
}
