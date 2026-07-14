package net.maiatoday.tagspotter.core.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class SecretsProviderTest {

    @Test
    fun testFakeSecretsProviderReturnsApiKey() {
        val provider = FakeSecretsProvider("test-key-123")
        assertEquals("test-key-123", provider.getGeminiApiKey())

        provider.apiKey = "another-key"
        assertEquals("another-key", provider.getGeminiApiKey())
    }

    @Test
    fun testFakeSecretsProviderMutatesApiKey() {
        val provider = FakeSecretsProvider()
        assertEquals("", provider.getGeminiApiKey())
        provider.apiKey = "new-key"
        assertEquals("new-key", provider.getGeminiApiKey())
    }
}
