package net.maiatoday.tagspotter.core.settings

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FakeSettingsRepositoryTest {

    @Test
    fun testPhotographerNameFlowWithTurbine() = runTest {
        val repo = FakeSettingsRepository("Initial Name", "Initial City")

        repo.photographerName.test {
            assertEquals("Initial Name", awaitItem())

            repo.updatePhotographerName("New Photographer")
            assertEquals("New Photographer", awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun testHomeCityFlowWithTurbine() = runTest {
        val repo = FakeSettingsRepository("Initial Name", "Milan")

        repo.homeCity.test {
            assertEquals("Milan", awaitItem())

            repo.updateHomeCity("London")
            assertEquals("London", awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun testPreferencesFlowsWithTurbine() = runTest {
        val repo = FakeSettingsRepository()

        repo.showTestData.test {
            assertFalse(awaitItem())

            repo.updateShowTestData(true)
            assertTrue(awaitItem())

            cancelAndConsumeRemainingEvents()
        }

        repo.darkMapEnabled.test {
            assertFalse(awaitItem())

            repo.updateDarkMapEnabled(true)
            assertTrue(awaitItem())

            cancelAndConsumeRemainingEvents()
        }

        repo.artistRecognitionEnabled.test {
            assertTrue(awaitItem())

            repo.updateArtistRecognitionEnabled(false)
            assertFalse(awaitItem())

            cancelAndConsumeRemainingEvents()
        }

        repo.geminiApiKey.test {
            assertEquals("", awaitItem())

            repo.updateGeminiApiKey("test_key_123")
            assertEquals("test_key_123", awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }
}
