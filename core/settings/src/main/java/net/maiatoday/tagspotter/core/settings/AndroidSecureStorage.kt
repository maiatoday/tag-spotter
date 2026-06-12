package net.maiatoday.tagspotter.core.settings

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.core.content.edit

@Suppress("DEPRECATION")
class AndroidSecureStorage(private val context: Context) : SecureStorage {
    private val securePreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secure_tag_spotter_settings",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Throwable) {
        e.printStackTrace()
        context.getSharedPreferences("secure_tag_spotter_settings_fallback", Context.MODE_PRIVATE)
    }

    override fun getString(key: String, defaultValue: String): String {
        return securePreferences.getString(key, defaultValue) ?: defaultValue
    }

    override fun putString(key: String, value: String) {
        securePreferences.edit { putString(key, value) }
    }
}
