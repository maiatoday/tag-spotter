package net.maiatoday.tagspotter.core.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import kotlin.random.Random

actual fun createTestDatastore(): DataStore<Preferences> {
    val randomId = Random.nextInt(100000)
    val tempDir = System.getProperty("java.io.tmpdir")
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { "$tempDir/test_settings_$randomId.preferences_pb".toPath() }
    )
}
