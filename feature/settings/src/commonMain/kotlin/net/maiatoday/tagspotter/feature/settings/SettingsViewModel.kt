package net.maiatoday.tagspotter.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.maiatoday.tagspotter.core.settings.SettingsRepository
import net.maiatoday.tagspotter.core.database.SpotRepository
import net.maiatoday.tagspotter.core.sync.AuthService
import net.maiatoday.tagspotter.core.sync.SyncManager
import net.maiatoday.tagspotter.core.sync.FirebaseUserWrapper

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

import net.maiatoday.tagspotter.core.ai.AiRecognitionService

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val spotRepository: SpotRepository,
    private val authService: AuthService,
    private val syncManager: SyncManager,
    aiRecognitionService: AiRecognitionService
) : ViewModel() {
    val isAiSupported: Boolean = aiRecognitionService.isSupported

    init {
        viewModelScope.launch {
            authService.currentUserFlow.collect { user ->
                spotRepository.activeUid = user?.uid
            }
        }
    }

    val currentUser: StateFlow<FirebaseUserWrapper?> = authService.currentUserFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val hasOfflineSpots: StateFlow<Boolean> = spotRepository.getAllSpots()
        .map { spots -> spots.any { it.spot.ownerUid == null } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val isSyncing: StateFlow<Boolean> = syncManager.isSyncing
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun syncNow() {
        viewModelScope.launch {
            syncManager.syncNow()
        }
    }
    val isGoogleSignInSupported: Boolean = authService.isGoogleSignInSupported
    fun signInWithGoogle(idToken: String? = null, onResult: ((Result<Unit>) -> Unit)? = null) {
        viewModelScope.launch {
            val result = authService.signInWithGoogle(idToken)
            if (result.isFailure) {
                val exception = result.exceptionOrNull()
                println("Google Sign-In failed: ${exception?.message}")
                exception?.printStackTrace()
            }
            if (result.isSuccess) {
                authService.currentUserFlow.first()?.uid?.let { uid ->
                    syncManager.startRealtimeSync(uid)
                }
            }
            onResult?.invoke(result)
        }
    }

    fun signInWithEmailAndPassword(email: String, password: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = authService.signInWithEmailAndPassword(email, password)
            if (result.isSuccess) {
                authService.currentUserFlow.first()?.uid?.let { uid ->
                    syncManager.startRealtimeSync(uid)
                }
            }
            onResult(result)
        }
    }

    fun signUpWithEmailAndPassword(email: String, password: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = authService.signUpWithEmailAndPassword(email, password)
            if (result.isSuccess) {
                authService.currentUserFlow.first()?.uid?.let { uid ->
                    syncManager.startRealtimeSync(uid)
                }
            }
            onResult(result)
        }
    }

    fun sendPasswordResetEmail(email: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = authService.sendPasswordResetEmail(email)
            onResult(result)
        }
    }

    fun adoptOfflineSpots(backup: Boolean) {
        viewModelScope.launch {
            authService.currentUserFlow.first()?.uid?.let { uid ->
                spotRepository.adoptLocalSpots(uid, backup)
            }
        }
    }

    fun signOut(clearCache: Boolean = false) {
        viewModelScope.launch {
            val uid = authService.currentUserFlow.first()?.uid
            if (uid != null && clearCache) {
                spotRepository.clearUserCache(uid)
            }
            authService.signOut()
            syncManager.stopRealtimeSync()
            spotRepository.activeUid = null
        }
    }

    val photographerName: StateFlow<String> = settingsRepository.photographerName
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    val homeCity: StateFlow<String> = settingsRepository.homeCity
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Milan"
        )

    val showTestData: StateFlow<Boolean> = settingsRepository.showTestData
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )



    val darkMapEnabled: StateFlow<Boolean> = settingsRepository.darkMapEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun updatePhotographerName(name: String) {
        viewModelScope.launch {
            settingsRepository.updatePhotographerName(name)
        }
    }

    fun updateHomeCity(city: String) {
        viewModelScope.launch {
            settingsRepository.updateHomeCity(city)
        }
    }

    fun updateShowTestData(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShowTestData(show)
            if (show) {
                spotRepository.loadTestData()
            } else {
                spotRepository.unloadTestData()
            }
        }
    }



    val artistRecognitionEnabled: StateFlow<Boolean> = settingsRepository.artistRecognitionEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    fun updateDarkMapEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateDarkMapEnabled(enabled)
        }
    }

    fun updateArtistRecognitionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateArtistRecognitionEnabled(enabled)
        }
    }
}
