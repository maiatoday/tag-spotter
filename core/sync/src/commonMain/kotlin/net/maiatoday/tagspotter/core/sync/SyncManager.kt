package net.maiatoday.tagspotter.core.sync

import kotlinx.coroutines.flow.StateFlow

interface SyncManager {
    val isSyncing: StateFlow<Boolean>
    suspend fun syncNow()
    fun startRealtimeSync(userId: String)
    fun stopRealtimeSync()
}
