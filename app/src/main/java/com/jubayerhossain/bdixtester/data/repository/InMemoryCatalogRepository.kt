package com.jubayerhossain.bdixtester.data.repository

import com.jubayerhossain.bdixtester.core.model.CatalogAuditSummary
import com.jubayerhossain.bdixtester.core.model.ConfidenceLevel
import com.jubayerhossain.bdixtester.core.model.EndpointGovernanceRecord
import com.jubayerhossain.bdixtester.core.model.GovernanceDecision
import com.jubayerhossain.bdixtester.core.model.ServerCategory
import com.jubayerhossain.bdixtester.core.model.VerificationEventRecord
import com.jubayerhossain.bdixtester.core.model.VerificationLifecycleStatus
import com.jubayerhossain.bdixtester.core.model.VerificationRunProgress
import com.jubayerhossain.bdixtester.data.catalog.BuiltInCatalog
import com.jubayerhossain.bdixtester.domain.model.CatalogIntegrityReport
import com.jubayerhossain.bdixtester.domain.model.CatalogSyncState
import com.jubayerhossain.bdixtester.domain.model.FavoriteInfo
import com.jubayerhossain.bdixtester.domain.model.ScanProgressState
import com.jubayerhossain.bdixtester.domain.model.ServerEndpoint
import com.jubayerhossain.bdixtester.domain.model.ServerTestResult
import com.jubayerhossain.bdixtester.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * Lightweight in-memory [CatalogRepository] retained strictly for isolated Compose previews and unit tests.
 * Production runtime uses [RoomCatalogRepository].
 */
class InMemoryCatalogRepository(
  rawLinks: List<String>? = null,
) : CatalogRepository {
  private val parsedEndpoints: List<ServerEndpoint> =
    if (rawLinks == null) {
      BuiltInCatalog.loadCuratedCatalog()
    } else {
      BuiltInCatalog.parseSourceCatalog(rawLinks)
    }

  private val _allCatalogEndpoints = MutableStateFlow(parsedEndpoints)
  override val allCatalogEndpoints: StateFlow<List<ServerEndpoint>> = _allCatalogEndpoints.asStateFlow()

  private val _governanceRecords =
    MutableStateFlow(
      parsedEndpoints.associate { ep ->
        ep.id to EndpointGovernanceRecord(serverId = ep.id)
      },
    )
  override val governanceRecords: StateFlow<Map<String, EndpointGovernanceRecord>> = _governanceRecords.asStateFlow()

  private val _verificationRunProgress = MutableStateFlow(VerificationRunProgress())
  override val verificationRunProgress: StateFlow<VerificationRunProgress> = _verificationRunProgress.asStateFlow()

  private val verificationEvents = MutableStateFlow<List<VerificationEventRecord>>(emptyList())

  private val _endpoints = MutableStateFlow(parsedEndpoints)
  override val endpoints: StateFlow<List<ServerEndpoint>> = _endpoints.asStateFlow()

  private val _latestResults = MutableStateFlow<Map<String, ServerTestResult>>(emptyMap())
  override val latestResults: StateFlow<Map<String, ServerTestResult>> = _latestResults.asStateFlow()

  private val _favorites = MutableStateFlow<Map<String, FavoriteInfo>>(emptyMap())
  override val favorites: StateFlow<Map<String, FavoriteInfo>> = _favorites.asStateFlow()

  private val _history = MutableStateFlow<List<ServerTestResult>>(emptyList())
  override val history: StateFlow<List<ServerTestResult>> = _history.asStateFlow()

  private val _scanProgress =
    MutableStateFlow(
      ScanProgressState(
        isScanning = false,
        scanMode = null,
        totalCount = parsedEndpoints.size,
        totalCatalogEntries = parsedEndpoints.size,
      ),
    )
  override val scanProgress: StateFlow<ScanProgressState> = _scanProgress.asStateFlow()

  private val _auditSummary =
    MutableStateFlow(
      if (rawLinks == null) {
        BuiltInCatalog.computeCuratedAuditSummary()
      } else {
        BuiltInCatalog.computeAuditSummary(rawLinks)
      },
    )
  override val auditSummary: StateFlow<CatalogAuditSummary> = _auditSummary.asStateFlow()

  private val _catalogSyncState = MutableStateFlow(CatalogSyncState())
  override val catalogSyncState: StateFlow<CatalogSyncState> = _catalogSyncState.asStateFlow()

  private val _lastCatalogUpdateEpochMs = MutableStateFlow<Long?>(null)
  override val lastCatalogUpdateEpochMs: StateFlow<Long?> = _lastCatalogUpdateEpochMs.asStateFlow()

  private val _catalogIntegrity =
    MutableStateFlow<CatalogIntegrityReport?>(
      CatalogIntegrityReport(
        builtInSourceCount = parsedEndpoints.size,
        builtInDatabaseCount = parsedEndpoints.size,
        orderMatches = true,
        exactUrlsMatch = true,
        isConsistent = true,
      ),
    )
  override val catalogIntegrity: StateFlow<CatalogIntegrityReport?> = _catalogIntegrity.asStateFlow()

  override suspend fun ensureInitialized(): CatalogIntegrityReport = _catalogIntegrity.value!!

  override suspend fun refreshBuiltInCatalog(): CatalogIntegrityReport = _catalogIntegrity.value!!

  override fun getEndpointById(serverId: String): ServerEndpoint? =
    _endpoints.value.firstOrNull { it.id == serverId }
      ?: _allCatalogEndpoints.value.firstOrNull { it.id == serverId }

  override fun observeHistoryForServer(serverId: String): Flow<List<ServerTestResult>> =
    _history.map { list -> list.filter { it.serverId == serverId } }

  override fun observeVerificationEvents(serverId: String): Flow<List<VerificationEventRecord>> =
    verificationEvents.map { list -> list.filter { it.serverId == serverId } }

  override suspend fun toggleFavorite(serverId: String) {
    val current = _favorites.value.toMutableMap()
    if (current.containsKey(serverId)) {
      current.remove(serverId)
    } else {
      current[serverId] =
        FavoriteInfo(
          serverId = serverId,
          isPinned = false,
          displayOrder = 0,
          addedAtEpochMs = System.currentTimeMillis(),
          note = null,
        )
    }
    _favorites.value = current
  }

  override suspend fun restoreFavorite(favorite: FavoriteInfo) {
    _favorites.value = _favorites.value + (favorite.serverId to favorite)
  }

  override suspend fun setFavoritePinned(
    serverId: String,
    pinned: Boolean,
  ) {
    val existing = _favorites.value[serverId]
    val updated =
      existing?.copy(isPinned = pinned) ?: FavoriteInfo(
        serverId = serverId,
        isPinned = pinned,
        displayOrder = 0,
        addedAtEpochMs = System.currentTimeMillis(),
        note = null,
      )
    _favorites.value = _favorites.value + (serverId to updated)
  }

  override suspend fun clearAllFavorites() {
    _favorites.value = emptyMap()
  }

  override suspend fun saveCustomServer(
    existingId: String?,
    name: String,
    rawUrl: String,
    category: ServerCategory,
    speedTestUrl: String?,
  ): Result<ServerEndpoint> = Result.failure(UnsupportedOperationException("In-memory preview repository"))

  override suspend fun deleteCustomServer(serverId: String) = Unit

  override suspend fun deleteAllCustomServers() = Unit

  override suspend fun recordTestResults(results: List<ServerTestResult>) {
    if (results.isEmpty()) return
    val nextLatest = _latestResults.value.toMutableMap()
    results.forEach { nextLatest[it.serverId] = it }
    _latestResults.value = nextLatest
    _history.value = (results + _history.value).sortedByDescending { it.testedAtEpochMs }
  }

  override suspend fun deleteHistoryEntry(historyId: Long) {
    _history.value = _history.value.filterNot { it.historyId == historyId }
  }

  override suspend fun restoreHistoryEntry(result: ServerTestResult) {
    val withoutDup = _history.value.filterNot { it.historyId == result.historyId }
    _history.value = (withoutDup + result).sortedByDescending { it.testedAtEpochMs }
  }

  override suspend fun clearAllHistory() {
    _history.value = emptyList()
    _latestResults.value = emptyMap()
  }

  override suspend fun pruneExpiredHistory(retentionDays: Int) = Unit

  override suspend fun applyGovernanceDecision(
    decision: GovernanceDecision,
    triggerSource: String,
  ) {
    applyGovernanceDecisions(listOf(decision), triggerSource)
  }

  override suspend fun applyGovernanceDecisions(
    decisions: List<GovernanceDecision>,
    triggerSource: String,
  ) {
    if (decisions.isEmpty()) return
    val map = _governanceRecords.value.toMutableMap()
    val events = verificationEvents.value.toMutableList()
    decisions.forEach { d ->
      val prev = map[d.serverId]?.lifecycleStatus ?: VerificationLifecycleStatus.PENDING_VERIFICATION
      map[d.serverId] =
        EndpointGovernanceRecord(
          serverId = d.serverId,
          lifecycleStatus = d.status,
          confidence = d.confidence,
          reason = d.reason,
          recommendedAction = d.recommendedAction,
          removalEligible = d.removalEligible,
          normalizedDomain = d.normalizedDomain,
          dnsSummary =
            d.dns?.summary ?: map[d.serverId]?.dnsSummary
              ?: com.jubayerhossain.bdixtester.core.model.DnsSummary.NOT_CHECKED,
          registrationStatus =
            d.registration?.lookupStatus
              ?: map[d.serverId]?.registrationStatus
              ?: com.jubayerhossain.bdixtester.core.model.RegistrationLookupStatus.NOT_CHECKED,
          checkedSources = d.checkedSources,
          observedEvidence = d.observedEvidence,
          missingEvidence = d.missingEvidence,
          dns = d.dns,
          registration = d.registration,
          http = d.http,
          ftp = d.ftp,
          ispProbes = d.ispProbes,
          recheckAfterEpochMs = d.recheckAfterEpochMs,
          lastVerifiedAtEpochMs = d.checkedAtEpochMs,
          archivedAtEpochMs = if (d.status == VerificationLifecycleStatus.ARCHIVED) d.checkedAtEpochMs else null,
          archivedByUser = d.status == VerificationLifecycleStatus.ARCHIVED,
          updatedAtEpochMs = d.checkedAtEpochMs,
        )
      events.add(
        0,
        VerificationEventRecord(
          eventId = events.size + 1L,
          serverId = d.serverId,
          previousStatus = prev,
          newStatus = d.status,
          confidence = d.confidence,
          triggerSource = triggerSource,
          summary = d.reason,
          createdAtEpochMs = d.checkedAtEpochMs,
        ),
      )
    }
    _governanceRecords.value = map
    verificationEvents.value = events
    recomputeVisibleEndpoints()
  }

  override suspend fun markRemovalCandidate(
    serverId: String,
    reason: String,
  ) {
    val now = System.currentTimeMillis()
    val current = _governanceRecords.value[serverId] ?: EndpointGovernanceRecord(serverId = serverId)
    val updated =
      current.copy(
        lifecycleStatus = VerificationLifecycleStatus.REMOVAL_CANDIDATE,
        confidence = ConfidenceLevel.MEDIUM,
        reason = reason,
        removalEligible = true,
        updatedAtEpochMs = now,
      )
    _governanceRecords.value = _governanceRecords.value + (serverId to updated)
    verificationEvents.value =
      listOf(
        VerificationEventRecord(
          eventId = verificationEvents.value.size + 1L,
          serverId = serverId,
          previousStatus = current.lifecycleStatus,
          newStatus = VerificationLifecycleStatus.REMOVAL_CANDIDATE,
          confidence = ConfidenceLevel.MEDIUM,
          triggerSource = "user_candidate",
          summary = reason,
          createdAtEpochMs = now,
        ),
      ) + verificationEvents.value
    recomputeVisibleEndpoints()
  }

  override suspend fun archiveEndpoint(
    serverId: String,
    reason: String,
  ) {
    val now = System.currentTimeMillis()
    val current = _governanceRecords.value[serverId] ?: EndpointGovernanceRecord(serverId = serverId)
    val updated =
      current.copy(
        lifecycleStatus = VerificationLifecycleStatus.ARCHIVED,
        confidence = ConfidenceLevel.HIGH,
        reason = reason,
        archivedAtEpochMs = now,
        archivedByUser = true,
        updatedAtEpochMs = now,
      )
    _governanceRecords.value = _governanceRecords.value + (serverId to updated)
    verificationEvents.value =
      listOf(
        VerificationEventRecord(
          eventId = verificationEvents.value.size + 1L,
          serverId = serverId,
          previousStatus = current.lifecycleStatus,
          newStatus = VerificationLifecycleStatus.ARCHIVED,
          confidence = ConfidenceLevel.HIGH,
          triggerSource = "user_archive",
          summary = reason,
          createdAtEpochMs = now,
        ),
      ) + verificationEvents.value
    recomputeVisibleEndpoints()
  }

  override suspend fun restoreEndpoint(
    serverId: String,
    reason: String,
  ) {
    val now = System.currentTimeMillis()
    val current = _governanceRecords.value[serverId] ?: EndpointGovernanceRecord(serverId = serverId)
    val updated =
      current.copy(
        lifecycleStatus = VerificationLifecycleStatus.ACTIVE,
        confidence = ConfidenceLevel.MEDIUM,
        reason = reason,
        removalEligible = false,
        archivedAtEpochMs = null,
        archivedByUser = false,
        updatedAtEpochMs = now,
      )
    _governanceRecords.value = _governanceRecords.value + (serverId to updated)
    verificationEvents.value =
      listOf(
        VerificationEventRecord(
          eventId = verificationEvents.value.size + 1L,
          serverId = serverId,
          previousStatus = current.lifecycleStatus,
          newStatus = VerificationLifecycleStatus.ACTIVE,
          confidence = ConfidenceLevel.MEDIUM,
          triggerSource = "user_restore",
          summary = reason,
          createdAtEpochMs = now,
        ),
      ) + verificationEvents.value
    recomputeVisibleEndpoints()
  }

  override fun updateVerificationProgress(progress: VerificationRunProgress) {
    _verificationRunProgress.value = progress
  }

  private fun recomputeVisibleEndpoints() {
    val gov = _governanceRecords.value
    _endpoints.value =
      _allCatalogEndpoints.value.filter {
        gov[it.id]?.lifecycleStatus != VerificationLifecycleStatus.ARCHIVED
      }
  }

  override suspend fun resetAllLocalUserData() {
    _favorites.value = emptyMap()
    _history.value = emptyList()
    _latestResults.value = emptyMap()
  }
}
