package com.example.tagspotter.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeSettingsRepository(initialName: String = "") : SettingsRepository {
    private val _photographerName = MutableStateFlow(initialName)
    override val photographerName: Flow<String> = _photographerName.asStateFlow()

    override suspend fun updatePhotographerName(name: String) {
        _photographerName.value = name
    }
}
