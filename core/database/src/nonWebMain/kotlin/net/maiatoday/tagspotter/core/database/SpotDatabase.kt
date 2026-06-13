package net.maiatoday.tagspotter.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.ConstructedBy
import androidx.room.RoomDatabaseConstructor
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
@ConstructedBy(SpotDatabaseConstructor::class)
abstract class SpotDatabase : RoomDatabase() {
    abstract fun spotDao(): SpotDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object SpotDatabaseConstructor : RoomDatabaseConstructor<SpotDatabase> {
    override fun initialize(): SpotDatabase
}

expect fun getDatabaseBuilder(ctx: Any? = null): RoomDatabase.Builder<SpotDatabase>
