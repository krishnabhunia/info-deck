package com.krishna.infodeck.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Converters {
    @TypeConverter fun unitToString(u: RingUnit?): String? = u?.name
    @TypeConverter fun stringToUnit(s: String?): RingUnit? = s?.let { runCatching { RingUnit.valueOf(it) }.getOrNull() }
    @TypeConverter fun dirToString(d: RingDirection?): String? = d?.name
    @TypeConverter fun stringToDir(s: String?): RingDirection? = s?.let { runCatching { RingDirection.valueOf(it) }.getOrNull() }
    @TypeConverter fun statusToString(s: RingStatus?): String? = s?.name
    @TypeConverter fun stringToStatus(s: String?): RingStatus? = s?.let { runCatching { RingStatus.valueOf(it) }.getOrNull() }
    @TypeConverter fun actionToString(a: RingAction?): String? = a?.name
    @TypeConverter fun stringToAction(s: String?): RingAction? = s?.let { runCatching { RingAction.valueOf(it) }.getOrNull() }
}

/**
 * Room schema version. Also published as the DB_SCHEMA meta-data in AndroidManifest.xml so a
 * downloaded APK can be checked before installing it over this one (design 1.15.0.D1).
 * DbSchemaManifestTest fails the build if the two ever differ.
 */
const val DB_SCHEMA_VERSION = 2

@Database(
    entities = [
        CategoryEntity::class, CardEntity::class, DataVarEntity::class,
        DateVarEntity::class, SchedulerEntity::class, RingLogEntity::class, ErrorLogEntity::class,
        TagEntity::class, CardTagEntity::class, FileEntity::class
    ],
    version = DB_SCHEMA_VERSION,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): InfoDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        /**
         * v1 -> v2: three new tables only. Nothing existing is touched, so every
         * card, date and scheduler survives the upgrade untouched.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS tags (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL, createdAt INTEGER NOT NULL, lastUsedAt INTEGER NOT NULL)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_tags_name ON tags (name)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS card_tags (cardId INTEGER NOT NULL, tagId INTEGER NOT NULL, " +
                        "PRIMARY KEY(cardId, tagId), " +
                        "FOREIGN KEY(cardId) REFERENCES cards(id) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                        "FOREIGN KEY(tagId) REFERENCES tags(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_card_tags_cardId ON card_tags (cardId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_card_tags_tagId ON card_tags (tagId)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS files (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "cardId INTEGER NOT NULL, displayName TEXT NOT NULL, storedName TEXT NOT NULL, " +
                        "mime TEXT NOT NULL, sizeBytes INTEGER NOT NULL, addedAt INTEGER NOT NULL, " +
                        "fileOrder INTEGER NOT NULL, " +
                        "FOREIGN KEY(cardId) REFERENCES cards(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_files_cardId ON files (cardId)")
            }
        }

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: build(context.applicationContext).also { instance = it }
        }

        private fun build(app: Context): AppDatabase =
            Room.databaseBuilder(app, AppDatabase::class.java, "infodeck.db")
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        // Built-in Uncategorised category, fixed id, never deletable.
                        db.execSQL(
                            "INSERT INTO categories (id, name, colorIndex, sortOrder, builtIn, createdAt, updatedAt) " +
                                "VALUES ($UNCATEGORISED_ID, 'Uncategorised', 7, 100000, 1, ${System.currentTimeMillis()}, ${System.currentTimeMillis()})"
                        )
                    }
                })
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
