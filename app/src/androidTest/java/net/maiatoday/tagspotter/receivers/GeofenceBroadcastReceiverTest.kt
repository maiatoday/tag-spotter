package net.maiatoday.tagspotter.receivers

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.service.notification.StatusBarNotification
import androidx.core.os.BundleCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.maiatoday.tagspotter.data.Spot
import net.maiatoday.tagspotter.data.SpotDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GeofenceBroadcastReceiverTest {

    private lateinit var context: Context
    private lateinit var receiver: GeofenceBroadcastReceiver
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        receiver = GeofenceBroadcastReceiver()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Grant notification permission for test on Android 13+ for both packages
        val targetPackage = context.packageName
        val testPackage = InstrumentationRegistry.getInstrumentation().context.packageName
        
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand(
            "pm grant $targetPackage android.permission.POST_NOTIFICATIONS"
        )
        instrumentation.uiAutomation.executeShellCommand(
            "pm grant $testPackage android.permission.POST_NOTIFICATIONS"
        )
        // Small delay to ensure permission settings take effect
        Thread.sleep(200)
    }

    @Test
    fun testShowNotificationWithBitmapUsesBigPictureStyle() {
        // Create mock data
        val spot = Spot(
            id = 9999L,
            latitude = 12.34,
            longitude = 56.78,
            createdAt = System.currentTimeMillis(),
            description = "Test Spot Description",
            tags = listOf("stencil", "mural"),
            category = "graffiti",
            status = "active",
            artists = listOf("Banksy"),
            photographer = "Alice"
        )
        val spotDetails = SpotDetails(
            spot = spot,
            images = emptyList(),
            notes = emptyList()
        )

        // Create a test bitmap (e.g. 100x100 pixels)
        val testBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        // Call the internal method
        receiver.showNotification(context, spotDetails, 250, testBitmap)

        // Verify notification was posted and has correct attributes (wait up to 1 second)
        var myNotification: StatusBarNotification? = null
        for (i in 1..10) {
            val activeNotifications = notificationManager.activeNotifications
            myNotification = activeNotifications.firstOrNull { it.id == spot.id.toInt() }
            if (myNotification != null) break
            Thread.sleep(100)
        }
        
        assertNotNull("Notification should have been posted", myNotification)
        val notification = myNotification!!.notification
        
        // Verify title & basic properties
        assertEquals("Nearby Starred Spot!", notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString())
        
        // Verify BigPictureStyle was applied
        assertEquals(
            $$"android.app.Notification$BigPictureStyle",
            notification.extras.getString(Notification.EXTRA_TEMPLATE)
        )
        
        // Verify that the picture extra is populated
        val picture = BundleCompat.getParcelable(
            notification.extras,
            Notification.EXTRA_PICTURE,
            Bitmap::class.java
        )
        assertNotNull("Big picture bitmap should be present in notification extras", picture)
        
        // Cancel the notification after test
        notificationManager.cancel(spot.id.toInt())
    }

    @Test
    fun testShowNotificationWithoutBitmapDoesNotUseBigPictureStyle() {
        // Create mock data
        val spot = Spot(
            id = 8888L,
            latitude = 12.34,
            longitude = 56.78,
            createdAt = System.currentTimeMillis(),
            description = "Test Spot Description 2",
            tags = listOf("sticker"),
            category = "sculpture",
            status = "active",
            artists = emptyList(),
            photographer = "Bob"
        )
        val spotDetails = SpotDetails(
            spot = spot,
            images = emptyList(),
            notes = emptyList()
        )

        // Call the internal method with null bitmap
        receiver.showNotification(context, spotDetails, 150, null)

        // Verify notification was posted and has correct attributes (wait up to 1 second)
        var myNotification: StatusBarNotification? = null
        for (i in 1..10) {
            val activeNotifications = notificationManager.activeNotifications
            myNotification = activeNotifications.firstOrNull { it.id == spot.id.toInt() }
            if (myNotification != null) break
            Thread.sleep(100)
        }
        
        assertNotNull("Notification should have been posted", myNotification)
        val notification = myNotification!!.notification
        
        // Verify BigPictureStyle is NOT applied
        val template = notification.extras.getString(Notification.EXTRA_TEMPLATE)
        assertNull("Template should be null when no bitmap is provided", template)
        
        // Cancel the notification after test
        notificationManager.cancel(spot.id.toInt())
    }
    }
