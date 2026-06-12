package net.maiatoday.tagspotter.core.settings

interface SecureStorage {
    fun getString(key: String, defaultValue: String): String
    fun putString(key: String, value: String)
}
