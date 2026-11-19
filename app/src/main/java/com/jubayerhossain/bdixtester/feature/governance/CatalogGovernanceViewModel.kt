package com.jubayerhossain.bdixtester.feature.governance

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jubayerhossain.bdixtester.core.di.AppContainer
import com.jubayerhossain.bdixtester.core.model.EndpointGovernanceRecord
import com.jubayerhossain.bdixtester.core.model.GovernanceFilter
import com.jubayerhossain.bdixtester.core.model.GovernanceOverviewCounts
import com.jubayerhossain.bdixtester.core.model.VerificationEventRecord
import com.jubayerhossain.bdixtester.core.model.VerificationLifecycleStatus
import com.jubayerhossain.bdixtester.core.model.VerificationRunProgress
import com.jubayerhossain.bdixtester.data.remote.VerificationNotificationHelper
import com.jubayerhossain.bdixtester.data.verification.CatalogGovernanceCoordinator
import com.jubayerhossain.bdixtester.data.verification.IspProbeLoadResult
import com.jubayerhossain.bdixtester.domain.model.ServerEndpoint
import com.jubayerhossain.bdixtester.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GovernanceListItem(
  val endpoint: ServerEndpoint,
  val record: EndpointGovernanceRecord,
)

data class CatalogGovernanceUiState(
  val items: List<GovernanceListItem> = emptyList(),
  val counts: GovernanceOverviewCounts = GovernanceOverviewCounts(),
  val selectedFilter: GovernanceFilter = GovernanceFilter.ALL,
  val searchQuery: String = "",
  val runProgress: VerificationRunProgress = VerificationRunProgress(),
  val selectedItem: GovernanceListItem? = null,
)

class CatalogGovernanceViewModel(
  private val catalogRepository: CatalogRepository,
  private val coordinator: CatalogGovernanceCoordinator,
  private val appContext: Context? = null,
) : ViewModel() {
  private val selectedFilter = MutableStateFlow(GovernanceFilter.ALL)
  private val searchQuery = MutableStateFlow("")
  private val selectedServerId = MutableStateFlow<String?>(null)

  val uiState: StateFlow<CatalogGovernanceUiState> =
    combine(
      combine(
        catalogRepository.allCatalogEndpoints,
        catalogRepository.governanceRecords,
      ) { endpoints, records -> endpoints to records },
      selectedFilter,
      searchQuery,
      catalogRepository.verificationRunProgress,
      selectedServerId,
    ) { (endpoints, records), filter, query, progress, inspectedId ->
      val counts = computeGovernanceOverviewCounts(endpoints, records)
      val trimmedQuery = query.trim().lowercase()
      val merged =
        endpoints.map { endpoint ->
          val record =
            records[endpoint.id]
              ?: EndpointGovernanceRecord(
                serverId = endpoint.id,
                lifecycleStatus = VerificationLifecycleStatus.PENDING_VERIFICATION,
              )
          GovernanceListItem(endpoint = endpoint, record = record)
        }

      val filtered =
        merged
          .filter { item -> item.record.lifecycleStatus.matchesFilter(filter) }
          .filter { item ->
            if (trimmedQuery.isEmpty()) {
              true
            } else {
              item.endpoint.name
                .lowercase()
                .contains(trimmedQuery) ||
                item.endpoint.host
                  .lowercase()
                  .contains(trimmedQuery) ||
                item.endpoint.originalUrl
                  .lowercase()
                  .contains(trimmedQuery) ||
                (
                  item.record.normalizedDomain
                    ?.lowercase()
                    ?.contains(trimmedQuery) == true
                ) ||
                item.record.lifecycleStatus.displayLabel
                  .lowercase()
                  .contains(trimmedQuery)
            }
          }.sortedWith(
            compareBy<GovernanceListItem>(
              { statusSortPriority(it.record.lifecycleStatus) },
              { it.endpoint.name.lowercase() },
            ),
          )

      val selected = inspectedId?.let { targetId -> merged.firstOrNull { it.endpoint.id == targetId } }
      CatalogGovernanceUiState(
        items = filtered,
        counts = counts,
        selectedFilter = filter,
        searchQuery = query,
        runProgress = progress,
        selectedItem = selected,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = CatalogGovernanceUiState(),
    )

  fun observeEventsFor(serverId: String?): Flow<List<VerificationEventRecord>> =
    if (serverId == null) {
      flowOf(emptyList())
    } else {
      catalogRepository.observeVerificationEvents(serverId)
    }

  fun selectFilter(filter: GovernanceFilter) {
    selectedFilter.value = filter
  }

  fun updateSearchQuery(query: String) {
    searchQuery.value = query
  }

  fun openEvidenceSheet(serverId: String) {
    selectedServerId.value = serverId
  }

  fun closeEvidenceSheet() {
    selectedServerId.value = null
  }

  fun runSafeVerification(maxEndpoints: Int = CatalogGovernanceCoordinator.DEFAULT_BATCH_LIMIT) {
    if (catalogRepository.verificationRunProgress.value.isRunning) return
    viewModelScope.launch {
      val result =
        coordinator.verifyBatch(
          maxEndpoints = maxEndpoints,
          triggerSource = "manual_batch_ui",
        )
      val ctx = appContext ?: return@launch
      result
        .onSuccess { decisions ->
          val candidates = decisions.count { it.status.isRemovalCandidateOrConfirmed }
          VerificationNotificationHelper.postSummaryNotification(
            context = ctx,
            checkedCount = decisions.size,
            candidateCount = candidates,
          )
        }.onFailure { err ->
          VerificationNotificationHelper.postFailureNotification(
            context = ctx,
            errorDetail = err.message ?: "Unknown error",
          )
        }
    }
  }

  fun verifySingleEndpoint(serverId: String) {
    viewModelScope.launch {
      coordinator.verifySingleEndpoint(serverId = serverId, triggerSource = "manual_single_ui")
    }
  }

  fun importIspProbesJson(rawJson: String): Result<IspProbeLoadResult> = coordinator.importIspProbeJson(rawJson)

  fun markRemovalCandidate(
    serverId: String,
    reason: String = "Marked for governance review by user",
  ) {
    viewModelScope.launch {
      catalogRepository.markRemovalCandidate(serverId, reason)
    }
  }

  fun archiveEndpoint(
    serverId: String,
    reason: String = "Reversibly archived by user after evidence review",
  ) {
    viewModelScope.launch {
      catalogRepository.archiveEndpoint(serverId, reason)
    }
  }

  fun restoreEndpoint(
    serverId: String,
    reason: String = "Restored to active catalog by user",
  ) {
    viewModelScope.launch {
      catalogRepository.restoreEndpoint(serverId, reason)
    }
  }

  companion object {
    fun factory(
      appContainer: AppContainer,
      context: Context? = null,
    ): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
          CatalogGovernanceViewModel(
            catalogRepository = appContainer.catalogRepository,
            coordinator = appContainer.catalogGovernanceCoordinator,
            appContext = context?.applicationContext,
          ) as T
      }
  }
}

fun VerificationLifecycleStatus.matchesFilter(filter: GovernanceFilter): Boolean =
  when (filter) {
    GovernanceFilter.ALL -> true
    GovernanceFilter.ACTIVE ->
      this == VerificationLifecycleStatus.ACTIVE || this == VerificationLifecycleStatus.REDIRECTED
    GovernanceFilter.ISP_RESTRICTED ->
      this == VerificationLifecycleStatus.ISP_RESTRICTED
    GovernanceFilter.UNREACHABLE ->
      this == VerificationLifecycleStatus.UNREACHABLE_FROM_CURRENT_ISP
    GovernanceFilter.TEMPORARILY_UNAVAILABLE ->
      this == VerificationLifecycleStatus.TEMPORARILY_UNAVAILABLE ||
        this == VerificationLifecycleStatus.DNS_TEMPORARY_FAILURE
    GovernanceFilter.EXPIRED_SUSPECTED ->
      this == VerificationLifecycleStatus.DNS_NXDOMAIN_SUSPECTED ||
        this == VerificationLifecycleStatus.DOMAIN_EXPIRED_SUSPECTED
    GovernanceFilter.REMOVAL_CANDIDATES ->
      this == VerificationLifecycleStatus.REMOVAL_CANDIDATE ||
        this == VerificationLifecycleStatus.DOMAIN_EXPIRED_CONFIRMED ||
        this == VerificationLifecycleStatus.PERMANENTLY_REMOVED
    GovernanceFilter.ARCHIVED ->
      this == VerificationLifecycleStatus.ARCHIVED
    GovernanceFilter.UNVERIFIED ->
      this == VerificationLifecycleStatus.UNKNOWN ||
        this == VerificationLifecycleStatus.PENDING_VERIFICATION ||
        this == VerificationLifecycleStatus.UNSUPPORTED_PROTOCOL
  }

fun computeGovernanceOverviewCounts(
  endpoints: List<ServerEndpoint>,
  records: Map<String, EndpointGovernanceRecord>,
): GovernanceOverviewCounts {
  var active = 0
  var ispRestricted = 0
  var unreachable = 0
  var tempUnavailable = 0
  var expiredSuspected = 0
  var removalCandidate = 0
  var archived = 0
  var unverified = 0

  for (ep in endpoints) {
    val status = records[ep.id]?.lifecycleStatus ?: VerificationLifecycleStatus.PENDING_VERIFICATION
    when {
      status.matchesFilter(GovernanceFilter.ACTIVE) -> active++
      status.matchesFilter(GovernanceFilter.ISP_RESTRICTED) -> ispRestricted++
      status.matchesFilter(GovernanceFilter.UNREACHABLE) -> unreachable++
      status.matchesFilter(GovernanceFilter.TEMPORARILY_UNAVAILABLE) -> tempUnavailable++
      status.matchesFilter(GovernanceFilter.EXPIRED_SUSPECTED) -> expiredSuspected++
      status.matchesFilter(GovernanceFilter.REMOVAL_CANDIDATES) -> removalCandidate++
      status.matchesFilter(GovernanceFilter.ARCHIVED) -> archived++
      else -> unverified++
    }
  }

  return GovernanceOverviewCounts(
    totalCatalogCount = endpoints.size,
    activeCount = active,
    ispRestrictedCount = ispRestricted,
    unreachableCount = unreachable,
    temporarilyUnavailableCount = tempUnavailable,
    expiredSuspectedCount = expiredSuspected,
    removalCandidateCount = removalCandidate,
    archivedCount = archived,
    unverifiedCount = unverified,
  )
}

private fun statusSortPriority(status: VerificationLifecycleStatus): Int =
  when (status) {
    VerificationLifecycleStatus.REMOVAL_CANDIDATE -> 0
    VerificationLifecycleStatus.DOMAIN_EXPIRED_CONFIRMED -> 1
    VerificationLifecycleStatus.PERMANENTLY_REMOVED -> 2
    VerificationLifecycleStatus.DOMAIN_EXPIRED_SUSPECTED -> 3
    VerificationLifecycleStatus.DNS_NXDOMAIN_SUSPECTED -> 4
    VerificationLifecycleStatus.ARCHIVED -> 5
    VerificationLifecycleStatus.TEMPORARILY_UNAVAILABLE -> 6
    VerificationLifecycleStatus.DNS_TEMPORARY_FAILURE -> 7
    VerificationLifecycleStatus.UNREACHABLE_FROM_CURRENT_ISP -> 8
    VerificationLifecycleStatus.ISP_RESTRICTED -> 9
    VerificationLifecycleStatus.ACTIVE -> 10
    VerificationLifecycleStatus.REDIRECTED -> 11
    VerificationLifecycleStatus.UNKNOWN -> 12
    VerificationLifecycleStatus.UNSUPPORTED_PROTOCOL -> 13
    VerificationLifecycleStatus.PENDING_VERIFICATION -> 14
  }
