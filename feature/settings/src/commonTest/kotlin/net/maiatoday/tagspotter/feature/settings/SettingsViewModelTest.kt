package net.maiatoday.tagspotter.feature.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.maiatoday.tagspotter.core.settings.FakeSettingsRepository
import net.maiatoday.tagspotter.core.database.FakeSpotRepository
import net.maiatoday.tagspotter.core.sync.AuthService
import net.maiatoday.tagspotter.core.sync.SyncManager
import net.maiatoday.tagspotter.core.sync.FirebaseUserWrapper
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNull

class FakeAuthService : AuthService {
    private val _currentUserFlow = MutableStateFlow<FirebaseUserWrapper?>(null)
    override val currentUserFlow = _currentUserFlow.asStateFlow()
    override val isGoogleSignInSupported: Boolean = true

    var signInWithGoogleCalled = false
    var signInWithEmailCalled = false
    var signUpWithEmailCalled = false
    var signOutCalled = false

    var shouldSucceed = true

    override suspend fun signInWithGoogle(idToken: String?): Result<Unit> {
        signInWithGoogleCalled = true
        return if (shouldSucceed) {
            _currentUserFlow.value = FirebaseUserWrapper("google_uid", "google@test.com", "Google User")
            Result.success(Unit)
        } else {
            Result.failure(Exception("Google Sign-In failed"))
        }
    }

    override suspend fun signInWithEmailAndPassword(email: String, password: String): Result<Unit> {
        signInWithEmailCalled = true
        return if (shouldSucceed) {
            _currentUserFlow.value = FirebaseUserWrapper("email_uid", email, "Email User")
            Result.success(Unit)
        } else {
            Result.failure(Exception("Email Sign-In failed"))
        }
    }

    override suspend fun signUpWithEmailAndPassword(email: String, password: String): Result<Unit> {
        signUpWithEmailCalled = true
        return if (shouldSucceed) {
            _currentUserFlow.value = FirebaseUserWrapper("email_uid", email, "Email User")
            Result.success(Unit)
        } else {
            Result.failure(Exception("Email Sign-Up failed"))
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return if (shouldSucceed) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Password reset failed"))
        }
    }

    override suspend fun signOut() {
        signOutCalled = true
        _currentUserFlow.value = null
    }

    fun simulateUserChange(user: FirebaseUserWrapper?) {
        _currentUserFlow.value = user
    }
}

class FakeSyncManager : SyncManager {
    private val _isSyncing = MutableStateFlow(false)
    override val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    var startRealtimeSyncCalledWith: String? = null
    var stopRealtimeSyncCalled = false
    var syncNowCalled = false

    override suspend fun syncNow() {
        syncNowCalled = true
    }

    override fun startRealtimeSync(userId: String) {
        startRealtimeSyncCalledWith = userId
    }

    override fun stopRealtimeSync() {
        stopRealtimeSyncCalled = true
    }

    override suspend fun sharePack(
        title: String,
        description: String,
        authorName: String,
        spots: List<net.maiatoday.tagspotter.core.model.SpotDetails>
    ): String {
        return "MOCK_CODE"
    }

    override suspend fun importPackByCode(code: String): net.maiatoday.tagspotter.core.model.SharedPack {
        return net.maiatoday.tagspotter.core.model.SharedPack(
            packId = code,
            title = "Mock Pack",
            authorName = "Mock Author",
            description = "Mock Description",
            spots = emptyList()
        )
    }

    override suspend fun saveImportedPack(sharedPack: net.maiatoday.tagspotter.core.model.SharedPack) {
        // No-op
    }

    fun setSyncing(syncing: Boolean) {
        _isSyncing.value = syncing
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val settingsRepository = FakeSettingsRepository("Initial Photographer", "Milan")
    private val spotRepository = FakeSpotRepository()
    private val authService = FakeAuthService()
    private val syncManager = FakeSyncManager()

    @Test
    fun getAndSetPreferencesWorkCorrectly() = runTest {
        val viewModel = SettingsViewModel(settingsRepository, spotRepository, authService, syncManager)

        // Collect StateFlows in backgroundScope to trigger WhileSubscribed updates
        backgroundScope.launch(testDispatcher) {
            viewModel.photographerName.collect {}
        }
        backgroundScope.launch(testDispatcher) {
            viewModel.homeCity.collect {}
        }
        backgroundScope.launch(testDispatcher) {
            viewModel.showTestData.collect {}
        }
        backgroundScope.launch(testDispatcher) {
            viewModel.darkMapEnabled.collect {}
        }

        // Verify initial state
        assertEquals("Initial Photographer", viewModel.photographerName.value)
        assertEquals("Milan", viewModel.homeCity.value)
        assertFalse(viewModel.showTestData.value)
        assertFalse(viewModel.darkMapEnabled.value)

        // Update photographer name
        viewModel.updatePhotographerName("New Photographer")
        assertEquals("New Photographer", viewModel.photographerName.value)

        // Update home city
        viewModel.updateHomeCity("London")
        assertEquals("London", viewModel.homeCity.value)

        // Toggle mock data on
        viewModel.updateShowTestData(true)
        assertTrue(viewModel.showTestData.value)

        // Toggle mock data off
        viewModel.updateShowTestData(false)
        assertFalse(viewModel.showTestData.value)

        // Toggle dark map off
        viewModel.updateDarkMapEnabled(false)
        assertFalse(viewModel.darkMapEnabled.value)

        // Toggle dark map on
        viewModel.updateDarkMapEnabled(true)
        assertTrue(viewModel.darkMapEnabled.value)
    }

    @Test
    fun authAndSyncIntegrationWorksCorrectly() = runTest {
        val viewModel = SettingsViewModel(settingsRepository, spotRepository, authService, syncManager)

        backgroundScope.launch(testDispatcher) {
            viewModel.currentUser.collect {}
        }
        backgroundScope.launch(testDispatcher) {
            viewModel.isSyncing.collect {}
        }

        // 1. Initial State
        assertNull(viewModel.currentUser.value)
        assertFalse(viewModel.isSyncing.value)

        // 2. Google Sign-In
        viewModel.signInWithGoogle()
        assertTrue(authService.signInWithGoogleCalled)
        assertEquals("google_uid", viewModel.currentUser.value?.uid)

        // 3. Sign Out
        viewModel.signOut()
        assertTrue(authService.signOutCalled)
        assertTrue(syncManager.stopRealtimeSyncCalled)
        assertNull(viewModel.currentUser.value)

        // 4. Email Sign-In
        var callbackSuccess = false
        viewModel.signInWithEmailAndPassword("test@user.com", "password") { result ->
            callbackSuccess = result.isSuccess
        }
        assertTrue(authService.signInWithEmailCalled)
        assertTrue(callbackSuccess)
        assertEquals("email_uid", viewModel.currentUser.value?.uid)
        assertEquals("email_uid", syncManager.startRealtimeSyncCalledWith)

        // 5. Syncing Reactive State
        assertFalse(viewModel.isSyncing.value)
        syncManager.setSyncing(true)
        assertTrue(viewModel.isSyncing.value)
    }

    @Test
    fun forcedSyncTriggersSyncNow() = runTest {
        val viewModel = SettingsViewModel(settingsRepository, spotRepository, authService, syncManager)
        assertFalse(syncManager.syncNowCalled)
        viewModel.syncNow()
        assertTrue(syncManager.syncNowCalled)
    }
}
