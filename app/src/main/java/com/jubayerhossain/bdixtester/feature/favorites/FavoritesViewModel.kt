package com.jubayerhossain.bdixtester.feature.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jubayerhossain.bdixtester.core.export.CatalogExportSerializer
import com.jubayerhossain.bdixtester.core.export.ExportDataset
import com.jubayerhossain.bdixtester.core.export.ExportFormat
import com.jubayerhossain.bdixtester.core.model.ServerProtocol
import com.jubayerhossain.bdixtester.core.model.TestStatus
import com.jubayerhossain.bdixtester.core.util.ActiveResultMatcher
import com.jubayerhossain.bdixtester.domain.model.FavoriteInfo
import com.jubayerhossain.bdixtester.domain.model.ServerEndpoint
import com.jubayerhossain.bdixtester.domain.model.ServerTestResult
import com.jubayerhossain.bdixtester.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FavoriteServerItem(
  val endpoint: ServerEndpoint,
  val favorite: FavoriteInfo,
  val latestResult: ServerTestResult?,
)

data class FavoritesUiState(
  val favoriteItems: List<FavoriteServerItem> = emptyList(),
  val historyItems: List<ServerTestResult> = emptyList(),
  val hasAnyFavorites: Boolean = false,
  val hasAnyHistory: Boolean = false,
  val searchQuery: String = "",
  val historyStatusFilter: TestStatus? = null,
  val historyProtocolFilter: ServerProtocol? = null,
)

class FavoritesViewModel(
  private val catalogRepository: CatalogRepository,
) : ViewModel() {
  private val _searchQuery = MutableStateFlow("")
  private val _historyStatusFilter = MutableStateFlow<TestStatus?>(null)
  private val _historyProtocolFilter = MutableStateFlow<ServerProtocol?>(null)

  val uiState: StateFlow<FavoritesUiState> =
    combine(
      catalogRepository.endpoints,
      catalogRepository.favorites,
      catalogRepository.latestResults,
      catalogRepository.history,
      _searchQuery,
      _historyStatusFilter,
      _historyProtocolFilter,
    ) { values ->
      @Suppress("UNCHECKED_CAST")
      val endpoints = values[0] as List<ServerEndpoint>

      @Suppress("UNCHECKED_CAST")
      val favorites = values[1] as Map<String, FavoriteInfo>

      @Suppress("UNCHECKED_CAST")
      val latest = values[2] as Map<String, ServerTestResult>

      @Suppress("UNCHECKED_CAST")
      val history = values[3] as List<ServerTestResult>
      val query = (values[4] as String).trim().lowercase()
      val statusFilter = values[5] as TestStatus?
      val protocolFilter = values[6] as ServerProtocol?

      val activeLatest = ActiveResultMatcher.matchActive(endpoints, latest)
      val endpointMap = endpoints.associateBy { it.id }

      val favItems =
        favorites.values
          .mapNotNull { fav ->
            val ep = endpointMap[fav.serverId] ?: return@mapNotNull null
            FavoriteServerItem(
              endpoint = ep,
              favorite = fav,
              latestResult = activeLatest[ep.id],
            )
          }.filter { item ->
            query.isEmpty() ||
              item.endpoint.name
                .lowercase()
                .contains(query) ||
              item.endpoint.originalUrl
                .lowercase()
                .contains(query) ||
              item.endpoint.host
                .lowercase()
                .contains(query)
          }.sortedWith(
            compareByDescending<FavoriteServerItem> { it.favorite.isPinned }
              .thenBy { it.favorite.displayOrder }
              .thenByDescending { it.favorite.addedAtEpochMs },
          )

      val filteredHistory =
        history
          .filter { statusFilter == null || it.status == statusFilter }
          .filter { protocolFilter == null || it.protocol == protocolFilter }
          .filter { item ->
            query.isEmpty() ||
              item.serverNameSnapshot.lowercase().contains(query) ||
              item.urlSnapshot.lowercase().contains(query) ||
              (item.networkProfileLabel?.lowercase()?.contains(query) == true)
          }

      FavoritesUiState(
        favoriteItems = favItems,
        historyItems = filteredHistory,
        hasAnyFavorites = favorites.isNotEmpty(),
        hasAnyHistory = history.isNotEmpty(),
        searchQuery = values[4] as String,
        historyStatusFilter = statusFilter,
        historyProtocolFilter = protocolFilter,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = FavoritesUiState(),
    )

  fun updateSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun selectHistoryStatusFilter(status: TestStatus?) {
    _historyStatusFilter.value = status
  }

  fun selectHistoryProtocolFilter(protocol: ServerProtocol?) {
    _historyProtocolFilter.value = protocol
  }

  fun toggleFavorite(serverId: String) {
    viewModelScope.launch {
      catalogRepository.toggleFavorite(serverId)
    }
  }

  fun restoreFavorite(favorite: FavoriteInfo) {
    viewModelScope.launch {
      catalogRepository.restoreFavorite(favorite)
    }
  }

  fun setPinned(
    serverId: String,
    pinned: Boolean,
  ) {
    viewModelScope.launch {
      catalogRepository.setFavoritePinned(serverId, pinned)
    }
  }

  fun deleteHistoryEntry(historyId: Long) {
    viewModelScope.launch {
      catalogRepository.deleteHistoryEntry(historyId)
    }
  }

  fun restoreHistoryEntry(result: ServerTestResult) {
    viewModelScope.launch {
      catalogRepository.restoreHistoryEntry(result)
    }
  }

  fun clearAllHistory() {
    viewModelScope.launch {
      catalogRepository.clearAllHistory()
    }
  }

  fun exportFavorites(format: ExportFormat): String {
    val state = uiState.value
    val favoriteEndpoints = state.favoriteItems.map { it.endpoint }
    return CatalogExportSerializer.exportEndpoints(
      endpoints = favoriteEndpoints,
      latestResults =
        ActiveResultMatcher.matchActive(
          favoriteEndpoints,
          catalogRepository.latestResults.value,
        ),
      favorites = catalogRepository.favorites.value,
      format = format,
      dataset = ExportDataset.FAVORITES,
    )
  }

  fun exportHistory(format: ExportFormat): String =
    CatalogExportSerializer.exportHistory(
      history = uiState.value.historyItems,
      format = format,
    )

  companion object {
    fun factory(catalogRepository: CatalogRepository): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = FavoritesViewModel(catalogRepository) as T
      }
  }
}
