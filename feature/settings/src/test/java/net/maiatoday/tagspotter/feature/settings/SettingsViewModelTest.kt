package net.maiatoday.tagspotter.feature.settings

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.maiatoday.tagspotter.MainDispatcherRule
import net.maiatoday.tagspotter.core.settings.FakeSettingsRepository
import net.maiatoday.tagspotter.core.database.FakeSpotRepository
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

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
        Assert.assertEquals("Initial Photographer", viewModel.photographerName.value)
        Assert.assertEquals("Milan", viewModel.homeCity.value)
        Assert.assertFalse(viewModel.showTestData.value)
        Assert.assertFalse(viewModel.notificationsEnabled.value)
        Assert.assertFalse(viewModel.darkMapEnabled.value)

        // Update photographer name
        viewModel.updatePhotographerName("New Photographer")
        Assert.assertEquals("New Photographer", viewModel.photographerName.value)

        // Update home city
        viewModel.updateHomeCity("London")
        Assert.assertEquals("London", viewModel.homeCity.value)

        // Toggle mock data on
        viewModel.updateShowTestData(true)
        Assert.assertTrue(viewModel.showTestData.value)

        // Toggle mock data off
        viewModel.updateShowTestData(false)
        Assert.assertFalse(viewModel.showTestData.value)

        // Toggle notifications on
        viewModel.updateNotificationsEnabled(true)
        Assert.assertTrue(viewModel.notificationsEnabled.value)

        // Toggle notifications off
        viewModel.updateNotificationsEnabled(false)
        Assert.assertFalse(viewModel.notificationsEnabled.value)

        // Toggle dark map off
        viewModel.updateDarkMapEnabled(false)
        Assert.assertFalse(viewModel.darkMapEnabled.value)

        // Toggle dark map on
        viewModel.updateDarkMapEnabled(true)
        Assert.assertTrue(viewModel.darkMapEnabled.value)
    }
}