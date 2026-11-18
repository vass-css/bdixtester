package com.jubayerhossain.bdixtester.domain.repository

import com.jubayerhossain.bdixtester.core.model.CatalogAuditSummary
import com.jubayerhossain.bdixtester.core.model.ServerCategory
import com.jubayerhossain.bdixtester.domain.model.CatalogIntegrityReport
import com.jubayerhossain.bdixtester.domain.model.CatalogSyncState
import com.jubayerhossain.bdixtester.domain.model.FavoriteInfo
import com.jubayerhossain.bdixtester.domain.model.ScanProgressState
import com.jubayerhossain.bdixtester.domain.model.ServerEndpoint
import com.jubayerhossain.bdixtester.domain.model.ServerTestResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface CatalogRepository {
  val endpoints: StateFlow<List<ServerEndpoint>>
  val latestResults: StateFlow<Map<String, ServerTestResult>>
  val favorites: StateFlow<Map<String, FavoriteInfo>>
  val history: StateFlow<List<ServerTestResult>>
  val scanProgress: StateFlow<ScanProgressState>
  val auditSummary: StateFlow<CatalogAuditSummary>
  val catalogSyncState: StateFlow<CatalogSyncState>
  val lastCatalogUpdateEpochMs: StateFlow<Long?>
  val catalogIntegrity: StateFlow<CatalogIntegrityReport?>

  suspend fun ensureInitialized(): CatalogIntegrityReport

  fun getEndpointById(serverId: String): ServerEndpoint?

  fun observeHistoryForServer(serverId: String): Flow<List<ServerTestResult>>

  suspend fun toggleFavorite(serverId: String)

  suspend fun restoreFavorite(favorite: FavoriteInfo)

  suspend fun setFavoritePinned(
    serverId: String,
    pinned: Boolean,
  )

  suspend fun clearAllFavorites()

  suspend fun saveCustomServer(
    existingId: String? = null,
    name: String,
    rawUrl: String,
    category: ServerCategory,
    speedTestUrl: String? = null,
  ): Result<ServerEndpoint>

  suspend fun deleteCustomServer(serverId: String)

  suspend fun deleteAllCustomServers()

  suspend fun recordTestResults(results: List<ServerTestResult>)

  suspend fun deleteHistoryEntry(historyId: Long)

  suspend fun restoreHistoryEntry(result: ServerTestResult)

  suspend fun clearAllHistory()

  suspend fun pruneExpiredHistory(retentionDays: Int)

  suspend fun resetAllLocalUserData()
}
