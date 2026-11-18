package com.jubayerhossain.bdixtester.feature.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jubayerhossain.bdixtester.core.export.CatalogExportSerializer
import com.jubayerhossain.bdixtester.core.export.ExportFormat
import com.jubayerhossain.bdixtester.core.model.CatalogSourceType
import com.jubayerhossain.bdixtester.core.model.ScanMode
import com.jubayerhossain.bdixtester.core.model.ScanScope
import com.jubayerhossain.bdixtester.core.model.ServerCategory
import com.jubayerhossain.bdixtester.core.model.TestStatus
import com.jubayerhossain.bdixtester.core.util.ActiveResultMatcher
import com.jubayerhossain.bdixtester.data.network.ScanCoordinator
import com.jubayerhossain.bdixtester.data.preferences.AppSettings
import com.jubayerhossain.bdixtester.data.preferences.SettingsRepository
import com.jubayerhossain.bdixtester.domain.model.FavoriteInfo
import com.jubayerhossain.bdixtester.domain.model.ScanProgressState
import com.jubayerhossain.bdixtester.domain.model.ServerEndpoint
import com.jubayerhossain.bdixtester.domain.model.ServerTestResult
import com.jubayerhossain.bdixtester.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ScanUiState(
  val selectedMode: ScanMode = ScanMode.QUICK_SCAN,
  val selectedScope: ScanScope = ScanScope.ALL_SERVERS,
  val selectedCategory: ServerCategory = ServerCategory.UNCATEGORIZED,
  val targetEntryCount: Int = 0,
  val targetUniqueUrlCount: Int = 0,
  val progress: ScanProgressState = ScanProgressState(),
  val settings: AppSettings = AppSettings(),
)

class ScanViewModel(
  private val catalogRepository: CatalogRepository,
  private val scanCoordinator: ScanCoordinator,
  private val settingsRepository: SettingsRepository,
) : ViewModel() {
  private val _selectedMode = MutableStateFlow(ScanMode.QUICK_SCAN)
  private val _selectedScope = MutableStateFlow(ScanScope.ALL_SERVERS)
  private val _selectedCategory = MutableStateFlow(ServerCategory.UNCATEGORIZED)

  val uiState: StateFlow<ScanUiState> =
    combine(
      _selectedMode,
      _selectedScope,
      _selectedCategory,
      catalogRepository.endpoints,
      catalogRepository.favorites,
      catalogRepository.latestResults,
      catalogRepository.scanProgress,
      settingsRepository.settings,
    ) { values ->
      val mode = values[0] as ScanMode
      val scope = values[1] as ScanScope
      val category = values[2] as ServerCategory

      @Suppress("UNCHECKED_CAST")
      val endpoints = values[3] as List<ServerEndpoint>

      @Suppress("UNCHECKED_CAST")
      val favorites = values[4] as Map<String, FavoriteInfo>

      @Suppress("UNCHECKED_CAST")
      val latestResults = values[5] as Map<String, ServerTestResult>
      val progress = values[6] as ScanProgressState
      val settings = values[7] as AppSettings

      val targets =
        filterTargets(
          endpoints = endpoints,
          scope = scope,
          selectedCategory = category,
          favorites = favorites,
          latestResults = latestResults,
        )

      ScanUiState(
        selectedMode = mode,
        selectedScope = scope,
        selectedCategory = category,
        targetEntryCount = targets.size,
        targetUniqueUrlCount = targets.map { it.originalUrl }.distinct().size,
        progress = progress,
        settings = settings,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = ScanUiState(),
    )

  fun selectScanMode(mode: ScanMode) {
    _selectedMode.value = mode
  }

  fun selectScanScope(scope: ScanScope) {
    _selectedScope.value = scope
  }

  fun selectScopeCategory(category: ServerCategory) {
    _selectedCategory.value = category
  }

  fun startScan(overrideMode: ScanMode? = null) {
    val mode = overrideMode ?: _selectedMode.value
    if (overrideMode != null) {
      _selectedMode.value = overrideMode
    }
    viewModelScope.launch {
      val targets =
        filterTargets(
          endpoints = catalogRepository.endpoints.value,
          scope = _selectedScope.value,
          selectedCategory = _selectedCategory.value,
          favorites = catalogRepository.favorites.value,
          latestResults = catalogRepository.latestResults.value,
        )
      if (targets.isEmpty()) return@launch
      val settings = settingsRepository.settings.first()
      scanCoordinator.startScan(
        endpoints = targets,
        scanMode = mode,
        scanScope = _selectedScope.value,
        settings = settings,
      )
    }
  }

  fun stopScan() {
    viewModelScope.launch {
      scanCoordinator.stopScan()
    }
  }

  fun buildScanSummaryExport(format: ExportFormat): String {
    val progress = catalogRepository.scanProgress.value
    val runId = progress.scanRunId
    val runResults =
      if (runId != null) {
        catalogRepository.history.value.filter { it.scanRunId == runId }
      } else {
        catalogRepository.latestResults.value.values
          .toList()
      }
    return CatalogExportSerializer.exportScanSummary(
      progress = progress,
      resultsForRun = runResults,
      format = format,
    )
  }

  private fun filterTargets(
    endpoints: List<ServerEndpoint>,
    scope: ScanScope,
    selectedCategory: ServerCategory,
    favorites: Map<String, FavoriteInfo>,
    latestResults: Map<String, ServerTestResult>,
  ): List<ServerEndpoint> =
    when (scope) {
      ScanScope.ALL_SERVERS -> endpoints
      ScanScope.SELECTED_CATEGORY -> {
        if (selectedCategory == ServerCategory.ALL) {
          endpoints
        } else {
          endpoints.filter { it.category == selectedCategory }
        }
      }
      ScanScope.FAVORITES -> endpoints.filter { favorites.containsKey(it.id) }
      ScanScope.CUSTOM_SERVERS -> endpoints.filter { it.sourceType == CatalogSourceType.USER_CUSTOM }
      ScanScope.RETRY_FAILED -> {
        val activeResults = ActiveResultMatcher.matchActive(endpoints, latestResults)
        endpoints.filter { ep ->
          val status = activeResults[ep.id]?.status
          status == TestStatus.UNREACHABLE || status == TestStatus.TIMED_OUT || status == TestStatus.CANCELLED
        }
      }
    }

  companion object {
    fun factory(
      catalogRepository: CatalogRepository,
      scanCoordinator: ScanCoordinator,
      settingsRepository: SettingsRepository,
    ): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
          ScanViewModel(
            catalogRepository = catalogRepository,
            scanCoordinator = scanCoordinator,
            settingsRepository = settingsRepository,
          ) as T
      }
  }
}
