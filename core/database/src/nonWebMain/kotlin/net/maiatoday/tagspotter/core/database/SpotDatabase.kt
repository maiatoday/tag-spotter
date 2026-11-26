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

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE spots ADD COLUMN ownerUid TEXT")
        connection.execSQL("ALTER TABLE spot_images ADD COLUMN ownerUid TEXT")
        connection.execSQL("ALTER TABLE spot_notes ADD COLUMN ownerUid TEXT")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE spots ADD COLUMN parentPackId TEXT")
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS loaded_packs (
                packId TEXT NOT NULL PRIMARY KEY,
                title TEXT NOT NULL,
                authorName TEXT NOT NULL,
                description TEXT NOT NULL,
                importedAt INTEGER NOT NULL,
                lastRefreshedAt INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE spots ADD COLUMN isPublic INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(connection: SQLiteConnection) {
        // 1. Add explicit spot provenance column and backfill imported spots
        connection.execSQL("ALTER TABLE spots ADD COLUMN origin TEXT NOT NULL DEFAULT 'OWN'")
        connection.execSQL("UPDATE spots SET origin = 'IMPORTED' WHERE isImported = 1 OR parentPackId IS NOT NULL")

        // 2. Add separate remoteThumbnailUrl column to spot_images and backfill any existing HTTPS URLs
        connection.execSQL("ALTER TABLE spot_images ADD COLUMN remoteThumbnailUrl TEXT NOT NULL DEFAULT ''")
        connection.execSQL("UPDATE spot_images SET remoteThumbnailUrl = thumbnailPath WHERE thumbnailPath LIKE 'http%'")

        // 3. Ensure every spot has a non-empty unique UUID before creating the unique index
        val rfc4122v4UuidExpr = "lower(hex(randomblob(4))) || '-' || lower(hex(randomblob(2))) || '-4' || substr(lower(hex(randomblob(2))),2) || '-' || substr('89ab',abs(random()) % 4 + 1, 1) || substr(lower(hex(randomblob(2))),2) || '-' || lower(hex(randomblob(6)))"
        connection.execSQL("UPDATE spots SET uuid = ($rfc4122v4UuidExpr) WHERE uuid = ''")
        // Deduplicate any duplicate UUID rows (keep the most recently edited / highest id)
        connection.execSQL("""
            DELETE FROM spots
            WHERE id NOT IN (
                SELECT MAX(id) FROM spots GROUP BY uuid
            )
        """.trimIndent())

        // 4. Recreate spots UUID index as UNIQUE and add ownerUid / origin / scoped child indexes
        connection.execSQL("DROP INDEX IF EXISTS index_spots_uuid")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_spots_uuid ON spots(uuid)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_spots_ownerUid ON spots(ownerUid)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_spots_origin ON spots(origin)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_spots_origin_ownerUid ON spots(origin, ownerUid)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_spot_images_spotId_uuid ON spot_images(spotId, uuid)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_spot_notes_spotId_uuid ON spot_notes(spotId, uuid)")
    }
}

fun RoomDatabase.Builder<SpotDatabase>.configureSpotDatabase(): RoomDatabase.Builder<SpotDatabase> {
    return this.addMigrations(MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15)
}

@Database(
    entities = [SpotEntity::class, SpotImageEntity::class, SpotNoteEntity::class, LoadedPackEntity::class],
    version = 15,
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
