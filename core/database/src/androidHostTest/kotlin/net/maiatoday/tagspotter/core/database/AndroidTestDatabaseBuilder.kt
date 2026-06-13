package net.maiatoday.tagspotter.core.database

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider

actual fun getTestDatabaseBuilder(): RoomDatabase.Builder<SpotDatabase> {
    return Room.inMemoryDatabaseBuilder(
        context = ApplicationProvider.getApplicationContext(),
        klass = SpotDatabase::class.java
    )
}
