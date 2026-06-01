package net.maiatoday.tagspotter.ui.viewmodel

import net.maiatoday.tagspotter.MainDispatcherRule
import net.maiatoday.tagspotter.data.FakeSettingsRepository
import net.maiatoday.tagspotter.data.FakeSpotRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

        // Verify initial state
        assertEquals("Initial Photographer", viewModel.photographerName.value)
        assertEquals("Milan", viewModel.homeCity.value)
        assertFalse(viewModel.showTestData.value)

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
    }
}
