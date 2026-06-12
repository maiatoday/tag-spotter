package net.maiatoday.tagspotter.core.settings

import com.russhwolf.settings.Settings

class SettingsSecureStorage(private val settings: Settings) : SecureStorage {
    override fun getString(key: String, defaultValue: String): String {
        return settings.getString(key, defaultValue)
    }

    override fun putString(key: String, value: String) {
        settings.putString(key, value)
    }
}
