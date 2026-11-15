package net.maiatoday.tagalong.desktop

import net.maiatoday.tagspotter.core.settings.SecretsProvider
import java.io.File
import java.util.Properties

class DesktopSecretsProvider : SecretsProvider {
    override fun getGeminiApiKey(): String {
        val envKey = System.getenv("GEMINI_API_KEY")
        if (!envKey.isNullOrEmpty()) return envKey

        val propertiesFile = File("local.properties")
        if (propertiesFile.exists()) {
            val props = Properties()
            propertiesFile.inputStream().use { props.load(it) }
            val key = props.getProperty("gemini.api.key")
            if (!key.isNullOrEmpty()) return key
        }
        return ""
    }
}
