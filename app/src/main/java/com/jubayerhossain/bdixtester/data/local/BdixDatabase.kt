package com.jubayerhossain.bdixtester.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jubayerhossain.bdixtester.data.local.dao.CatalogMetadataDao
import com.jubayerhossain.bdixtester.data.local.dao.FavoriteDao
import com.jubayerhossain.bdixtester.data.local.dao.ScanSessionDao
import com.jubayerhossain.bdixtester.data.local.dao.ServerDao
import com.jubayerhossain.bdixtester.data.local.dao.TestHistoryDao
import com.jubayerhossain.bdixtester.data.local.entity.CatalogMetadataEntity
import com.jubayerhossain.bdixtester.data.local.entity.FavoriteEntity
import com.jubayerhossain.bdixtester.data.local.entity.ScanSessionEntity
import com.jubayerhossain.bdixtester.data.local.entity.ServerEntity
import com.jubayerhossain.bdixtester.data.local.entity.TestHistoryEntity

@Database(
  entities = [
    ServerEntity::class,
    FavoriteEntity::class,
    TestHistoryEntity::class,
    ScanSessionEntity::class,
    CatalogMetadataEntity::class,
  ],
  version = 2,
  exportSchema = true,
)
abstract class BdixDatabase : RoomDatabase() {
  abstract fun serverDao(): ServerDao

  abstract fun favoriteDao(): FavoriteDao

  abstract fun testHistoryDao(): TestHistoryDao

  abstract fun scanSessionDao(): ScanSessionDao

  abstract fun catalogMetadataDao(): CatalogMetadataDao

  companion object {
    const val DATABASE_NAME: String = "bdix_tester.db"

    /**
     * Non-destructive schema migration from v1 to v2 adding `isQuickScanInconclusive`
     * and `probeGroupId` index on `test_history` while preserving all existing user data.
     */
    val MIGRATION_1_2 =
      object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            "ALTER TABLE test_history ADD COLUMN isQuickScanInconclusive INTEGER NOT NULL DEFAULT 0",
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_test_history_probeGroupId ON test_history(probeGroupId)",
          )
        }
      }

    fun create(context: Context): BdixDatabase =
      Room
        .databaseBuilder(
          context.applicationContext,
          BdixDatabase::class.java,
          DATABASE_NAME,
        ).addMigrations(MIGRATION_1_2)
        .build()
  }
}
