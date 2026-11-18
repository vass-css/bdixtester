package com.jubayerhossain.bdixtester.data.network

import com.jubayerhossain.bdixtester.core.model.ErrorClassification
import com.jubayerhossain.bdixtester.core.model.ScanMode
import com.jubayerhossain.bdixtester.core.model.ScanScope
import com.jubayerhossain.bdixtester.core.model.ScanSessionState
import com.jubayerhossain.bdixtester.core.model.ServerProtocol
import com.jubayerhossain.bdixtester.core.model.TestStatus
import com.jubayerhossain.bdixtester.data.local.entity.ScanSessionEntity
import com.jubayerhossain.bdixtester.data.preferences.AppSettings
import com.jubayerhossain.bdixtester.data.repository.RoomCatalogRepository
import com.jubayerhossain.bdixtester.domain.model.ScanProgressState
import com.jubayerhossain.bdixtester.domain.model.ServerEndpoint
import com.jubayerhossain.bdixtester.domain.model.ServerTestResult
import com.jubayerhossain.bdixtester.domain.repository.CatalogRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class RoutingEndpointProbeExecutor(
  private val httpTester: EndpointProbeExecutor,
  private val ftpTester: EndpointProbeExecutor,
  private val clock: () -> Long = { System.currentTimeMillis() },
) : EndpointProbeExecutor {
  override suspend fun probe(
    endpoint: ServerEndpoint,
    config: EndpointProbeConfig,
  ): ServerTestResult =
    when (endpoint.protocol) {
      ServerProtocol.HTTP, ServerProtocol.HTTPS -> httpTester.probe(endpoint, config)
      ServerProtocol.FTP, ServerProtocol.FTPS -> ftpTester.probe(endpoint, config)
      ServerProtocol.UNSUPPORTED ->
        ServerTestResult(
          serverId = endpoint.id,
          serverNameSnapshot = endpoint.name,
          urlSnapshot = endpoint.originalUrl,
          testedAtEpochMs = clock(),
          scanMode = config.scanMode,
          protocol = endpoint.protocol,
          status = TestStatus.UNSUPPORTED,
          errorClassification = ErrorClassification.UNSUPPORTED_SCHEME,
          failureMessage = "Scheme is unsupported by connectivity engine.",
          networkProfileLabel = config.networkProfileLabel,
        )
    }
}

class ScanCoordinator(
  private val catalogRepository: CatalogRepository,
  private val probeExecutor: EndpointProbeExecutor,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
  private val clock: () -> Long = { System.currentTimeMillis() },
) {
  private data class ActiveScanMeta(
    val job: Job,
    val scanRunId: String,
    val scanMode: ScanMode,
    val scanScope: ScanScope,
    val totalCatalogEntries: Int,
    val uniqueExactUrlCount: Int,
    val startNanos: Long,
    val startEpoch: Long,
    val finalized: AtomicBoolean = AtomicBoolean(false),
  )

  private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
  private val scanLock = Mutex()
  private var activeScanMeta: ActiveScanMeta? = null

  suspend fun startScan(
    endpoints: List<ServerEndpoint>,
    scanMode: ScanMode,
    scanScope: ScanScope,
    settings: AppSettings,
  ): Boolean {
    return scanLock.withLock {
      if (activeScanMeta?.job?.isActive == true) {
        return@withLock false
      }
      if (endpoints.isEmpty()) {
        return@withLock false
      }

      val scanRunId = "scan_${UUID.randomUUID()}"
      val config = buildProbeConfig(scanMode, settings)
      val concurrency =
        settings.maxConcurrency.coerceIn(
          AppSettings.MIN_CONCURRENCY,
          AppSettings.MAX_CONCURRENCY,
        )
      val groupedByExactUrl = endpoints.groupBy { it.originalUrl }
      val startNanos = System.nanoTime()
      val startEpoch = clock()
      val finalized = AtomicBoolean(false)

      updateProgress(
        ScanProgressState(
          scanRunId = scanRunId,
          sessionState = ScanSessionState.RUNNING,
          isScanning = true,
          scanMode = scanMode,
          scanScope = scanScope,
          totalCount = endpoints.size,
          totalCatalogEntries = endpoints.size,
          uniqueExactUrlCount = groupedByExactUrl.size,
          executedUniqueProbeCount = 0,
          mappedSourceEntryCount = 0,
          queuedCount = groupedByExactUrl.size,
          activeCount = 0,
        ),
      )

      persistSession(
        ScanSessionEntity(
          scanRunId = scanRunId,
          scanMode = scanMode.name,
          scopeLabel = scanScope.name,
          state = ScanSessionState.RUNNING.name,
          totalCatalogEntries = endpoints.size,
          uniqueExactUrlCount = groupedByExactUrl.size,
          executedUniqueProbeCount = 0,
          mappedSourceEntryCount = 0,
          startedAtEpochMs = startEpoch,
          updatedAtEpochMs = startEpoch,
          finishedAtEpochMs = null,
        ),
      )

      val job =
        scope.launch {
          runBatch(
            scanRunId = scanRunId,
            scanMode = scanMode,
            scanScope = scanScope,
            endpoints = endpoints,
            groupedByExactUrl = groupedByExactUrl,
            concurrency = concurrency,
            config = config,
            startNanos = startNanos,
            startEpoch = startEpoch,
            finalized = finalized,
          )
        }
      activeScanMeta =
        ActiveScanMeta(
          job = job,
          scanRunId = scanRunId,
          scanMode = scanMode,
          scanScope = scanScope,
          totalCatalogEntries = endpoints.size,
          uniqueExactUrlCount = groupedByExactUrl.size,
          startNanos = startNanos,
          startEpoch = startEpoch,
          finalized = finalized,
        )
      true
    }
  }

  suspend fun stopScan() {
    val meta =
      scanLock.withLock {
        activeScanMeta
      } ?: return
    meta.job.cancelAndJoin()
    if (meta.finalized.compareAndSet(false, true)) {
      withContext(NonCancellable) {
        val finishedAt = clock()
        val elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - meta.startNanos)
        updateProgress(
          ScanProgressState(
            scanRunId = meta.scanRunId,
            sessionState = ScanSessionState.CANCELLED,
            isScanning = false,
            scanMode = meta.scanMode,
            scanScope = meta.scanScope,
            totalCount = meta.totalCatalogEntries,
            totalCatalogEntries = meta.totalCatalogEntries,
            uniqueExactUrlCount = meta.uniqueExactUrlCount,
            executedUniqueProbeCount = 0,
            mappedSourceEntryCount = 0,
            queuedCount = 0,
            activeCount = 0,
            completedCount = 0,
            reachableCount = 0,
            unreachableCount = 0,
            restrictedOrErrorCount = 0,
            unsupportedCount = 0,
            cancelledCount = meta.totalCatalogEntries,
            elapsedMs = elapsed,
            lastScanCompletedAtEpochMs = finishedAt,
          ),
        )
        persistSession(
          ScanSessionEntity(
            scanRunId = meta.scanRunId,
            scanMode = meta.scanMode.name,
            scopeLabel = meta.scanScope.name,
            state = ScanSessionState.CANCELLED.name,
            totalCatalogEntries = meta.totalCatalogEntries,
            uniqueExactUrlCount = meta.uniqueExactUrlCount,
            executedUniqueProbeCount = 0,
            mappedSourceEntryCount = 0,
            startedAtEpochMs = meta.startEpoch,
            updatedAtEpochMs = finishedAt,
            finishedAtEpochMs = finishedAt,
          ),
        )
        scanLock.withLock {
          if (activeScanMeta?.scanRunId == meta.scanRunId) {
            activeScanMeta = null
          }
        }
      }
    }
  }

  suspend fun testSingleEndpoint(
    endpoint: ServerEndpoint,
    scanMode: ScanMode,
    settings: AppSettings,
  ): ServerTestResult {
    val config = buildProbeConfig(scanMode, settings)
    val probeGroupId = "grp_${UUID.randomUUID()}"
    val scanRunId = "single_${UUID.randomUUID()}"
    val result =
      probeExecutor.probe(endpoint, config).copy(
        scanRunId = scanRunId,
        probeGroupId = probeGroupId,
        isCoalescedReplica = false,
      )
    catalogRepository.recordTestResults(listOf(result))
    return result
  }

  private suspend fun runBatch(
    scanRunId: String,
    scanMode: ScanMode,
    scanScope: ScanScope,
    endpoints: List<ServerEndpoint>,
    groupedByExactUrl: Map<String, List<ServerEndpoint>>,
    concurrency: Int,
    config: EndpointProbeConfig,
    startNanos: Long,
    startEpoch: Long,
    finalized: AtomicBoolean,
  ) {
    val semaphore = Semaphore(concurrency)
    val hostMutexes = ConcurrentHashMap<String, Mutex>()

    val activeProbes = AtomicInteger(0)
    val executedProbes = AtomicInteger(0)
    val mappedEntries = AtomicInteger(0)
    val reachable = AtomicInteger(0)
    val unreachable = AtomicInteger(0)
    val restricted = AtomicInteger(0)
    val unsupported = AtomicInteger(0)
    val cancelled = AtomicInteger(0)
    val lastProgressEmitNanos = AtomicLong(0)

    fun publishProgressThrottled(currentServerName: String) {
      val now = System.nanoTime()
      val last = lastProgressEmitNanos.get()
      if (now - last < TimeUnit.MILLISECONDS.toNanos(PROGRESS_UPDATE_INTERVAL_MS) &&
        last != 0L
      ) {
        return
      }
      if (!lastProgressEmitNanos.compareAndSet(last, now)) return

      val currentActive = activeProbes.get()
      val executed = executedProbes.get()
      val mapped = mappedEntries.get()
      updateProgress(
        ScanProgressState(
          scanRunId = scanRunId,
          sessionState = ScanSessionState.RUNNING,
          isScanning = true,
          scanMode = scanMode,
          scanScope = scanScope,
          totalCount = endpoints.size,
          totalCatalogEntries = endpoints.size,
          uniqueExactUrlCount = groupedByExactUrl.size,
          executedUniqueProbeCount = executed,
          mappedSourceEntryCount = mapped,
          queuedCount = (groupedByExactUrl.size - executed - currentActive).coerceAtLeast(0),
          activeCount = currentActive,
          completedCount = mapped,
          reachableCount = reachable.get(),
          unreachableCount = unreachable.get(),
          restrictedOrErrorCount = restricted.get(),
          unsupportedCount = unsupported.get(),
          cancelledCount = cancelled.get(),
          elapsedMs = TimeUnit.NANOSECONDS.toMillis(now - startNanos),
          currentServerName = currentServerName,
        ),
      )
    }

    var wasCancelled = false
    try {
      supervisorScope {
        val tasks =
          groupedByExactUrl.entries.map { (_, groupEndpoints) ->
            val primaryEndpoint = groupEndpoints.first()
            val probeGroupId = "grp_${UUID.randomUUID()}"
            async {
              semaphore.withPermit {
                val hostLock = hostMutexes.getOrPut(primaryEndpoint.host.lowercase()) { Mutex() }
                hostLock.withLock {
                  activeProbes.incrementAndGet()
                  publishProgressThrottled(primaryEndpoint.name)

                  try {
                    val baseResult = probeExecutor.probe(primaryEndpoint, config)
                    val mappedResults =
                      groupEndpoints.mapIndexed { idx, ep ->
                        baseResult.copy(
                          serverId = ep.id,
                          serverNameSnapshot = ep.name,
                          urlSnapshot = ep.originalUrl,
                          scanRunId = scanRunId,
                          probeGroupId = probeGroupId,
                          isCoalescedReplica = idx > 0,
                        )
                      }
                    catalogRepository.recordTestResults(mappedResults)

                    executedProbes.incrementAndGet()
                    mappedEntries.addAndGet(mappedResults.size)
                    when (baseResult.status) {
                      TestStatus.REACHABLE -> reachable.addAndGet(mappedResults.size)
                      TestStatus.RESTRICTED -> restricted.addAndGet(mappedResults.size)
                      TestStatus.UNSUPPORTED -> unsupported.addAndGet(mappedResults.size)
                      TestStatus.CANCELLED -> cancelled.addAndGet(mappedResults.size)
                      TestStatus.UNREACHABLE, TestStatus.TIMED_OUT ->
                        unreachable.addAndGet(
                          mappedResults.size,
                        )
                      TestStatus.NOT_TESTED, TestStatus.QUEUED, TestStatus.TESTING -> Unit
                    }
                    publishProgressThrottled(primaryEndpoint.name)
                  } finally {
                    activeProbes.decrementAndGet()
                  }
                }
              }
            }
          }
        tasks.awaitAll()
      }
    } catch (_: CancellationException) {
      wasCancelled = true
    } finally {
      withContext(NonCancellable) {
        if (finalized.compareAndSet(false, true)) {
          val finishedAt = clock()
          val elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos)
          val remainingCancelled =
            if (wasCancelled) {
              (endpoints.size - mappedEntries.get()).coerceAtLeast(0)
            } else {
              0
            }
          val totalCancelled = cancelled.get() + remainingCancelled
          val finalState = if (wasCancelled) ScanSessionState.CANCELLED else ScanSessionState.COMPLETED

          updateProgress(
            ScanProgressState(
              scanRunId = scanRunId,
              sessionState = finalState,
              isScanning = false,
              scanMode = scanMode,
              scanScope = scanScope,
              totalCount = endpoints.size,
              totalCatalogEntries = endpoints.size,
              uniqueExactUrlCount = groupedByExactUrl.size,
              executedUniqueProbeCount = executedProbes.get(),
              mappedSourceEntryCount = mappedEntries.get(),
              queuedCount = 0,
              activeCount = 0,
              completedCount = mappedEntries.get(),
              reachableCount = reachable.get(),
              unreachableCount = unreachable.get(),
              restrictedOrErrorCount = restricted.get(),
              unsupportedCount = unsupported.get(),
              cancelledCount = totalCancelled,
              elapsedMs = elapsed,
              lastScanCompletedAtEpochMs = finishedAt,
            ),
          )

          persistSession(
            ScanSessionEntity(
              scanRunId = scanRunId,
              scanMode = scanMode.name,
              scopeLabel = scanScope.name,
              state = finalState.name,
              totalCatalogEntries = endpoints.size,
              uniqueExactUrlCount = groupedByExactUrl.size,
              executedUniqueProbeCount = executedProbes.get(),
              mappedSourceEntryCount = mappedEntries.get(),
              startedAtEpochMs = startEpoch,
              updatedAtEpochMs = finishedAt,
              finishedAtEpochMs = finishedAt,
            ),
          )
        }

        scanLock.withLock {
          if (activeScanMeta?.scanRunId == scanRunId) {
            activeScanMeta = null
          }
        }
      }
    }
  }

  private fun buildProbeConfig(
    scanMode: ScanMode,
    settings: AppSettings,
  ): EndpointProbeConfig {
    val label = settings.manualNetworkLabel.takeIf { it.isNotBlank() }
    return if (scanMode == ScanMode.QUICK_SCAN) {
      val quick = settings.quickScanDeadlineMs
      EndpointProbeConfig(
        scanMode = ScanMode.QUICK_SCAN,
        connectTimeoutMs = quick,
        readTimeoutMs = quick,
        overallDeadlineMs = quick,
        networkProfileLabel = label,
      )
    } else {
      EndpointProbeConfig(
        scanMode = ScanMode.FULL_SCAN,
        connectTimeoutMs = settings.fullScanConnectTimeoutMs,
        readTimeoutMs = settings.fullScanReadTimeoutMs,
        overallDeadlineMs = AppSettings.DEFAULT_FULL_REQUEST_DEADLINE_MS,
        networkProfileLabel = label,
      )
    }
  }

  private fun updateProgress(state: ScanProgressState) {
    (catalogRepository as? RoomCatalogRepository)?.updateLiveScanProgress(state)
  }

  private suspend fun persistSession(session: ScanSessionEntity) {
    (catalogRepository as? RoomCatalogRepository)?.saveScanSession(session)
  }

  private companion object {
    const val PROGRESS_UPDATE_INTERVAL_MS = 250L
  }
}
