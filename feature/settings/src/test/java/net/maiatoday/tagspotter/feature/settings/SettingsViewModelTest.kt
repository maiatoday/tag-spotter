package net.maiatoday.tagspotter.feature.settings

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.maiatoday.tagspotter.MainDispatcherExtension
import net.maiatoday.tagspotter.core.settings.FakeSettingsRepository
import net.maiatoday.tagspotter.core.database.FakeSpotRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherExtension = MainDispatcherExtension()

    private val settingsRepository = FakeSettingsRepository("Initial Photographer", "Milan")
    private val spotRepository = FakeSpotRepository()

    @Test
    fun getAndSetPreferencesWorkCorrectly() = runTest {
        val viewModel = SettingsViewModel(settingsRepository, spotRepository)

        // Collect StateFlows in backgroundScope to trigger WhileSubscribed updates
        val collectJobName = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.photographerName.collect {}
        }
        val collectJobCity = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.homeCity.collect {}
        }
        val collectJobShowTest = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.showTestData.collect {}
        }
        val collectJobNotifications =
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.notificationsEnabled.collect {}
            }
        val collectJobDarkMap = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.darkMapEnabled.collect {}
        }

        // Verify initial state
        assertEquals("Initial Photographer", viewModel.photographerName.value)
        assertEquals("Milan", viewModel.homeCity.value)
        assertFalse(viewModel.showTestData.value)
        assertFalse(viewModel.notificationsEnabled.value)
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

        // Toggle notifications on
        viewModel.updateNotificationsEnabled(true)
        assertTrue(viewModel.notificationsEnabled.value)

        // Toggle notifications off
        viewModel.updateNotificationsEnabled(false)
        assertFalse(viewModel.notificationsEnabled.value)

        // Toggle dark map off
        viewModel.updateDarkMapEnabled(false)
        assertFalse(viewModel.darkMapEnabled.value)

        // Toggle dark map on
        viewModel.updateDarkMapEnabled(true)
        assertTrue(viewModel.darkMapEnabled.value)
    }
}