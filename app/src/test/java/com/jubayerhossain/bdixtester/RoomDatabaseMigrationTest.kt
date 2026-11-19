package com.jubayerhossain.bdixtester

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import com.jubayerhossain.bdixtester.core.model.CatalogSourceType
import com.jubayerhossain.bdixtester.core.model.ScanMode
import com.jubayerhossain.bdixtester.core.model.ScanSessionState
import com.jubayerhossain.bdixtester.core.model.ServerCategory
import com.jubayerhossain.bdixtester.core.model.ServerProtocol
import com.jubayerhossain.bdixtester.core.model.TestStatus
import com.jubayerhossain.bdixtester.core.model.VerificationLifecycleStatus
import com.jubayerhossain.bdixtester.data.catalog.BuiltInCatalog
import com.jubayerhossain.bdixtester.data.catalog.FtpLinks
import com.jubayerhossain.bdixtester.data.local.BdixDatabase
import com.jubayerhossain.bdixtester.data.local.entity.ScanSessionEntity
import com.jubayerhossain.bdixtester.data.local.entity.ServerEntity
import com.jubayerhossain.bdixtester.data.repository.RoomCatalogRepository
import com.jubayerhossain.bdixtester.domain.model.ServerTestResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomDatabaseMigrationTest {
  private lateinit var context: Context
  private lateinit var db: BdixDatabase
  private var activeRepo: RoomCatalogRepository? = null

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    db =
      Room
        .inMemoryDatabaseBuilder(context, BdixDatabase::class.java)
        .addMigrations(BdixDatabase.MIGRATION_1_2, BdixDatabase.MIGRATION_2_3)
        .allowMainThreadQueries()
        .build()
  }

  @After
  fun tearDown() {
    runBlocking { activeRepo?.closeAndJoin() }
    db.close()
  }

  @Test
  fun exportedRoomSchemaV2ExistsAndIsValidJson() {
    val resourceStream =
      javaClass.classLoader?.getResourceAsStream(
        "com.jubayerhossain.bdixtester.data.local.BdixDatabase/2.json",
      )
    val schemaText =
      if (resourceStream != null) {
        resourceStream.bufferedReader().use { it.readText() }
      } else {
        val candidates =
          listOf(
            File("schemas/com.jubayerhossain.bdixtester.data.local.BdixDatabase/2.json"),
            File("app/schemas/com.jubayerhossain.bdixtester.data.local.BdixDatabase/2.json"),
          )
        val file =
          candidates.firstOrNull { it.exists() }
            ?: error("Exported Room schema 2.json not found in test resources or workspace.")
        file.readText()
      }
    val json = JSONObject(schemaText)
    assertEquals(2, json.getJSONObject("database").getInt("version"))
  }

  @Test
  fun exportedRoomSchemaV3ExistsAndIsValidJson() {
    val resourceStream =
      javaClass.classLoader?.getResourceAsStream(
        "com.jubayerhossain.bdixtester.data.local.BdixDatabase/3.json",
      )
    val schemaText =
      if (resourceStream != null) {
        resourceStream.bufferedReader().use { it.readText() }
      } else {
        val candidates =
          listOf(
            File("schemas/com.jubayerhossain.bdixtester.data.local.BdixDatabase/3.json"),
            File("app/schemas/com.jubayerhossain.bdixtester.data.local.BdixDatabase/3.json"),
          )
        val file =
          candidates.firstOrNull { it.exists() }
            ?: error("Exported Room schema 3.json not found in test resources or workspace.")
        file.readText()
      }
    val json = JSONObject(schemaText)
    assertEquals(3, json.getJSONObject("database").getInt("version"))
  }

  @Test
  fun migration1To2PreservesCustomServersFavoritesAndHistory() {
    val tempDbFile = File.createTempFile("bdix_v1_test", ".db")
    try {
      SQLiteDatabase.openOrCreateDatabase(tempDbFile, null).use { v1 ->
        v1.execSQL(
          """
          CREATE TABLE IF NOT EXISTS servers (
              id TEXT NOT NULL PRIMARY KEY,
              sourceType TEXT NOT NULL,
              sourceIndex INTEGER,
              feedId TEXT,
              remoteEntryId TEXT,
              name TEXT NOT NULL,
              originalUrl TEXT NOT NULL,
              host TEXT NOT NULL,
              explicitPort INTEGER,
              protocol TEXT NOT NULL,
              category TEXT NOT NULL,
              isFeatured INTEGER NOT NULL,
              description TEXT,
              sourceAttribution TEXT,
              verificationState TEXT NOT NULL,
              lastReviewedAtEpochMs INTEGER,
              speedTestUrl TEXT,
              speedTestMaxBytes INTEGER,
              isPrivateIp INTEGER NOT NULL,
              createdAtEpochMs INTEGER NOT NULL,
              updatedAtEpochMs INTEGER NOT NULL
          )
          """.trimIndent(),
        )
        v1.execSQL(
          """
          CREATE TABLE IF NOT EXISTS favorites (
              serverId TEXT NOT NULL PRIMARY KEY,
              isPinned INTEGER NOT NULL,
              displayOrder INTEGER NOT NULL,
              addedAtEpochMs INTEGER NOT NULL,
              note TEXT
          )
          """.trimIndent(),
        )
        v1.execSQL(
          """
          CREATE TABLE IF NOT EXISTS test_history (
              historyId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              serverId TEXT NOT NULL,
              scanRunId TEXT,
              probeGroupId TEXT,
              isCoalescedReplica INTEGER NOT NULL,
              serverNameSnapshot TEXT NOT NULL,
              urlSnapshot TEXT NOT NULL,
              protocol TEXT NOT NULL,
              scanMode TEXT NOT NULL,
              status TEXT NOT NULL,
              errorClassification TEXT NOT NULL,
              testedAtEpochMs INTEGER NOT NULL,
              dnsTimeMs INTEGER,
              tcpConnectTimeMs INTEGER,
              tlsHandshakeTimeMs INTEGER,
              httpResponseLatencyMs INTEGER,
              ftpGreetingLatencyMs INTEGER,
              ftpGreetingCode INTEGER,
              httpStatusCode INTEGER,
              finalUrl TEXT,
              failureMessage TEXT,
              downloadMbps REAL,
              downloadMibPerSec REAL,
              transferredBytes INTEGER,
              speedElapsedNanos INTEGER,
              speedTestState TEXT,
              speedTerminationReason TEXT,
              networkProfileLabel TEXT
          )
          """.trimIndent(),
        )
        v1.execSQL(
          "INSERT INTO servers VALUES ('custom:1','USER_CUSTOM',NULL,NULL,NULL,'My Node','http://10.0.0.2','10.0.0.2',NULL,'HTTP','OTHER',0,NULL,NULL,'UNKNOWN',NULL,NULL,NULL,1,1000,1000)",
        )
        v1.execSQL("INSERT INTO favorites VALUES ('custom:1',1,0,1000,NULL)")
        v1.execSQL(
          "INSERT INTO test_history (serverId, scanRunId, probeGroupId, isCoalescedReplica, serverNameSnapshot, urlSnapshot, protocol, scanMode, status, errorClassification, testedAtEpochMs) VALUES ('custom:1','run1','grp1',0,'My Node','http://10.0.0.2','HTTP','FULL_SCAN','REACHABLE','NONE',2000)",
        )
        v1.version = 1
      }

      // Apply MIGRATION_1_2 on raw SupportSQLiteDatabase using Android SQLiteOpenHelper
      val helper = android.database.sqlite.SQLiteOpenHelper::class.java
      assertNotNull(helper)
      SQLiteDatabase.openDatabase(tempDbFile.path, null, SQLiteDatabase.OPEN_READWRITE).use { raw ->
        val wrapper =
          object : androidx.sqlite.db.SupportSQLiteDatabase by androidx.sqlite.db.framework
            .FrameworkSQLiteOpenHelperFactory()
            .create(
              SupportSQLiteOpenHelper.Configuration
                .builder(context)
                .name(tempDbFile.path)
                .callback(
                  object : SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {}

                    override fun onUpgrade(
                      db: androidx.sqlite.db.SupportSQLiteDatabase,
                      oldVersion: Int,
                      newVersion: Int,
                    ) {
                      BdixDatabase.MIGRATION_1_2.migrate(db)
                    }
                  },
                ).build(),
            ).writableDatabase {}
        wrapper.query("SELECT isQuickScanInconclusive, serverId FROM test_history").use { c ->
          assertTrue(c.moveToFirst())
          assertEquals(0, c.getInt(0))
          assertEquals("custom:1", c.getString(1))
        }
        wrapper.close()
        raw.close()
      }
    } finally {
      tempDbFile.delete()
    }
  }

  @Test
  fun repositorySeedsCuratedCatalogSyncsUpgradesAndRejectsDuplicateCustomServers() =
    runTest {
      val curatedCatalog = BuiltInCatalog.loadCuratedCatalog()
      db.scanSessionDao().upsertSession(
        ScanSessionEntity(
          scanRunId = "orphaned_run",
          scanMode = ScanMode.FULL_SCAN.name,
          scopeLabel = "ALL_SERVERS",
          state = ScanSessionState.RUNNING.name,
          totalCatalogEntries = curatedCatalog.size,
          uniqueExactUrlCount = curatedCatalog.size,
          executedUniqueProbeCount = 12,
          mappedSourceEntryCount = 12,
          startedAtEpochMs = 1000L,
          updatedAtEpochMs = 2000L,
          finishedAtEpochMs = null,
        ),
      )

      val repo = RoomCatalogRepository(database = db).also { activeRepo = it }
      val firstReport = repo.ensureInitialized()
      assertTrue(firstReport.isConsistent)
      assertEquals(curatedCatalog.size, firstReport.builtInSourceCount)
      assertEquals(curatedCatalog.size, firstReport.builtInDatabaseCount)
      assertTrue(firstReport.orderMatches)
      assertTrue(firstReport.exactUrlsMatch)

      // Idempotent second call
      val secondReport = repo.ensureInitialized()
      assertTrue(secondReport.isConsistent)
      assertEquals(curatedCatalog.size, db.serverDao().countBuiltInServers())

      // Interrupted scan session should no longer be RUNNING
      assertTrue(db.scanSessionDao().getRunningSessions().isEmpty())
      val latestSession = db.scanSessionDao().observeLatestSession().first()
      assertEquals(ScanSessionState.INTERRUPTED.name, latestSession?.state)

      // Custom server + favorite + history survive remote catalog atomic replacement
      val custom =
        repo
          .saveCustomServer(
            name = "Home NAS",
            rawUrl = "http://192.168.0.10:8096",
            category = ServerCategory.MOVIES,
            speedTestUrl = "http://192.168.0.10:8096/test.bin",
          ).getOrThrow()
      repo.toggleFavorite(custom.id)
      repo.setFavoritePinned(custom.id, true)
      repo.recordTestResults(
        listOf(
          ServerTestResult(
            serverId = custom.id,
            serverNameSnapshot = custom.name,
            urlSnapshot = custom.originalUrl,
            testedAtEpochMs = 5000L,
            scanMode = ScanMode.QUICK_SCAN,
            protocol = ServerProtocol.HTTP,
            status = TestStatus.REACHABLE,
          ),
        ),
      )

      // Duplicate rejection: built-in URL with trailing slash or different case, and existing custom URL
      val dupBuiltIn =
        repo.saveCustomServer(
          name = "Dup BuiltIn",
          rawUrl = "http://103.58.73.6/",
          category = ServerCategory.FTP_FILES,
          speedTestUrl = null,
        )
      assertTrue(dupBuiltIn.isFailure)

      val dupCustom =
        repo.saveCustomServer(
          name = "Dup Custom",
          rawUrl = "HTTP://192.168.0.10:8096/",
          category = ServerCategory.MOVIES,
          speedTestUrl = null,
        )
      assertTrue(dupCustom.isFailure)

      // Simulate upgrading from an older built-in row (stale metadata & count) without losing custom data
      db.serverDao().syncBuiltInServersAtomically(
        BuiltInCatalog.parseSourceCatalog(FtpLinks.links.take(3)).map {
          ServerEntity(
            id = it.id,
            sourceType = CatalogSourceType.BUILT_IN.name,
            sourceIndex = it.sourceIndex,
            feedId = null,
            remoteEntryId = null,
            name = it.name,
            originalUrl = it.originalUrl,
            host = it.host,
            explicitPort = it.explicitPort,
            protocol = it.protocol.name,
            category = it.category.name,
            isFeatured = false,
            description = null,
            sourceAttribution = it.sourceAttribution,
            verificationState = it.verificationState.name,
            lastReviewedAtEpochMs = null,
            speedTestUrl = null,
            speedTestMaxBytes = null,
            isPrivateIp = it.isPrivateIp,
            createdAtEpochMs = 100L,
            updatedAtEpochMs = 100L,
          )
        },
      )
      val upgradeReport = repo.ensureInitialized()
      assertTrue(upgradeReport.isConsistent)
      assertEquals(curatedCatalog.size, db.serverDao().countBuiltInServers())

      db.serverDao().replaceRemoteCatalogAtomically(
        listOf(
          ServerEntity(
            id = "remote:remote_primary:r1",
            sourceType = CatalogSourceType.REMOTE_CATALOG.name,
            sourceIndex = 0,
            feedId = "remote_primary",
            remoteEntryId = "r1",
            name = "Remote Mirror",
            originalUrl = "https://mirror.example.net/",
            host = "mirror.example.net",
            explicitPort = null,
            protocol = ServerProtocol.HTTPS.name,
            category = ServerCategory.WEB.name,
            isFeatured = true,
            description = null,
            sourceAttribution = "Remote",
            verificationState = "COMMUNITY_SUBMITTED",
            lastReviewedAtEpochMs = 6000L,
            speedTestUrl = null,
            speedTestMaxBytes = null,
            isPrivateIp = false,
            createdAtEpochMs = 6000L,
            updatedAtEpochMs = 6000L,
          ),
        ),
      )

      assertEquals(curatedCatalog.size, db.serverDao().countBuiltInServers())
      assertEquals(1, db.serverDao().countRemoteServers())
      assertNotNull(db.serverDao().getServerById(custom.id))
      assertEquals(true, db.favoriteDao().getFavorite(custom.id)?.isPinned)
      assertEquals(
        1,
        db
          .testHistoryDao()
          .observeHistoryForServer(custom.id)
          .first()
          .size,
      )
    }

  @Test
  fun migration2To3PreservesV2DataAndReversibleArchiveRestorePreservesFavorites() =
    runTest {
      val tempDbFile = File.createTempFile("bdix_v2_test", ".db")
      try {
        SQLiteDatabase.openOrCreateDatabase(tempDbFile, null).use { v2 ->
          v2.execSQL(
            """
            CREATE TABLE IF NOT EXISTS servers (
                id TEXT NOT NULL PRIMARY KEY,
                sourceType TEXT NOT NULL,
                sourceIndex INTEGER,
                feedId TEXT,
                remoteEntryId TEXT,
                name TEXT NOT NULL,
                originalUrl TEXT NOT NULL,
                host TEXT NOT NULL,
                explicitPort INTEGER,
                protocol TEXT NOT NULL,
                category TEXT NOT NULL,
                isFeatured INTEGER NOT NULL,
                description TEXT,
                sourceAttribution TEXT,
                verificationState TEXT NOT NULL,
                lastReviewedAtEpochMs INTEGER,
                speedTestUrl TEXT,
                speedTestMaxBytes INTEGER,
                isPrivateIp INTEGER NOT NULL,
                createdAtEpochMs INTEGER NOT NULL,
                updatedAtEpochMs INTEGER NOT NULL
            )
            """.trimIndent(),
          )
          v2.execSQL(
            """
            CREATE TABLE IF NOT EXISTS favorites (
                serverId TEXT NOT NULL PRIMARY KEY,
                isPinned INTEGER NOT NULL,
                displayOrder INTEGER NOT NULL,
                addedAtEpochMs INTEGER NOT NULL,
                note TEXT
            )
            """.trimIndent(),
          )
          v2.execSQL(
            """
            CREATE TABLE IF NOT EXISTS test_history (
                historyId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                serverId TEXT NOT NULL,
                scanRunId TEXT,
                probeGroupId TEXT,
                isCoalescedReplica INTEGER NOT NULL,
                serverNameSnapshot TEXT NOT NULL,
                urlSnapshot TEXT NOT NULL,
                protocol TEXT NOT NULL,
                scanMode TEXT NOT NULL,
                status TEXT NOT NULL,
                errorClassification TEXT NOT NULL,
                testedAtEpochMs INTEGER NOT NULL,
                dnsTimeMs INTEGER,
                tcpConnectTimeMs INTEGER,
                tlsHandshakeTimeMs INTEGER,
                httpResponseLatencyMs INTEGER,
                ftpGreetingLatencyMs INTEGER,
                ftpGreetingCode INTEGER,
                httpStatusCode INTEGER,
                finalUrl TEXT,
                failureMessage TEXT,
                downloadMbps REAL,
                downloadMibPerSec REAL,
                transferredBytes INTEGER,
                speedElapsedNanos INTEGER,
                speedTestState TEXT,
                speedTerminationReason TEXT,
                networkProfileLabel TEXT,
                isQuickScanInconclusive INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
          )
          v2.execSQL(
            """
            CREATE TABLE IF NOT EXISTS scan_sessions (
                scanRunId TEXT NOT NULL PRIMARY KEY,
                scanMode TEXT NOT NULL,
                scopeLabel TEXT NOT NULL,
                state TEXT NOT NULL,
                totalCatalogEntries INTEGER NOT NULL,
                uniqueExactUrlCount INTEGER NOT NULL,
                executedUniqueProbeCount INTEGER NOT NULL,
                mappedSourceEntryCount INTEGER NOT NULL,
                startedAtEpochMs INTEGER NOT NULL,
                updatedAtEpochMs INTEGER NOT NULL,
                finishedAtEpochMs INTEGER
            )
            """.trimIndent(),
          )
          v2.execSQL(
            """
            CREATE TABLE IF NOT EXISTS catalog_metadata (
                id INTEGER NOT NULL PRIMARY KEY,
                builtInSourceCount INTEGER NOT NULL,
                builtInDatabaseCount INTEGER NOT NULL,
                remoteFeedId TEXT,
                remoteFeedVersion TEXT,
                remoteSequence INTEGER NOT NULL,
                remoteEntryCount INTEGER NOT NULL,
                lastSyncStatus TEXT NOT NULL,
                lastSyncMessage TEXT,
                lastSyncAttemptEpochMs INTEGER,
                lastSuccessfulSyncEpochMs INTEGER
            )
            """.trimIndent(),
          )
          v2.execSQL(
            "INSERT INTO servers VALUES ('custom:v2node','USER_CUSTOM',NULL,NULL,NULL,'V2 Node','http://10.88.88.88','10.88.88.88',NULL,'HTTP','MOVIES',0,NULL,NULL,'UNKNOWN',NULL,NULL,NULL,1,1000,1000)",
          )
          v2.execSQL("INSERT INTO favorites VALUES ('custom:v2node',1,0,1000,'Pinned v2 fav')")
          v2.version = 2
        }

        val migratedDb =
          Room
            .databaseBuilder(context, BdixDatabase::class.java, tempDbFile.path)
            .addMigrations(BdixDatabase.MIGRATION_1_2, BdixDatabase.MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()
        try {
          assertNotNull(migratedDb.serverDao().getServerById("custom:v2node"))
          assertEquals(true, migratedDb.favoriteDao().getFavorite("custom:v2node")?.isPinned)

          val repo = RoomCatalogRepository(database = migratedDb)
          repo.ensureInitialized()

          assertTrue(repo.endpoints.first().any { it.id == "custom:v2node" })

          repo.markRemovalCandidate("custom:v2node", "Test candidate")
          assertEquals(
            VerificationLifecycleStatus.REMOVAL_CANDIDATE,
            repo.governanceRecords.first()["custom:v2node"]?.lifecycleStatus,
          )

          repo.archiveEndpoint("custom:v2node", "Reversible archive test")
          assertTrue(repo.endpoints.first().none { it.id == "custom:v2node" })
          assertTrue(repo.allCatalogEndpoints.first().any { it.id == "custom:v2node" })
          assertEquals(true, migratedDb.favoriteDao().getFavorite("custom:v2node")?.isPinned)

          repo.restoreEndpoint("custom:v2node", "Restored in test")
          assertTrue(repo.endpoints.first().any { it.id == "custom:v2node" })
          assertEquals(VerificationLifecycleStatus.ACTIVE, repo.governanceRecords.first()["custom:v2node"]?.lifecycleStatus)
          assertEquals(true, migratedDb.favoriteDao().getFavorite("custom:v2node")?.isPinned)

          val events = repo.observeVerificationEvents("custom:v2node").first()
          assertTrue(events.size >= 3)
          repo.closeAndJoin()
        } finally {
          migratedDb.close()
        }
      } finally {
        tempDbFile.delete()
      }
    }
}
