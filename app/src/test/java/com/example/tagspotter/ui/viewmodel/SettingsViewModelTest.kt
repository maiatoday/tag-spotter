package com.example.tagspotter.ui.viewmodel

import com.example.tagspotter.MainDispatcherRule
import com.example.tagspotter.data.FakeSettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settingsRepository = FakeSettingsRepository("Initial Photographer", "Milan")

    @Test
    fun getAndSetPreferencesWorkCorrectly() = runTest {
        val viewModel = SettingsViewModel(settingsRepository)

        // Collect StateFlows in backgroundScope to trigger WhileSubscribed updates
        val collectJobName = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.photographerName.collect {}
        }
        val collectJobCity = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.homeCity.collect {}
        }

        // Verify initial state
        assertEquals("Initial Photographer", viewModel.photographerName.value)
        assertEquals("Milan", viewModel.homeCity.value)

        // Update photographer name
        viewModel.updatePhotographerName("New Photographer")
        assertEquals("New Photographer", viewModel.photographerName.value)

        // Update home city
        viewModel.updateHomeCity("London")
        assertEquals("London", viewModel.homeCity.value)
    }
}
