package com.jubayerhossain.bdixtester.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jubayerhossain.bdixtester.core.di.AppContainer
import com.jubayerhossain.bdixtester.core.model.AppLanguage
import com.jubayerhossain.bdixtester.core.model.AppThemePreset
import com.jubayerhossain.bdixtester.core.model.ScanMode
import com.jubayerhossain.bdixtester.core.model.ThemePreference
import com.jubayerhossain.bdixtester.core.network.NetworkSnapshot
import com.jubayerhossain.bdixtester.data.catalog.BuiltInCatalog
import com.jubayerhossain.bdixtester.data.preferences.AppSettings
import com.jubayerhossain.bdixtester.domain.model.CatalogIntegrityReport
import com.jubayerhossain.bdixtester.domain.model.CatalogSyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
  val settings: AppSettings = AppSettings(),
  val catalogSyncState: CatalogSyncState = CatalogSyncState(),
  val catalogIntegrity: CatalogIntegrityReport? = null,
  val networkSnapshot: NetworkSnapshot = NetworkSnapshot(),
  val totalServerCount: Int = BuiltInCatalog.DEFAULT_CURATED_COUNT,
  val isSyncing: Boolean = false,
)

class SettingsViewModel(
  private val appContainer: AppContainer,
) : ViewModel() {
  private val settingsRepo = appContainer.settingsRepository
  private val catalogRepo = appContainer.catalogRepository
  private val _isSyncing = MutableStateFlow(false)

  private val catalogSummaryFlow =
    combine(
      catalogRepo.catalogSyncState,
      catalogRepo.catalogIntegrity,
      catalogRepo.endpoints,
    ) { syncState, integrity, endpoints ->
      Triple(syncState, integrity, endpoints.size.takeIf { it > 0 } ?: BuiltInCatalog.DEFAULT_CURATED_COUNT)
    }

  val uiState: StateFlow<SettingsUiState> =
    combine(
      settingsRepo.settings,
      catalogSummaryFlow,
      appContainer.connectivityObserver.observeConnectivity(),
      _isSyncing,
    ) { settings, (syncState, integrity, totalServers), network, isSyncing ->
      SettingsUiState(
        settings = settings,
        catalogSyncState = syncState,
        catalogIntegrity = integrity,
        networkSnapshot = network,
        totalServerCount = totalServers,
        isSyncing = isSyncing,
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
    viewModelScope.launch { settingsRepo.setRemoteCatalogUrl(url) }
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

  fun syncCatalogNow(overrideUrl: String? = null) {
    if (_isSyncing.value) return
    viewModelScope.launch {
      _isSyncing.value = true
      try {
        val current = settingsRepo.settings.first()
        if (overrideUrl != null && overrideUrl.trim() != current.remoteCatalogUrl) {
          settingsRepo.setRemoteCatalogUrl(overrideUrl)
        }
        val effective = settingsRepo.settings.first()
        appContainer.remoteCatalogSyncManager.syncCatalog(effective, overrideUrl)
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
