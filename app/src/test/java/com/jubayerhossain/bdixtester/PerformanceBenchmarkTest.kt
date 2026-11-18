package com.jubayerhossain.bdixtester

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jubayerhossain.bdixtester.core.di.AppContainer
import com.jubayerhossain.bdixtester.core.model.AppLanguage
import com.jubayerhossain.bdixtester.core.model.AppThemePreset
import com.jubayerhossain.bdixtester.core.model.ScanMode
import com.jubayerhossain.bdixtester.core.model.ScanScope
import com.jubayerhossain.bdixtester.core.model.ScanSessionState
import com.jubayerhossain.bdixtester.core.model.TestStatus
import com.jubayerhossain.bdixtester.core.model.ThemePreference
import com.jubayerhossain.bdixtester.core.util.ServerDisplayClassifier
import com.jubayerhossain.bdixtester.core.validation.UrlParser
import com.jubayerhossain.bdixtester.data.catalog.BuiltInCatalog
import com.jubayerhossain.bdixtester.data.local.BdixDatabase
import com.jubayerhossain.bdixtester.data.network.EndpointProbeConfig
import com.jubayerhossain.bdixtester.data.network.EndpointProbeExecutor
import com.jubayerhossain.bdixtester.data.network.HttpEndpointTester
import com.jubayerhossain.bdixtester.data.network.ScanCoordinator
import com.jubayerhossain.bdixtester.data.preferences.AppSettings
import com.jubayerhossain.bdixtester.data.preferences.SettingsRepository
import com.jubayerhossain.bdixtester.data.repository.RoomCatalogRepository
import com.jubayerhossain.bdixtester.domain.model.ServerEndpoint
import com.jubayerhossain.bdixtester.domain.model.ServerTestResult
import com.jubayerhossain.bdixtester.feature.servers.ServerBottomTab
import com.jubayerhossain.bdixtester.feature.servers.ServerSortOrder
import com.jubayerhossain.bdixtester.feature.servers.ServerStatusTab
import com.jubayerhossain.bdixtester.feature.servers.ServersViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PerformanceBenchmarkTest {
  private fun msSince(startNanos: Long): Double = (System.nanoTime() - startNanos) / 1_000_000.0

  private fun report(
    metric: String,
    value: Any,
    unit: String = "",
  ) {
    val formatted = if (value is Double) String.format(Locale.US, "%.2f", value) else value.toString()
    println("[PERF_BENCH] $metric=$formatted$unit")
  }

  @Test
  fun benchmark_01_catalogLoadAndAuditSummary_752() {
    // Warmup JIT
    repeat(3) {
      BuiltInCatalog.loadCuratedCatalog()
      BuiltInCatalog.computeCuratedAuditSummary()
    }

    val loadStart = System.nanoTime()
    var loadedCount = 0
    repeat(10) {
      loadedCount = BuiltInCatalog.loadCuratedCatalog().size
    }
    val loadAvgMs = msSince(loadStart) / 10.0

    val auditStart = System.nanoTime()
    var uniqueCount = 0
    repeat(10) {
      uniqueCount = BuiltInCatalog.computeCuratedAuditSummary().uniqueEndpointCount
    }
    val auditAvgMs = msSince(auditStart) / 10.0

    assertEquals(BuiltInCatalog.DEFAULT_CURATED_COUNT, loadedCount)
    assertEquals(BuiltInCatalog.DEFAULT_CURATED_COUNT, uniqueCount)
    report("catalog_load_curated_avg_ms", loadAvgMs, "ms")
    report("catalog_compute_audit_avg_ms", auditAvgMs, "ms")
  }

  @Test
  fun benchmark_02_urlParserAndClassifier_752() {
    val catalog = BuiltInCatalog.loadCuratedCatalog()
    // Warmup
    catalog.forEach {
      UrlParser.canonicalKey(it.originalUrl)
      UrlParser.isValidIpv4Host(it.host)
      ServerDisplayClassifier.classify(it)
      ServerDisplayClassifier.displayLabel(it)
    }

    val canonicalStart = System.nanoTime()
    repeat(10) {
      for (i in catalog.indices) {
        UrlParser.canonicalKey(catalog[i].originalUrl)
      }
    }
    val canonicalMs = msSince(canonicalStart) / 10.0

    val ipv4Start = System.nanoTime()
    repeat(20) {
      for (i in catalog.indices) {
        UrlParser.isValidIpv4Host(catalog[i].host)
      }
    }
    val ipv4Ms = msSince(ipv4Start) / 20.0

    val classifyStart = System.nanoTime()
    repeat(20) {
      for (i in catalog.indices) {
        ServerDisplayClassifier.classify(catalog[i])
        ServerDisplayClassifier.displayLabel(catalog[i])
      }
    }
    val classifyMs = msSince(classifyStart) / 20.0

    report("url_canonical_752x_avg_ms", canonicalMs, "ms")
    report("ipv4_check_752x_avg_ms", ipv4Ms, "ms")
    report("display_classifier_752x_avg_ms", classifyMs, "ms")
  }

  @Test
  fun benchmark_03_appContainerInit_syncCost() {
    val context: Context = ApplicationProvider.getApplicationContext()
    val start = System.nanoTime()
    AppContainer(context)
    val syncInitMs = msSince(start)
    report("app_container_sync_ctor_ms", syncInitMs, "ms")
  }

  @Test
  fun benchmark_04_roomColdSeedAndWarmVerify_752() =
    runBlocking {
      val context: Context = ApplicationProvider.getApplicationContext()
      val db =
        Room
          .inMemoryDatabaseBuilder(context, BdixDatabase::class.java)
          .allowMainThreadQueries()
          .build()
      val repo = RoomCatalogRepository(database = db)

      try {
        val coldStart = System.nanoTime()
        val coldReport = repo.ensureInitialized()
        val coldSeedMs = msSince(coldStart)
        assertTrue(coldReport.isConsistent)
        assertEquals(BuiltInCatalog.DEFAULT_CURATED_COUNT, coldReport.builtInDatabaseCount)

        val warmStart = System.nanoTime()
        val warmReport = repo.ensureInitialized()
        val warmVerifyMs = msSince(warmStart)
        assertTrue(warmReport.isConsistent)

        val list = repo.endpoints.first { it.size == BuiltInCatalog.DEFAULT_CURATED_COUNT }
        assertEquals(BuiltInCatalog.DEFAULT_CURATED_COUNT, list.size)

        report("room_cold_seed_752_ms", coldSeedMs, "ms")
        report("room_warm_verify_752_ms", warmVerifyMs, "ms")
      } finally {
        repo.closeAndJoin()
        db.close()
      }
    }

  @Test
  fun benchmark_05_serversViewModelFilterSort_752() =
    runBlocking {
      val context: Context = ApplicationProvider.getApplicationContext()
      val db =
        Room
          .inMemoryDatabaseBuilder(context, BdixDatabase::class.java)
          .allowMainThreadQueries()
          .build()
      val repo = RoomCatalogRepository(database = db)
      val fakeSettings = InMemorySettingsRepository()

      try {
        repo.ensureInitialized()
        repo.endpoints.first { it.size == BuiltInCatalog.DEFAULT_CURATED_COUNT }
        val noopExecutor =
          object : EndpointProbeExecutor {
            override suspend fun probe(
              endpoint: ServerEndpoint,
              config: EndpointProbeConfig,
            ): ServerTestResult {
              error("Unused")
            }
          }
        val scanCoordinator = ScanCoordinator(repo, noopExecutor)
        val vm = ServersViewModel(repo, scanCoordinator, fakeSettings)

        val collectorJob =
          launch(Dispatchers.Main.immediate) {
            vm.uiState.collect { }
          }
        vm.selectStatusTab(ServerStatusTab.NOT_TESTED)
        vm.uiState.first { it.ftpCount == 542 && it.filteredServers.size == 542 }

        val filterStart = System.nanoTime()
        val queries = listOf("bdix", "dhaka", "ftp", "10.", "tv", "")
        for (q in queries) {
          vm.updateSearchQuery(q)
          vm.uiState.first { it.searchQuery == q }
        }
        vm.selectBottomTab(ServerBottomTab.MOVIE)
        vm.uiState.first { it.selectedBottomTab == ServerBottomTab.MOVIE }
        vm.updateSortOrder(ServerSortOrder.NAME_ASC)
        vm.uiState.first { it.sortOrder == ServerSortOrder.NAME_ASC }
        vm.selectBottomTab(ServerBottomTab.TV)
        vm.uiState.first { it.selectedBottomTab == ServerBottomTab.TV }
        vm.selectBottomTab(ServerBottomTab.OTT)
        vm.uiState.first { it.selectedBottomTab == ServerBottomTab.OTT }
        vm.selectBottomTab(ServerBottomTab.FTP)
        vm.uiState.first { it.selectedBottomTab == ServerBottomTab.FTP }
        val totalFilterSortMs = msSince(filterStart)

        collectorJob.cancel()
        report("servers_vm_filter_sort_cycle_ms", totalFilterSortMs, "ms")
      } finally {
        repo.closeAndJoin()
        db.close()
      }
    }

  @Test
  fun benchmark_06_scan200Endpoints_dbInvalidationsAndProgressEmissions() =
    runBlocking {
      val context: Context = ApplicationProvider.getApplicationContext()
      val db =
        Room
          .inMemoryDatabaseBuilder(context, BdixDatabase::class.java)
          .allowMainThreadQueries()
          .build()
      val repo = RoomCatalogRepository(database = db)

      try {
        repo.ensureInitialized()
        val endpoints = repo.endpoints.first { it.size == BuiltInCatalog.DEFAULT_CURATED_COUNT }.take(200)

        val latestEmissionCount = AtomicInteger(0)
        val progressEmissionCount = AtomicInteger(0)
        val latestCollector =
          launch(Dispatchers.IO) {
            db.testHistoryDao().observeLatestResultPerServer().collect {
              latestEmissionCount.incrementAndGet()
            }
          }
        val progressCollector =
          launch(Dispatchers.IO) {
            repo.scanProgress.collect {
              progressEmissionCount.incrementAndGet()
            }
          }
        delay(40)
        latestEmissionCount.set(0)
        progressEmissionCount.set(0)

        val fastExecutor =
          object : EndpointProbeExecutor {
            override suspend fun probe(
              endpoint: ServerEndpoint,
              config: EndpointProbeConfig,
            ): ServerTestResult {
              delay(8)
              return ServerTestResult(
                serverId = endpoint.id,
                serverNameSnapshot = endpoint.name,
                urlSnapshot = endpoint.originalUrl,
                testedAtEpochMs = System.currentTimeMillis(),
                scanMode = config.scanMode,
                protocol = endpoint.protocol,
                status = TestStatus.REACHABLE,
                httpResponseLatencyMs = 5L,
              )
            }
          }

        val coordinator = ScanCoordinator(repo, fastExecutor)
        val scanStart = System.nanoTime()
        assertTrue(
          coordinator.startScan(
            endpoints = endpoints,
            scanMode = ScanMode.QUICK_SCAN,
            scanScope = ScanScope.ALL_SERVERS,
            settings = AppSettings(maxConcurrency = 5),
          ),
        )
        while (repo.scanProgress.value.isScanning) {
          delay(15)
        }
        val scanWallMs = msSince(scanStart)

        // Wait briefly for Room InvalidationTracker to deliver final query
        delay(150)
        latestCollector.cancel()
        progressCollector.cancel()

        val persistedLatest = repo.latestResults.first { it.size == 200 }
        assertEquals(200, persistedLatest.size)
        assertEquals(ScanSessionState.COMPLETED, repo.scanProgress.value.sessionState)
        assertEquals(200, repo.scanProgress.value.mappedSourceEntryCount)

        report("scan_200_wall_ms", scanWallMs, "ms")
        report("scan_200_db_latest_emissions", latestEmissionCount.get())
        report("scan_200_progress_emissions", progressEmissionCount.get())
      } finally {
        repo.closeAndJoin()
        db.close()
      }
    }

  @Test
  fun benchmark_07_httpEndpointTesterProbe_mockServer60() =
    runBlocking {
      val server = MockWebServer()
      server.dispatcher =
        object : Dispatcher() {
          override fun dispatch(request: RecordedRequest): MockResponse = MockResponse().setResponseCode(200)
        }
      server.start()

      try {
        val url = server.url("/").toString()
        val tester = HttpEndpointTester(OkHttpClient())
        val parsed = UrlParser.parse(url)
        val endpoint =
          ServerEndpoint(
            id = "bench_http",
            name = "bench_http",
            originalUrl = url,
            host = parsed.host ?: "127.0.0.1",
            explicitPort = parsed.explicitPort,
            protocol = parsed.protocol,
            category = com.jubayerhossain.bdixtester.core.model.ServerCategory.FTP_FILES,
            isFeatured = false,
            sourceType = com.jubayerhossain.bdixtester.core.model.CatalogSourceType.BUILT_IN,
            verificationState = com.jubayerhossain.bdixtester.core.model.VerificationState.UNKNOWN,
            lastReviewedAtEpochMs = null,
            speedTestUrl = null,
            isPrivateIp = true,
          )
        val config =
          EndpointProbeConfig(
            scanMode = ScanMode.QUICK_SCAN,
            connectTimeoutMs = 2_500,
            readTimeoutMs = 2_500,
            overallDeadlineMs = 2_500,
          )

        // Warmup 5
        repeat(5) {
          assertEquals(TestStatus.REACHABLE, tester.probe(endpoint, config).status)
        }

        val start = System.nanoTime()
        repeat(60) {
          val r = tester.probe(endpoint, config)
          assertEquals(TestStatus.REACHABLE, r.status)
        }
        val elapsedMs = msSince(start)
        report("http_tester_60_probes_ms", elapsedMs, "ms")
        report("http_tester_per_probe_ms", elapsedMs / 60.0, "ms")
      } finally {
        server.shutdown()
      }
    }

  @Test
  fun benchmark_08_repeatedScanCancelMemoryAndLeakSafety() =
    runBlocking {
      val context: Context = ApplicationProvider.getApplicationContext()
      val db =
        Room
          .inMemoryDatabaseBuilder(context, BdixDatabase::class.java)
          .allowMainThreadQueries()
          .build()
      val repo = RoomCatalogRepository(database = db)

      try {
        repo.ensureInitialized()
        val endpoints = repo.endpoints.first { it.size == BuiltInCatalog.DEFAULT_CURATED_COUNT }.take(40)
        val slowExecutor =
          object : EndpointProbeExecutor {
            override suspend fun probe(
              endpoint: ServerEndpoint,
              config: EndpointProbeConfig,
            ): ServerTestResult {
              delay(200)
              return ServerTestResult(
                serverId = endpoint.id,
                serverNameSnapshot = endpoint.name,
                urlSnapshot = endpoint.originalUrl,
                testedAtEpochMs = System.currentTimeMillis(),
                scanMode = config.scanMode,
                protocol = endpoint.protocol,
                status = TestStatus.REACHABLE,
              )
            }
          }
        val coordinator = ScanCoordinator(repo, slowExecutor)
        val cycleStart = System.nanoTime()
        repeat(5) {
          assertTrue(
            coordinator.startScan(
              endpoints = endpoints,
              scanMode = ScanMode.QUICK_SCAN,
              scanScope = ScanScope.ALL_SERVERS,
              settings = AppSettings(maxConcurrency = 3),
            ),
          )
          delay(25)
          coordinator.stopScan()
          assertFalse(repo.scanProgress.value.isScanning)
          assertEquals(ScanSessionState.CANCELLED, repo.scanProgress.value.sessionState)
        }
        val totalMs = msSince(cycleStart)
        report("repeated_start_cancel_5x_ms", totalMs, "ms")
      } finally {
        repo.closeAndJoin()
        db.close()
      }
    }

  private class InMemorySettingsRepository : SettingsRepository {
    private val state = MutableStateFlow(AppSettings())
    override val settings: Flow<AppSettings> = state.asStateFlow()

    override suspend fun setThemePreset(preset: AppThemePreset) {
      state.value = state.value.copy(themePreset = preset)
    }

    override suspend fun setThemePreference(preference: ThemePreference) {
      state.value = state.value.copy(themePreference = preference)
    }

    override suspend fun setAppLanguage(language: AppLanguage) {
      state.value = state.value.copy(appLanguage = language)
    }

    override suspend fun setDefaultScanMode(scanMode: ScanMode) {
      state.value = state.value.copy(defaultScanMode = scanMode)
    }

    override suspend fun setFullScanTimeouts(
      connectTimeoutMs: Int,
      readTimeoutMs: Int,
    ) {
      state.value =
        state.value.copy(
          fullScanConnectTimeoutMs = connectTimeoutMs,
          fullScanReadTimeoutMs = readTimeoutMs,
        )
    }

    override suspend fun setQuickScanDeadline(deadlineMs: Int) {
      state.value = state.value.copy(quickScanDeadlineMs = deadlineMs)
    }

    override suspend fun setMaxConcurrency(maxConcurrency: Int) {
      state.value = state.value.copy(maxConcurrency = maxConcurrency)
    }

    override suspend fun setAutoCatalogUpdates(enabled: Boolean) {
      state.value = state.value.copy(autoCatalogUpdates = enabled)
    }

    override suspend fun setWifiOnlyCatalogUpdates(wifiOnly: Boolean) {
      state.value = state.value.copy(wifiOnlyCatalogUpdates = wifiOnly)
    }

    override suspend fun setRemoteCatalogUrl(url: String) {
      state.value = state.value.copy(remoteCatalogUrl = url)
    }

    override suspend fun setAllowCrossOriginRemotePayload(allow: Boolean) {
      state.value = state.value.copy(allowCrossOriginRemotePayload = allow)
    }

    override suspend fun setSpeedTestMaxBytes(maxBytes: Long) {
      state.value = state.value.copy(speedTestMaxBytes = maxBytes)
    }

    override suspend fun setHistoryRetentionDays(days: Int) {
      state.value = state.value.copy(historyRetentionDays = days)
    }

    override suspend fun setManualNetworkLabel(label: String) {
      state.value = state.value.copy(manualNetworkLabel = label)
    }

    override suspend fun setReduceAnimations(enabled: Boolean) {
      state.value = state.value.copy(reduceAnimations = enabled)
    }

    override suspend fun resetSettings() {
      state.value = AppSettings()
    }
  }
}
