package com.example.tagspotter.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface SettingsRepository {
    val photographerName: Flow<String>
    suspend fun updatePhotographerName(name: String)
    val homeCity: Flow<String>
    suspend fun updateHomeCity(city: String)
}

class SharedPreferencesSettingsRepository(context: Context) : SettingsRepository {
    private val sharedPreferences = context.getSharedPreferences("tag_spotter_settings", Context.MODE_PRIVATE)
    
    private val _photographerName = MutableStateFlow(getSavedName())
    override val photographerName: Flow<String> = _photographerName.asStateFlow()

    private val _homeCity = MutableStateFlow(getSavedHomeCity())
    override val homeCity: Flow<String> = _homeCity.asStateFlow()

    private fun getSavedName(): String {
        return sharedPreferences.getString("photographer_name", "") ?: ""
    }

    private fun getSavedHomeCity(): String {
        return sharedPreferences.getString("home_city", "Milan") ?: "Milan"
    }

    override suspend fun updatePhotographerName(name: String) {
        sharedPreferences.edit().putString("photographer_name", name).apply()
        _photographerName.value = name
    }

    override suspend fun updateHomeCity(city: String) {
        sharedPreferences.edit().putString("home_city", city).apply()
        _homeCity.value = city
    }
}
