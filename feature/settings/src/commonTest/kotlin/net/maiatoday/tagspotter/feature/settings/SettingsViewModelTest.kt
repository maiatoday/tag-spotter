package net.maiatoday.tagspotter.feature.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.maiatoday.tagspotter.core.settings.FakeSettingsRepository
import net.maiatoday.tagspotter.core.database.FakeSpotRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    @Test
    fun getAndSetPreferencesWorkCorrectly() = runTest {
        val viewModel = SettingsViewModel(settingsRepository, spotRepository)

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
}
