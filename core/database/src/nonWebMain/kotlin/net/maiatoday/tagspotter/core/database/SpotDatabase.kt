package net.maiatoday.tagspotter.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.ConstructedBy
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
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

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(connection: SQLiteConnection) {
        // 1. Add columns to spots table
        connection.execSQL("ALTER TABLE spots ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE spots ADD COLUMN photographerUuid TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE spots ADD COLUMN lastEditedAt INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE spots ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0")

        // 2. Add columns to spot_images table
        connection.execSQL("ALTER TABLE spot_images ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE spot_images ADD COLUMN lastEditedAt INTEGER NOT NULL DEFAULT 0")

        // 3. Add columns to spot_notes table
        connection.execSQL("ALTER TABLE spot_notes ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE spot_notes ADD COLUMN lastEditedAt INTEGER NOT NULL DEFAULT 0")

        // 4. Create high-performance indexes
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_spots_uuid ON spots(uuid)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_spot_images_uuid ON spot_images(uuid)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_spot_notes_uuid ON spot_notes(uuid)")

        // 5. Backfill timestamps
        connection.execSQL("UPDATE spots SET lastEditedAt = createdAt")
        connection.execSQL("UPDATE spot_images SET lastEditedAt = timestamp")
        connection.execSQL("UPDATE spot_notes SET lastEditedAt = timestamp")

        // 6. Generate RFC4122 v4 UUIDs natively in SQLite for all existing rows (where UUID is empty)
        val rfc4122v4UuidExpr = "lower(hex(randomblob(4))) || '-' || lower(hex(randomblob(2))) || '-4' || substr(lower(hex(randomblob(2))),2) || '-' || substr('89ab',abs(random()) % 4 + 1, 1) || substr(lower(hex(randomblob(2))),2) || '-' || lower(hex(randomblob(6)))"
        connection.execSQL("UPDATE spots SET uuid = ($rfc4122v4UuidExpr) WHERE uuid = ''")
        connection.execSQL("UPDATE spot_images SET uuid = ($rfc4122v4UuidExpr) WHERE uuid = ''")
        connection.execSQL("UPDATE spot_notes SET uuid = ($rfc4122v4UuidExpr) WHERE uuid = ''")
    }
}

fun RoomDatabase.Builder<SpotDatabase>.configureSpotDatabase(): RoomDatabase.Builder<SpotDatabase> {
    return this.addMigrations(MIGRATION_10_11)
}

@Database(
    entities = [SpotEntity::class, SpotImageEntity::class, SpotNoteEntity::class],
    version = 11,
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
