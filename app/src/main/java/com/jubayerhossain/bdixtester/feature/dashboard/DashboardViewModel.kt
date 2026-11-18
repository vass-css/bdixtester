package com.jubayerhossain.bdixtester.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jubayerhossain.bdixtester.core.model.CatalogAuditSummary
import com.jubayerhossain.bdixtester.core.model.CatalogSourceType
import com.jubayerhossain.bdixtester.core.model.ScanMode
import com.jubayerhossain.bdixtester.core.model.ServerCategory
import com.jubayerhossain.bdixtester.core.model.TestStatus
import com.jubayerhossain.bdixtester.core.network.ConnectivityObserver
import com.jubayerhossain.bdixtester.core.network.NetworkSnapshot
import com.jubayerhossain.bdixtester.core.util.ActiveResultMatcher
import com.jubayerhossain.bdixtester.data.preferences.AppSettings
import com.jubayerhossain.bdixtester.data.preferences.SettingsRepository
import com.jubayerhossain.bdixtester.domain.model.CatalogIntegrityReport
import com.jubayerhossain.bdixtester.domain.model.CatalogSyncState
import com.jubayerhossain.bdixtester.domain.model.FavoriteInfo
import com.jubayerhossain.bdixtester.domain.model.ScanProgressState
import com.jubayerhossain.bdixtester.domain.model.ServerEndpoint
import com.jubayerhossain.bdixtester.domain.model.ServerTestResult
import com.jubayerhossain.bdixtester.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
  val networkSnapshot: NetworkSnapshot = NetworkSnapshot(),
  val manualNetworkLabel: String = "",
  val selectedScanMode: ScanMode = ScanMode.QUICK_SCAN,
  val totalServers: Int = 0,
  val builtInCount: Int = 0,
  val remoteCount: Int = 0,
  val customCount: Int = 0,
  val categoryCounts: Map<ServerCategory, Int> = emptyMap(),
  val testedCount: Int = 0,
  val reachableCount: Int = 0,
  val failedCount: Int = 0,
  val averageReachableLatencyMs: Long? = null,
  val unreachableCount: Int = 0,
  val restrictedOrErrorCount: Int = 0,
  val unsupportedCount: Int = 0,
  val featuredServers: List<ServerEndpoint> = emptyList(),
  val favorites: Map<String, FavoriteInfo> = emptyMap(),
  val recentlyTested: List<Pair<ServerEndpoint, ServerTestResult>> = emptyList(),
  val lastCatalogUpdateEpochMs: Long? = null,
  val lastScanEpochMs: Long? = null,
  val isScanning: Boolean = false,
  val scanProgress: ScanProgressState = ScanProgressState(),
  val auditSummary: CatalogAuditSummary = CatalogAuditSummary(),
  val catalogSyncState: CatalogSyncState = CatalogSyncState(),
  val catalogIntegrity: CatalogIntegrityReport? = null,
)

class DashboardViewModel(
  private val catalogRepository: CatalogRepository,
  connectivityObserver: ConnectivityObserver,
  settingsRepository: SettingsRepository,
) : ViewModel() {
  val uiState: StateFlow<DashboardUiState> =
    combine(
      catalogRepository.endpoints,
      catalogRepository.latestResults,
      catalogRepository.favorites,
      catalogRepository.scanProgress,
      catalogRepository.auditSummary,
      catalogRepository.catalogSyncState,
      catalogRepository.catalogIntegrity,
      settingsRepository.settings,
      connectivityObserver.observeConnectivity(),
    ) { values ->
      @Suppress("UNCHECKED_CAST")
      val endpoints = values[0] as List<ServerEndpoint>

      @Suppress("UNCHECKED_CAST")
      val results = values[1] as Map<String, ServerTestResult>

      @Suppress("UNCHECKED_CAST")
      val favorites = values[2] as Map<String, FavoriteInfo>
      val scanProgress = values[3] as ScanProgressState
      val audit = values[4] as CatalogAuditSummary
      val syncState = values[5] as CatalogSyncState
      val integrity = values[6] as CatalogIntegrityReport?
      val settings = values[7] as AppSettings
      val network = values[8] as NetworkSnapshot
      val scanMode = settings.defaultScanMode

      val activeResults = ActiveResultMatcher.matchActive(endpoints, results)

      val categoryCounts =
        ServerCategory.entries
          .filter { it != ServerCategory.ALL }
          .associateWith { cat -> endpoints.count { it.category == cat } }

      val recent =
        activeResults.values
          .sortedByDescending { it.testedAtEpochMs }
          .mapNotNull { res ->
            endpoints.firstOrNull { it.id == res.serverId }?.let { it to res }
          }.take(6)

      val reachableResults = activeResults.values.filter { it.status == TestStatus.REACHABLE }
      val unreachableCount =
        activeResults.values.count {
          it.status == TestStatus.UNREACHABLE || it.status == TestStatus.TIMED_OUT
        }
      val restrictedOrErrorCount = activeResults.values.count { it.status == TestStatus.RESTRICTED }
      val unsupportedCount = activeResults.values.count { it.status == TestStatus.UNSUPPORTED }
      val avgLatencyMs =
        reachableResults
          .mapNotNull { it.primaryLatencyMs }
          .takeIf { it.isNotEmpty() }
          ?.average()
          ?.toLong()

      DashboardUiState(
        networkSnapshot = network,
        manualNetworkLabel = settings.manualNetworkLabel,
        selectedScanMode = scanMode,
        totalServers = endpoints.size,
        builtInCount = endpoints.count { it.sourceType == CatalogSourceType.BUILT_IN },
        remoteCount = endpoints.count { it.sourceType == CatalogSourceType.REMOTE_CATALOG },
        customCount = endpoints.count { it.sourceType == CatalogSourceType.USER_CUSTOM },
        categoryCounts = categoryCounts,
        testedCount = activeResults.size,
        reachableCount = reachableResults.size,
        failedCount = unreachableCount + restrictedOrErrorCount + unsupportedCount,
        averageReachableLatencyMs = avgLatencyMs,
        unreachableCount = unreachableCount,
        restrictedOrErrorCount = restrictedOrErrorCount,
        unsupportedCount = unsupportedCount,
        featuredServers = endpoints.filter { it.isFeatured }.take(5),
        favorites = favorites,
        recentlyTested = recent,
        lastCatalogUpdateEpochMs = syncState.lastSuccessfulSyncAtEpochMs,
        lastScanEpochMs = scanProgress.lastScanCompletedAtEpochMs,
        isScanning = scanProgress.isScanning,
        scanProgress = scanProgress,
        auditSummary = audit,
        catalogSyncState = syncState,
        catalogIntegrity = integrity,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = DashboardUiState(networkSnapshot = connectivityObserver.currentSnapshot()),
    )

  fun toggleFavorite(serverId: String) {
    viewModelScope.launch {
      catalogRepository.toggleFavorite(serverId)
    }
  }

  companion object {
    fun factory(
      catalogRepository: CatalogRepository,
      connectivityObserver: ConnectivityObserver,
      settingsRepository: SettingsRepository,
    ): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
          DashboardViewModel(
            catalogRepository = catalogRepository,
            connectivityObserver = connectivityObserver,
            settingsRepository = settingsRepository,
          ) as T
      }
  }
}
