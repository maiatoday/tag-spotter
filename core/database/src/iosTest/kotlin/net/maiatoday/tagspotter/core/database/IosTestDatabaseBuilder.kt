package net.maiatoday.tagspotter.core.database

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

actual fun getTestDatabaseBuilder(): RoomDatabase.Builder<SpotDatabase> {
    return Room.inMemoryDatabaseBuilder<SpotDatabase>().setDriver(BundledSQLiteDriver())
}
