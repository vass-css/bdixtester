package com.jubayerhossain.bdixtester

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jubayerhossain.bdixtester.core.model.CatalogSourceType
import com.jubayerhossain.bdixtester.core.model.ScanMode
import com.jubayerhossain.bdixtester.core.model.ScanScope
import com.jubayerhossain.bdixtester.core.model.ScanSessionState
import com.jubayerhossain.bdixtester.core.model.ServerCategory
import com.jubayerhossain.bdixtester.core.model.ServerProtocol
import com.jubayerhossain.bdixtester.core.model.TestStatus
import com.jubayerhossain.bdixtester.core.model.VerificationState
import com.jubayerhossain.bdixtester.data.local.BdixDatabase
import com.jubayerhossain.bdixtester.data.network.EndpointProbeConfig
import com.jubayerhossain.bdixtester.data.network.EndpointProbeExecutor
import com.jubayerhossain.bdixtester.data.network.ScanCoordinator
import com.jubayerhossain.bdixtester.data.preferences.AppSettings
import com.jubayerhossain.bdixtester.data.repository.RoomCatalogRepository
import com.jubayerhossain.bdixtester.domain.model.ServerEndpoint
import com.jubayerhossain.bdixtester.domain.model.ServerTestResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ScanCoordinatorTest {
  private lateinit var db: BdixDatabase
  private lateinit var repo: RoomCatalogRepository

  @Before
  fun setUp() {
    val context: Context = ApplicationProvider.getApplicationContext()
    db =
      Room
        .inMemoryDatabaseBuilder(context, BdixDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    repo =
      RoomCatalogRepository(
        database = db,
        rawBuiltInLinks =
          listOf(
            "http://dup.example.net",
            "http://unique.example.net",
            "http://dup.example.net",
            "http://host-two.example.net",
          ),
      )
  }

  @After
  fun tearDown() {
    runBlocking { repo.closeAndJoin() }
    db.close()
  }

  @Test
  fun exactDuplicateUrlsCoalesceSingleProbeAndPreserveReplicasWithSharedRunAndGroupIds() =
    runTest {
      repo.ensureInitialized()
      val endpoints =
        db.serverDao().getBuiltInServersOrdered().map {
          ServerEndpoint(
            id = it.id,
            name = it.name,
            originalUrl = it.originalUrl,
            host = it.host,
            explicitPort = it.explicitPort,
            protocol = ServerProtocol.HTTP,
            category = ServerCategory.UNCATEGORIZED,
            isFeatured = false,
            sourceType = CatalogSourceType.BUILT_IN,
            sourceIndex = it.sourceIndex,
            verificationState = VerificationState.UNKNOWN,
            lastReviewedAtEpochMs = null,
            speedTestUrl = null,
            isPrivateIp = false,
          )
        }

      val probeCalls = AtomicInteger(0)
      val maxConcurrentObserved = AtomicInteger(0)
      val currentConcurrent = AtomicInteger(0)

      val executor =
        object : EndpointProbeExecutor {
          override suspend fun probe(
            endpoint: ServerEndpoint,
            config: EndpointProbeConfig,
          ): ServerTestResult {
            val active = currentConcurrent.incrementAndGet()
            maxConcurrentObserved.updateAndGet { prev -> maxOf(prev, active) }
            probeCalls.incrementAndGet()
            delay(40)
            currentConcurrent.decrementAndGet()
            return ServerTestResult(
              serverId = endpoint.id,
              serverNameSnapshot = endpoint.name,
              urlSnapshot = endpoint.originalUrl,
              testedAtEpochMs = 10_000L,
              scanMode = config.scanMode,
              protocol = endpoint.protocol,
              status = TestStatus.REACHABLE,
              httpResponseLatencyMs = 12L,
            )
          }
        }

      val coordinator = ScanCoordinator(repo, executor)
      val started =
        coordinator.startScan(
          endpoints = endpoints,
          scanMode = ScanMode.FULL_SCAN,
          scanScope = ScanScope.ALL_SERVERS,
          settings = AppSettings(maxConcurrency = 2),
        )
      assertTrue(started)

      // Second start while active must be rejected
      assertFalse(
        coordinator.startScan(
          endpoints = endpoints,
          scanMode = ScanMode.QUICK_SCAN,
          scanScope = ScanScope.ALL_SERVERS,
          settings = AppSettings(),
        ),
      )

      // Wait for completion
      while (repo.scanProgress.value.isScanning) {
        delay(20)
      }

      val progress = repo.scanProgress.value
      assertEquals(ScanSessionState.COMPLETED, progress.sessionState)
      assertEquals(4, progress.totalCatalogEntries)
      assertEquals(3, progress.uniqueExactUrlCount)
      assertEquals(3, progress.executedUniqueProbeCount)
      assertEquals(4, progress.mappedSourceEntryCount)
      assertEquals(3, probeCalls.get())
      assertTrue(maxConcurrentObserved.get() <= 2)

      val history = db.testHistoryDao().observeAllHistory().first()
      assertEquals(4, history.size)
      val dupEntries =
        history
          .filter { it.urlSnapshot == "http://dup.example.net" }
          .sortedBy { it.isCoalescedReplica }
      assertEquals(2, dupEntries.size)
      assertNotNull(dupEntries[0].scanRunId)
      assertEquals(dupEntries[0].scanRunId, dupEntries[1].scanRunId)
      assertEquals(dupEntries[0].probeGroupId, dupEntries[1].probeGroupId)
      assertFalse(dupEntries[0].isCoalescedReplica)
      assertTrue(dupEntries[1].isCoalescedReplica)
    }

  @Test
  fun stopScanCancelsGracefullyAndMarksSessionCancelled() =
    runTest {
      repo.ensureInitialized()
      val endpoints =
        db.serverDao().getBuiltInServersOrdered().map {
          ServerEndpoint(
            id = it.id,
            name = it.name,
            originalUrl = it.originalUrl,
            host = it.host,
            explicitPort = it.explicitPort,
            protocol = ServerProtocol.HTTP,
            category = ServerCategory.UNCATEGORIZED,
            isFeatured = false,
            sourceType = CatalogSourceType.BUILT_IN,
            sourceIndex = it.sourceIndex,
            verificationState = VerificationState.UNKNOWN,
            lastReviewedAtEpochMs = null,
            speedTestUrl = null,
            isPrivateIp = false,
          )
        }
      val slowExecutor =
        object : EndpointProbeExecutor {
          override suspend fun probe(
            endpoint: ServerEndpoint,
            config: EndpointProbeConfig,
          ): ServerTestResult {
            delay(2_000)
            return ServerTestResult(
              serverId = endpoint.id,
              testedAtEpochMs = 1L,
              scanMode = config.scanMode,
              protocol = endpoint.protocol,
              status = TestStatus.REACHABLE,
            )
          }
        }
      val coordinator = ScanCoordinator(repo, slowExecutor)
      assertTrue(
        coordinator.startScan(endpoints, ScanMode.QUICK_SCAN, ScanScope.ALL_SERVERS, AppSettings(maxConcurrency = 1)),
      )
      delay(60)
      coordinator.stopScan()

      assertFalse(repo.scanProgress.value.isScanning)
      assertEquals(ScanSessionState.CANCELLED, repo.scanProgress.value.sessionState)
    }
}
