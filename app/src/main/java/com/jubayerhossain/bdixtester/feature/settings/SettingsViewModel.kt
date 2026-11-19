package com.jubayerhossain.bdixtester.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jubayerhossain.bdixtester.core.di.AppContainer
import com.jubayerhossain.bdixtester.core.model.AppLanguage
import com.jubayerhossain.bdixtester.core.model.AppThemePreset
import com.jubayerhossain.bdixtester.core.model.GovernanceOverviewCounts
import com.jubayerhossain.bdixtester.core.model.ScanMode
import com.jubayerhossain.bdixtester.core.model.SyncStatus
import com.jubayerhossain.bdixtester.core.model.ThemePreference
import com.jubayerhossain.bdixtester.core.network.NetworkSnapshot
import com.jubayerhossain.bdixtester.data.catalog.BdOttPlatforms
import com.jubayerhossain.bdixtester.data.catalog.BuiltInCatalog
import com.jubayerhossain.bdixtester.data.preferences.AppSettings
import com.jubayerhossain.bdixtester.domain.model.CatalogIntegrityReport
import com.jubayerhossain.bdixtester.domain.model.CatalogSyncState
import com.jubayerhossain.bdixtester.feature.governance.computeGovernanceOverviewCounts
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SettingsSyncFeedback {
  data class BuiltInRefreshed(
    val bdixCount: Int,
    val ottCount: Int,
    val isConsistent: Boolean,
  ) : SettingsSyncFeedback

  data class RemoteSyncCompleted(
    val status: SyncStatus,
    val message: String,
    val entryCount: Int,
  ) : SettingsSyncFeedback
}

data class SettingsUiState(
  val settings: AppSettings = AppSettings(),
  val catalogSyncState: CatalogSyncState = CatalogSyncState(),
  val catalogIntegrity: CatalogIntegrityReport? = null,
  val networkSnapshot: NetworkSnapshot = NetworkSnapshot(),
  val totalServerCount: Int = BuiltInCatalog.DEFAULT_CURATED_COUNT,
  val governanceCounts: GovernanceOverviewCounts = GovernanceOverviewCounts(),
  val isSyncing: Boolean = false,
  val lastBuiltInVerifiedEpochMs: Long? = null,
)

class SettingsViewModel(
  private val appContainer: AppContainer,
) : ViewModel() {
  private val settingsRepo = appContainer.settingsRepository
  private val catalogRepo = appContainer.catalogRepository
  private val _isSyncing = MutableStateFlow(false)
  private val _lastBuiltInVerifiedEpochMs = MutableStateFlow<Long?>(null)
  private val _syncFeedback = MutableSharedFlow<SettingsSyncFeedback>(extraBufferCapacity = 8)
  val syncFeedback: SharedFlow<SettingsSyncFeedback> = _syncFeedback.asSharedFlow()

  private val catalogSummaryFlow =
    combine(
      catalogRepo.catalogSyncState,
      catalogRepo.catalogIntegrity,
      catalogRepo.endpoints,
    ) { syncState, integrity, endpoints ->
      Triple(syncState, integrity, endpoints.size.takeIf { it > 0 } ?: BuiltInCatalog.DEFAULT_CURATED_COUNT)
    }

  private val governanceCountsFlow =
    combine(
      catalogRepo.allCatalogEndpoints,
      catalogRepo.governanceRecords,
    ) { allEndpoints, records ->
      computeGovernanceOverviewCounts(allEndpoints, records)
    }

  private val syncRuntimeFlow =
    combine(
      _isSyncing,
      _lastBuiltInVerifiedEpochMs,
    ) { isSyncing, verifiedAt ->
      isSyncing to verifiedAt
    }

  val uiState: StateFlow<SettingsUiState> =
    combine(
      settingsRepo.settings,
      catalogSummaryFlow,
      governanceCountsFlow,
      appContainer.connectivityObserver.observeConnectivity(),
      syncRuntimeFlow,
    ) { settings, (syncState, integrity, totalServers), governanceCounts, network, (isSyncing, builtInVerifiedAt) ->
      SettingsUiState(
        settings = settings,
        catalogSyncState = syncState,
        catalogIntegrity = integrity,
        networkSnapshot = network,
        totalServerCount = totalServers,
        governanceCounts = governanceCounts,
        isSyncing = isSyncing,
        lastBuiltInVerifiedEpochMs = builtInVerifiedAt,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue =
        SettingsUiState(
          settings = appContainer.appSettings.value,
          networkSnapshot = appContainer.connectivityObserver.currentSnapshot(),
        ),
    )

  fun selectThemePreset(preset: AppThemePreset) {
    viewModelScope.launch { settingsRepo.setThemePreset(preset) }
  }

  fun selectTheme(preference: ThemePreference) {
    viewModelScope.launch { settingsRepo.setThemePreference(preference) }
  }

  fun selectLanguage(language: AppLanguage) {
    viewModelScope.launch { settingsRepo.setAppLanguage(language) }
  }

  fun setReduceAnimations(enabled: Boolean) {
    viewModelScope.launch { settingsRepo.setReduceAnimations(enabled) }
  }

  fun selectDefaultScanMode(mode: ScanMode) {
    viewModelScope.launch { settingsRepo.setDefaultScanMode(mode) }
  }

  fun setMaxConcurrency(concurrency: Int) {
    viewModelScope.launch { settingsRepo.setMaxConcurrency(concurrency) }
  }

  fun setFullScanTimeouts(
    connectMs: Int,
    readMs: Int,
  ) {
    viewModelScope.launch { settingsRepo.setFullScanTimeouts(connectMs, readMs) }
  }

  fun setQuickScanDeadline(deadlineMs: Int) {
    viewModelScope.launch { settingsRepo.setQuickScanDeadline(deadlineMs) }
  }

  fun setSpeedTestMaxBytes(maxBytes: Long) {
    viewModelScope.launch { settingsRepo.setSpeedTestMaxBytes(maxBytes) }
  }

  fun setManualNetworkLabel(label: String) {
    viewModelScope.launch { settingsRepo.setManualNetworkLabel(label) }
  }

  fun setRemoteCatalogUrl(url: String) {
    viewModelScope.launch { settingsRepo.setRemoteCatalogUrl(url.trim()) }
  }

  fun setAutoCatalogUpdates(enabled: Boolean) {
    viewModelScope.launch { settingsRepo.setAutoCatalogUpdates(enabled) }
  }

  fun setWifiOnlyCatalogUpdates(wifiOnly: Boolean) {
    viewModelScope.launch { settingsRepo.setWifiOnlyCatalogUpdates(wifiOnly) }
  }

  fun setAllowCrossOriginPayload(allow: Boolean) {
    viewModelScope.launch { settingsRepo.setAllowCrossOriginRemotePayload(allow) }
  }

  fun setHistoryRetentionDays(days: Int) {
    viewModelScope.launch {
      settingsRepo.setHistoryRetentionDays(days)
      catalogRepo.pruneExpiredHistory(days)
    }
  }

  fun refreshBuiltInCatalogNow() {
    if (_isSyncing.value) return
    viewModelScope.launch {
      _isSyncing.value = true
      try {
        val report = catalogRepo.refreshBuiltInCatalog()
        _lastBuiltInVerifiedEpochMs.value = System.currentTimeMillis()
        _syncFeedback.tryEmit(
          SettingsSyncFeedback.BuiltInRefreshed(
            bdixCount = report.builtInDatabaseCount,
            ottCount = BdOttPlatforms.all.size,
            isConsistent = report.isConsistent,
          ),
        )
      } finally {
        _isSyncing.value = false
      }
    }
  }

  fun saveRemoteUrlAndSync(rawUrl: String) {
    val trimmed = rawUrl.trim()
    if (trimmed.isEmpty()) {
      setRemoteCatalogUrl("")
      return
    }
    syncCatalogNow(trimmed)
  }

  fun syncCatalogNow(overrideUrl: String? = null) {
    if (_isSyncing.value) return
    viewModelScope.launch {
      _isSyncing.value = true
      try {
        val trimmedOverride = overrideUrl?.trim()
        val current = settingsRepo.settings.first()
        if (trimmedOverride != null && trimmedOverride != current.remoteCatalogUrl) {
          settingsRepo.setRemoteCatalogUrl(trimmedOverride)
        }
        val effective = settingsRepo.settings.first()
        val effectiveUrl = trimmedOverride ?: effective.remoteCatalogUrl.trim()
        if (effectiveUrl.isEmpty()) {
          val report = catalogRepo.refreshBuiltInCatalog()
          _lastBuiltInVerifiedEpochMs.value = System.currentTimeMillis()
          _syncFeedback.tryEmit(
            SettingsSyncFeedback.BuiltInRefreshed(
              bdixCount = report.builtInDatabaseCount,
              ottCount = BdOttPlatforms.all.size,
              isConsistent = report.isConsistent,
            ),
          )
          return@launch
        }
        val result = appContainer.remoteCatalogSyncManager.syncCatalog(effective, effectiveUrl)
        _syncFeedback.tryEmit(
          SettingsSyncFeedback.RemoteSyncCompleted(
            status = result.status,
            message = result.message,
            entryCount = result.entryCount,
          ),
        )
      } finally {
        _isSyncing.value = false
      }
    }
  }

  fun clearAllHistory() {
    viewModelScope.launch { catalogRepo.clearAllHistory() }
  }

  fun clearAllFavorites() {
    viewModelScope.launch { catalogRepo.clearAllFavorites() }
  }

  fun deleteAllCustomServers() {
    viewModelScope.launch { catalogRepo.deleteAllCustomServers() }
  }

  fun resetAllLocalData() {
    viewModelScope.launch {
      catalogRepo.resetAllLocalUserData()
      settingsRepo.resetSettings()
    }
  }

  companion object {
    fun factory(appContainer: AppContainer): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(appContainer) as T
      }
  }
}
