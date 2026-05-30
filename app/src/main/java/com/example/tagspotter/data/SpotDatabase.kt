package com.example.tagspotter.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isEmpty()) emptyList() else value.split(",")
    }
}

@Database(
    entities = [Spot::class, SpotImage::class, SpotNote::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SpotDatabase : RoomDatabase() {

    abstract fun spotDao(): SpotDao

    companion object {
        @Volatile
        private var INSTANCE: SpotDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE spots ADD COLUMN artists TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): SpotDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SpotDatabase::class.java,
                    "spot_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
