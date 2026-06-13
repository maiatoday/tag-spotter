package net.maiatoday.tagspotter.core.database

import androidx.room.RoomDatabase

expect fun getTestDatabaseBuilder(): RoomDatabase.Builder<SpotDatabase>
