package net.maiatoday.tagspotter.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        if (value.isEmpty()) return emptyList()
        return try {
            Json.decodeFromString<List<String>>(value)
        } catch (_: Exception) {
            value.split(",")
        }
    }
}

@Database(
    entities = [SpotEntity::class, SpotImageEntity::class, SpotNoteEntity::class],
    version = 10,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SpotDatabase : RoomDatabase() {

    abstract fun spotDao(): SpotDao

    companion object {
        @Volatile
        private var INSTANCE: SpotDatabase? = null

        fun getDatabase(context: Context): SpotDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SpotDatabase::class.java,
                    "spot_database"
                )
                .fallbackToDestructiveMigration(true)
                .build().also { INSTANCE = it }
            }
        }
    }
}
