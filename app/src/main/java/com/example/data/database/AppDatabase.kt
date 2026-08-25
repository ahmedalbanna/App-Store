package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [MarketplaceItem::class, ChatMessageEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun marketplaceDao(): MarketplaceDao
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Phase 1.4: explicit migration 1->2 adds indices
        val MARKETPLACE_DB_MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_marketplace_items_type` ON `marketplace_items` (`type`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_marketplace_items_downloadState` ON `marketplace_items` (`downloadState`)")
            }
        }

        // Phase 3.3: 2->3 adds chat_messages table
        val MARKETPLACE_DB_MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `chat_messages` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sender` TEXT NOT NULL,
                        `text` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "alwaha_marketplace_db"
                )
                    .addMigrations(MARKETPLACE_DB_MIGRATION_1_2, MARKETPLACE_DB_MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// Expose migrations for AppContainer
val MARKETPLACE_DB_MIGRATION_1_2 = AppDatabase.MARKETPLACE_DB_MIGRATION_1_2
val MARKETPLACE_DB_MIGRATION_2_3 = AppDatabase.MARKETPLACE_DB_MIGRATION_2_3
