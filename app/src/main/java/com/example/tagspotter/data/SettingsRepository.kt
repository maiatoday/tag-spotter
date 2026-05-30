package com.example.tagspotter.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface SettingsRepository {
    val photographerName: Flow<String>
    suspend fun updatePhotographerName(name: String)
}

class SharedPreferencesSettingsRepository(context: Context) : SettingsRepository {
    private val sharedPreferences = context.getSharedPreferences("tag_spotter_settings", Context.MODE_PRIVATE)
    private val _photographerName = MutableStateFlow(getSavedName())
    override val photographerName: Flow<String> = _photographerName.asStateFlow()

    private fun getSavedName(): String {
        return sharedPreferences.getString("photographer_name", "") ?: ""
    }

    override suspend fun updatePhotographerName(name: String) {
        sharedPreferences.edit().putString("photographer_name", name).apply()
        _photographerName.value = name
    }
}
