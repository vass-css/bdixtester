package com.jubayerhossain.bdixtester.data.repository

import androidx.room.withTransaction
import com.jubayerhossain.bdixtester.core.model.CatalogAuditSummary
import com.jubayerhossain.bdixtester.core.model.CatalogSourceType
import com.jubayerhossain.bdixtester.core.model.ScanSessionState
import com.jubayerhossain.bdixtester.core.model.ServerCategory
import com.jubayerhossain.bdixtester.core.model.ServerProtocol
import com.jubayerhossain.bdixtester.core.model.VerificationState
import com.jubayerhossain.bdixtester.core.validation.UrlParser
import com.jubayerhossain.bdixtester.data.catalog.BuiltInCatalog
import com.jubayerhossain.bdixtester.data.local.BdixDatabase
import com.jubayerhossain.bdixtester.data.local.entity.FavoriteEntity
import com.jubayerhossain.bdixtester.data.local.entity.ScanSessionEntity
import com.jubayerhossain.bdixtester.data.local.toDomain
import com.jubayerhossain.bdixtester.data.local.toEntity
import com.jubayerhossain.bdixtester.domain.model.CatalogIntegrityReport
import com.jubayerhossain.bdixtester.domain.model.CatalogSyncState
import com.jubayerhossain.bdixtester.domain.model.FavoriteInfo
import com.jubayerhossain.bdixtester.domain.model.ScanProgressState
import com.jubayerhossain.bdixtester.domain.model.ServerEndpoint
import com.jubayerhossain.bdixtester.domain.model.ServerTestResult
import com.jubayerhossain.bdixtester.domain.repository.CatalogRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

class RoomCatalogRepository(
  private val database: BdixDatabase,
  private val rawBuiltInLinks: List<String>? = null,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
  private val clock: () -> Long = { System.currentTimeMillis() },
) : CatalogRepository {
  private val repoJob: Job = SupervisorJob()
  private val repoScope = CoroutineScope(repoJob + ioDispatcher)
  private val initMutex = Mutex()
  private var cachedIntegrityReport: CatalogIntegrityReport? = null
  private val serverDao = database.serverDao()
  private val favoriteDao = database.favoriteDao()
  private val historyDao = database.testHistoryDao()
  private val scanSessionDao = database.scanSessionDao()
  private val metadataDao = database.catalogMetadataDao()

  override val endpoints: StateFlow<List<ServerEndpoint>> =
    serverDao
      .observeAllServers()
      .map { rows -> rows.map { it.toDomain() } }
      .stateIn(repoScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  override val latestResults: StateFlow<Map<String, ServerTestResult>> =
    historyDao
      .observeLatestResultPerServer()
      .map { rows -> rows.associate { it.serverId to it.toDomain() } }
      .stateIn(repoScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

  override val favorites: StateFlow<Map<String, FavoriteInfo>> =
    favoriteDao
      .observeAllFavorites()
      .map { rows -> rows.associate { it.serverId to it.toDomain() } }
      .stateIn(repoScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

  override val history: StateFlow<List<ServerTestResult>> =
    historyDao
      .observeAllHistory()
      .map { rows -> rows.map { it.toDomain() } }
      .stateIn(repoScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  private val _scanProgress = MutableStateFlow(ScanProgressState())
  override val scanProgress: StateFlow<ScanProgressState> = _scanProgress.asStateFlow()

  private val _auditSummary =
    MutableStateFlow(
      if (rawBuiltInLinks == null) {
        BuiltInCatalog.computeCuratedAuditSummary()
      } else {
        BuiltInCatalog.computeAuditSummary(rawBuiltInLinks)
      },
    )
  override val auditSummary: StateFlow<CatalogAuditSummary> = _auditSummary.asStateFlow()

  override val catalogSyncState: StateFlow<CatalogSyncState> =
    metadataDao
      .observeMetadata(REMOTE_FEED_ID)
      .map { it?.toDomain() ?: CatalogSyncState(feedId = REMOTE_FEED_ID) }
      .stateIn(
        repoScope,
        SharingStarted.WhileSubscribed(5_000),
        CatalogSyncState(feedId = REMOTE_FEED_ID),
      )

  override val lastCatalogUpdateEpochMs: StateFlow<Long?> =
    catalogSyncState
      .map { it.lastSuccessfulSyncAtEpochMs }
      .stateIn(repoScope, SharingStarted.WhileSubscribed(5_000), null)

  private val _catalogIntegrity = MutableStateFlow<CatalogIntegrityReport?>(null)
  override val catalogIntegrity: StateFlow<CatalogIntegrityReport?> = _catalogIntegrity.asStateFlow()

  override suspend fun ensureInitialized(): CatalogIntegrityReport =
    withContext(ioDispatcher) {
      initMutex.withLock {
        cachedIntegrityReport?.let { return@withLock it }

        val now = clock()
        val expectedSource =
          if (rawBuiltInLinks == null) {
            BuiltInCatalog.loadCuratedCatalog()
          } else {
            BuiltInCatalog.parseSourceCatalog(rawBuiltInLinks)
          }

        database.withTransaction {
          val existingBuiltIn = serverDao.getBuiltInServersOrdered().map { it.toDomain() }
          val currentIntegrity = compareSourceToDatabase(expectedSource, existingBuiltIn)
          if (!currentIntegrity.isConsistent && expectedSource.isNotEmpty()) {
            val entities = expectedSource.map { it.toEntity(nowEpochMs = now) }
            serverDao.syncBuiltInServersAtomically(entities)
          }
          val runningSessions = scanSessionDao.getRunningSessions()
          if (runningSessions.isNotEmpty()) {
            scanSessionDao.markRunningSessionsAs(
              newState = ScanSessionState.INTERRUPTED.name,
              updatedAtEpochMs = now,
              finishedAtEpochMs = now,
            )
          }
        }

        val persistedBuiltIn = serverDao.getBuiltInServersOrdered()
        val report = compareSourceToDatabase(expectedSource, persistedBuiltIn.map { it.toDomain() })
        _catalogIntegrity.value = report
        cachedIntegrityReport = report

        val currentProgress = _scanProgress.value
        if (!currentProgress.isScanning && currentProgress.totalCatalogEntries == 0) {
          val latestSession = scanSessionDao.getRunningSessions().firstOrNull()
          _scanProgress.value =
            currentProgress.copy(
              totalCount = persistedBuiltIn.size,
              totalCatalogEntries = persistedBuiltIn.size,
              sessionState =
                if (latestSession !=
                  null
                ) {
                  ScanSessionState.INTERRUPTED
                } else {
                  currentProgress.sessionState
                },
            )
        }
        report
      }
    }

  internal fun updateLiveScanProgress(state: ScanProgressState) {
    _scanProgress.value = state
  }

  internal suspend fun saveScanSession(session: ScanSessionEntity) =
    withContext(ioDispatcher) {
      scanSessionDao.upsertSession(session)
    }

  private fun compareSourceToDatabase(
    source: List<ServerEndpoint>,
    persisted: List<ServerEndpoint>,
  ): CatalogIntegrityReport {
    if (source.size != persisted.size) {
      return CatalogIntegrityReport(
        builtInSourceCount = source.size,
        builtInDatabaseCount = persisted.size,
        orderMatches = false,
        exactUrlsMatch = false,
        isConsistent = false,
        mismatchDetails = "Count mismatch: source=${source.size}, db=${persisted.size}",
      )
    }
    var orderMatches = true
    var exactUrlsMatch = true
    var metadataMatches = true
    var firstMismatch: String? = null
    for (i in source.indices) {
      val s = source[i]
      val p = persisted[i]
      if (p.sourceIndex != i || p.id != s.id) {
        orderMatches = false
        if (firstMismatch == null) {
          firstMismatch = "Index/ID mismatch at $i: expected ${s.id}, found ${p.id} (idx=${p.sourceIndex})"
        }
      }
      if (p.originalUrl != s.originalUrl) {
        exactUrlsMatch = false
        if (firstMismatch == null) {
          firstMismatch = "URL mismatch at $i: expected ${s.originalUrl}, found ${p.originalUrl}"
        }
      }
      if (p.name != s.name || p.category != s.category || p.sourceAttribution != s.sourceAttribution) {
        metadataMatches = false
        if (firstMismatch == null) {
          firstMismatch =
            "Metadata mismatch at $i: expected ${s.name}/${s.category}, found ${p.name}/${p.category}"
        }
      }
    }
    return CatalogIntegrityReport(
      builtInSourceCount = source.size,
      builtInDatabaseCount = persisted.size,
      orderMatches = orderMatches,
      exactUrlsMatch = exactUrlsMatch,
      isConsistent = orderMatches && exactUrlsMatch && metadataMatches,
      mismatchDetails = firstMismatch,
    )
  }

  override fun getEndpointById(serverId: String): ServerEndpoint? = endpoints.value.firstOrNull { it.id == serverId }

  override fun observeHistoryForServer(serverId: String): Flow<List<ServerTestResult>> =
    historyDao.observeHistoryForServer(serverId).map { rows ->
      rows.map {
        it.toDomain()
      }
    }

  override suspend fun toggleFavorite(serverId: String) =
    withContext(ioDispatcher) {
      val existing = favoriteDao.getFavorite(serverId)
      if (existing != null) {
        favoriteDao.deleteFavorite(serverId)
      } else {
        favoriteDao.upsertFavorite(
          FavoriteEntity(
            serverId = serverId,
            isPinned = false,
            displayOrder = 0,
            addedAtEpochMs = clock(),
            note = null,
          ),
        )
      }
    }

  override suspend fun restoreFavorite(favorite: FavoriteInfo) =
    withContext(ioDispatcher) {
      favoriteDao.upsertFavorite(
        FavoriteEntity(
          serverId = favorite.serverId,
          isPinned = favorite.isPinned,
          displayOrder = favorite.displayOrder,
          addedAtEpochMs = favorite.addedAtEpochMs,
          note = favorite.note,
        ),
      )
    }

  override suspend fun setFavoritePinned(
    serverId: String,
    pinned: Boolean,
  ) = withContext(ioDispatcher) {
    val existing = favoriteDao.getFavorite(serverId)
    val now = clock()
    if (existing != null) {
      favoriteDao.upsertFavorite(existing.copy(isPinned = pinned))
    } else {
      favoriteDao.upsertFavorite(
        FavoriteEntity(
          serverId = serverId,
          isPinned = pinned,
          displayOrder = 0,
          addedAtEpochMs = now,
          note = null,
        ),
      )
    }
  }

  override suspend fun clearAllFavorites() =
    withContext(ioDispatcher) {
      favoriteDao.clearAllFavorites()
    }

  override suspend fun saveCustomServer(
    existingId: String?,
    name: String,
    rawUrl: String,
    category: ServerCategory,
    speedTestUrl: String?,
  ): Result<ServerEndpoint> =
    withContext(ioDispatcher) {
      val trimmedUrl = rawUrl.trim()
      val parsed = UrlParser.parse(trimmedUrl)
      if (!parsed.isValid || parsed.host == null || parsed.protocol == ServerProtocol.UNSUPPORTED) {
        return@withContext Result.failure(
          IllegalArgumentException("Enter a valid http://, https://, ftp://, or ftps:// endpoint URL."),
        )
      }
      val cleanedSpeedUrl = speedTestUrl?.trim()?.takeIf { it.isNotEmpty() }
      if (cleanedSpeedUrl != null) {
        val parsedSpeed = UrlParser.parse(cleanedSpeedUrl)
        if (!parsedSpeed.isValid ||
          (parsedSpeed.protocol != ServerProtocol.HTTP && parsedSpeed.protocol != ServerProtocol.HTTPS)
        ) {
          return@withContext Result.failure(
            IllegalArgumentException("Speed-test resource URL must be a valid http:// or https:// URL."),
          )
        }
      }

      val effectiveCategory =
        when (category) {
          ServerCategory.ALL, ServerCategory.UNCATEGORIZED -> ServerCategory.FTP_FILES
          else -> category
        }
      val effectiveName =
        name.trim().ifEmpty {
          BuiltInCatalog.buildDisplayName(parsed.host, parsed.explicitPort)
        }
      val id = existingId?.takeIf { it.startsWith("custom:") } ?: "custom:${UUID.randomUUID()}"
      val now = clock()
      val existingEntity = serverDao.getServerById(id)
      if (existingEntity != null && existingEntity.sourceType != CatalogSourceType.USER_CUSTOM.name) {
        return@withContext Result.failure(
          IllegalStateException("Built-in or remote catalog entries cannot be overwritten as custom servers."),
        )
      }

      val candidateCanonicalKey = UrlParser.canonicalKey(trimmedUrl)
      val duplicateServer =
        serverDao.getAllServersOnce().firstOrNull { existingRow ->
          existingRow.id != id && UrlParser.canonicalKey(existingRow.originalUrl) == candidateCanonicalKey
        }
      if (duplicateServer != null) {
        return@withContext Result.failure(
          IllegalArgumentException(
            "Duplicate server not allowed: already exists as \"${duplicateServer.name}\" (${duplicateServer.originalUrl}).",
          ),
        )
      }

      val endpoint =
        ServerEndpoint(
          id = id,
          name = effectiveName,
          originalUrl = trimmedUrl,
          host = parsed.host,
          explicitPort = parsed.explicitPort,
          protocol = parsed.protocol,
          category = effectiveCategory,
          isFeatured = false,
          sourceType = CatalogSourceType.USER_CUSTOM,
          sourceIndex = null,
          verificationState = VerificationState.UNKNOWN,
          lastReviewedAtEpochMs = null,
          description = null,
          sourceAttribution = "User Custom Endpoint",
          speedTestUrl = cleanedSpeedUrl,
          speedTestMaxBytes = null,
          isPrivateIp = parsed.isPrivateIp,
        )
      serverDao.upsertServer(
        endpoint.toEntity(
          nowEpochMs = now,
          createdAtEpochMs = existingEntity?.createdAtEpochMs ?: now,
        ),
      )
      Result.success(endpoint)
    }

  override suspend fun deleteCustomServer(serverId: String) =
    withContext(ioDispatcher) {
      if (serverId.startsWith("custom:")) {
        serverDao.deleteCustomServerById(serverId)
        favoriteDao.deleteFavorite(serverId)
      }
    }

  override suspend fun deleteAllCustomServers() =
    withContext(ioDispatcher) {
      val customIds =
        endpoints.value
          .filter { it.sourceType == CatalogSourceType.USER_CUSTOM }
          .map { it.id }
      serverDao.deleteAllCustomServers()
      customIds.forEach { favoriteDao.deleteFavorite(it) }
    }

  override suspend fun recordTestResults(results: List<ServerTestResult>) =
    withContext(ioDispatcher) {
      if (results.isEmpty()) return@withContext
      historyDao.insertHistoryBatch(results.map { it.toEntity() })
    }

  override suspend fun deleteHistoryEntry(historyId: Long) =
    withContext(ioDispatcher) {
      historyDao.deleteHistoryById(historyId)
    }

  override suspend fun restoreHistoryEntry(result: ServerTestResult) =
    withContext(ioDispatcher) {
      historyDao.insertHistoryBatch(listOf(result.toEntity()))
    }

  override suspend fun clearAllHistory() =
    withContext(ioDispatcher) {
      historyDao.clearAllHistory()
      scanSessionDao.clearAllSessions()
    }

  override suspend fun pruneExpiredHistory(retentionDays: Int) =
    withContext(ioDispatcher) {
      val cutoff = clock() - retentionDays.coerceAtLeast(1) * MILLIS_PER_DAY
      historyDao.deleteOlderThan(cutoff)
    }

  override suspend fun resetAllLocalUserData() =
    withContext(ioDispatcher) {
      database.withTransaction {
        historyDao.clearAllHistory()
        scanSessionDao.clearAllSessions()
        favoriteDao.clearAllFavorites()
        serverDao.deleteAllCustomServers()
        serverDao.deleteRemoteCatalogServers()
        metadataDao.deleteMetadata(REMOTE_FEED_ID)
      }
    }

  suspend fun closeAndJoin() {
    repoJob.cancelAndJoin()
  }

  companion object {
    const val REMOTE_FEED_ID = "remote_primary"
    private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
  }
}
