package com.example.tagspotter.ui

import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.tagspotter.MainActivity
import com.example.tagspotter.TagSpotterApplication
import com.example.tagspotter.data.Spot
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DetailScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testArtistEditOnRealAppDatabase() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val app = context.applicationContext as TagSpotterApplication
        val repository = app.repository
        
        // Insert a test spot into the real application database
        val spotId = repository.saveSpot(
            Spot(
                latitude = 12.34,
                longitude = 56.78,
                createdAt = System.currentTimeMillis(),
                description = "Compose Test Spot",
                tags = emptyList(),
                category = "graffiti",
                status = "active",
                artists = listOf("Artist X")
            ),
            imagePath = "/fake/path"
        )
        
        // Now set the content using the real app's repository (which DetailScreen will look up)
        composeTestRule.runOnUiThread {
            composeTestRule.setContent {
                DetailScreen(
                    spotId = spotId,
                    onBack = {}
                )
            }
        }
        
        composeTestRule.waitForIdle()

        // Verify "Artist X" is displayed
        composeTestRule.onNodeWithText("Artist X").assertExists()
        
        // Click edit artist icon (the one with description "Edit artists")
        composeTestRule.onNodeWithContentDescription("Edit artists").performClick()
        
        // Let's type "Artist Y" in the OutlinedTextField labeled "Artist Name"
        composeTestRule.onNodeWithText("Artist Name").performTextInput("Artist Y")
        
        // Click add button (the one with description "Add artist")
        composeTestRule.onNodeWithContentDescription("Add artist").performClick()
        
        // Click check icon (description "Save artists")
        composeTestRule.onNodeWithContentDescription("Save artists").performClick()
        
        composeTestRule.waitForIdle()

        // Verify "Artist Y" is displayed
        composeTestRule.onNodeWithText("Artist Y").assertExists()
    }

    @Test
    fun testStatusUpdateOnRealAppDatabase() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val app = context.applicationContext as TagSpotterApplication
        val repository = app.repository
        
        val spotId = repository.saveSpot(
            Spot(
                latitude = 12.34,
                longitude = 56.78,
                createdAt = System.currentTimeMillis(),
                description = "Status Test Spot",
                tags = emptyList(),
                category = "sculpture",
                status = "active",
                artists = emptyList(),
                photographer = "Jane Doe"
            ),
            imagePath = "/fake/path"
        )
        
        composeTestRule.runOnUiThread {
            composeTestRule.setContent {
                DetailScreen(
                    spotId = spotId,
                    onBack = {}
                )
            }
        }
        
        composeTestRule.waitForIdle()

        // Verify initial status is active by looking for "Mark as Erased" button
        composeTestRule.onNodeWithText("Mark as Erased").assertExists()
        
        // Click "Mark as Erased" button
        composeTestRule.onNodeWithText("Mark as Erased").performClick()
        
        composeTestRule.waitForIdle()

        // Verify status changed to erased by looking for "Mark Active" button
        composeTestRule.onNodeWithText("Mark Active").assertExists()
    }
}
