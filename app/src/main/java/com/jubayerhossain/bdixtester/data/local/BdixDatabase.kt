package com.jubayerhossain.bdixtester.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jubayerhossain.bdixtester.data.local.dao.CatalogMetadataDao
import com.jubayerhossain.bdixtester.data.local.dao.FavoriteDao
import com.jubayerhossain.bdixtester.data.local.dao.GovernanceDao
import com.jubayerhossain.bdixtester.data.local.dao.ScanSessionDao
import com.jubayerhossain.bdixtester.data.local.dao.ServerDao
import com.jubayerhossain.bdixtester.data.local.dao.TestHistoryDao
import com.jubayerhossain.bdixtester.data.local.entity.CatalogMetadataEntity
import com.jubayerhossain.bdixtester.data.local.entity.EndpointGovernanceEntity
import com.jubayerhossain.bdixtester.data.local.entity.FavoriteEntity
import com.jubayerhossain.bdixtester.data.local.entity.ScanSessionEntity
import com.jubayerhossain.bdixtester.data.local.entity.ServerEntity
import com.jubayerhossain.bdixtester.data.local.entity.TestHistoryEntity
import com.jubayerhossain.bdixtester.data.local.entity.VerificationEventEntity

@Database(
  entities = [
    ServerEntity::class,
    FavoriteEntity::class,
    TestHistoryEntity::class,
    ScanSessionEntity::class,
    CatalogMetadataEntity::class,
    EndpointGovernanceEntity::class,
    VerificationEventEntity::class,
  ],
  version = 3,
  exportSchema = true,
)
abstract class BdixDatabase : RoomDatabase() {
  abstract fun serverDao(): ServerDao

  abstract fun favoriteDao(): FavoriteDao

  abstract fun testHistoryDao(): TestHistoryDao

  abstract fun scanSessionDao(): ScanSessionDao

  abstract fun catalogMetadataDao(): CatalogMetadataDao

  abstract fun governanceDao(): GovernanceDao

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

    /**
     * Non-destructive schema migration from v2 to v3 adding reversible endpoint governance
     * state and multi-layer verification audit trail tables while preserving all v2 user data.
     */
    val MIGRATION_2_3 =
      object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `endpoint_governance` (
              `serverId` TEXT NOT NULL,
              `lifecycleStatus` TEXT NOT NULL,
              `confidence` TEXT NOT NULL,
              `reason` TEXT NOT NULL,
              `recommendedAction` TEXT NOT NULL,
              `removalEligible` INTEGER NOT NULL,
              `normalizedDomain` TEXT,
              `dnsSummary` TEXT NOT NULL,
              `registrationStatus` TEXT NOT NULL,
              `evidenceJson` TEXT NOT NULL,
              `checkedSourcesCsv` TEXT NOT NULL,
              `missingEvidenceCsv` TEXT NOT NULL,
              `recheckAfterEpochMs` INTEGER,
              `lastVerifiedAtEpochMs` INTEGER,
              `archivedAtEpochMs` INTEGER,
              `archivedByUser` INTEGER NOT NULL,
              `updatedAtEpochMs` INTEGER NOT NULL,
              PRIMARY KEY(`serverId`)
            )
            """.trimIndent(),
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_endpoint_governance_lifecycleStatus` ON `endpoint_governance` (`lifecycleStatus`)",
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_endpoint_governance_lastVerifiedAtEpochMs` ON `endpoint_governance` (`lastVerifiedAtEpochMs`)",
          )
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `verification_events` (
              `eventId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              `serverId` TEXT NOT NULL,
              `previousStatus` TEXT NOT NULL,
              `newStatus` TEXT NOT NULL,
              `confidence` TEXT NOT NULL,
              `triggerSource` TEXT NOT NULL,
              `summary` TEXT NOT NULL,
              `detailsJson` TEXT NOT NULL,
              `createdAtEpochMs` INTEGER NOT NULL
            )
            """.trimIndent(),
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_verification_events_serverId_createdAtEpochMs` ON `verification_events` (`serverId`, `createdAtEpochMs`)",
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_verification_events_createdAtEpochMs` ON `verification_events` (`createdAtEpochMs`)",
          )
        }
      }

    fun create(context: Context): BdixDatabase =
      Room
        .databaseBuilder(
          context.applicationContext,
          BdixDatabase::class.java,
          DATABASE_NAME,
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .build()
  }
}
