package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [MarketplaceItem::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun marketplaceDao(): MarketplaceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Phase 1.4: explicit migration 1->2 adds indices; for now a simple no-op migration
        // since indices are created automatically by Room, but we keep a Migration object
        // to avoid fallbackToDestructiveMigration and preserve user library/bookmarks.
        val MARKETPLACE_DB_MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create indices if not exists (Room will create them, but ensure for existing DBs)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_marketplace_items_type` ON `marketplace_items` (`type`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_marketplace_items_downloadState` ON `marketplace_items` (`downloadState`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "alwaha_marketplace_db"
                )
                    .addMigrations(MARKETPLACE_DB_MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// Expose migration for Hilt module
val MARKETPLACE_DB_MIGRATION_1_2 = AppDatabase.MARKETPLACE_DB_MIGRATION_1_2
