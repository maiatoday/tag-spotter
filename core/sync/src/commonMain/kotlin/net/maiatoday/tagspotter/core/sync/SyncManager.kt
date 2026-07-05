package net.maiatoday.tagspotter.core.sync

import kotlinx.coroutines.flow.StateFlow
import net.maiatoday.tagspotter.core.model.SharedPack
import net.maiatoday.tagspotter.core.model.SpotDetails

interface SyncManager {
    val isSyncing: StateFlow<Boolean>
    suspend fun syncNow()
    fun startRealtimeSync(userId: String)
    fun stopRealtimeSync()

    suspend fun sharePack(title: String, description: String, authorName: String, spots: List<SpotDetails>): String
    suspend fun importPackByCode(code: String): SharedPack
    suspend fun saveImportedPack(sharedPack: SharedPack)
}
