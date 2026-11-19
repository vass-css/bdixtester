package com.jubayerhossain.bdixtester.feature.governance

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jubayerhossain.bdixtester.R
import com.jubayerhossain.bdixtester.core.model.GovernanceFilter
import com.jubayerhossain.bdixtester.core.model.GovernanceOverviewCounts
import com.jubayerhossain.bdixtester.core.model.VerificationLifecycleStatus
import com.jubayerhossain.bdixtester.core.model.VerificationRunProgress
import com.jubayerhossain.bdixtester.data.verification.IspProbeLoadResult
import com.jubayerhossain.bdixtester.ui.components.BdixSearchBar
import com.jubayerhossain.bdixtester.ui.components.EmptyStateView
import com.jubayerhossain.bdixtester.ui.components.LtrContent
import com.jubayerhossain.bdixtester.ui.theme.BdixBorder
import com.jubayerhossain.bdixtester.ui.theme.BdixCardShapes
import com.jubayerhossain.bdixtester.ui.theme.BdixSpacing
import com.jubayerhossain.bdixtester.ui.theme.LocalBdixSemanticColors
import com.jubayerhossain.bdixtester.ui.theme.MonoUrlStyle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogGovernanceScreen(
  uiState: CatalogGovernanceUiState,
  onNavigateBack: () -> Unit,
  onSelectFilter: (GovernanceFilter) -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onRunVerification: () -> Unit,
  onImportIspProbesJson: (String) -> Result<IspProbeLoadResult>,
  onOpenEvidence: (String) -> Unit,
  onDismissEvidence: () -> Unit,
  onVerifySingle: (String) -> Unit,
  onMarkCandidate: (String) -> Unit,
  onArchive: (String) -> Unit,
  onRestore: (String) -> Unit,
  observeEventsFor: (String?) -> kotlinx.coroutines.flow.Flow<List<com.jubayerhossain.bdixtester.core.model.VerificationEventRecord>>,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val resources = LocalResources.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val semantic = LocalBdixSemanticColors.current

  val candidateMsg = stringResource(R.string.governance_action_completed_candidate)
  val archiveMsg = stringResource(R.string.governance_action_completed_archive)
  val restoreMsg = stringResource(R.string.governance_action_completed_restore)

  // Request POST_NOTIFICATIONS gracefully on Android 13+; verification still proceeds even if denied.
  val notificationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
      onRunVerification()
    }

  val ispProbeDocumentLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri == null) return@rememberLauncherForActivityResult
      runCatching {
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
          ?: error("Empty file")
      }.fold(
        onSuccess = { jsonText ->
          onImportIspProbesJson(jsonText).fold(
            onSuccess = { result ->
              scope.launch {
                snackbarHostState.showSnackbar(
                  message = resources.getString(R.string.governance_isp_import_success, result.freshCount, result.staleCount),
                  duration = SnackbarDuration.Short,
                )
              }
            },
            onFailure = { err ->
              scope.launch {
                snackbarHostState.showSnackbar(
                  message = resources.getString(R.string.governance_isp_import_error, err.message ?: "Parse failed"),
                  duration = SnackbarDuration.Long,
                )
              }
            },
          )
        },
        onFailure = { err ->
          scope.launch {
            snackbarHostState.showSnackbar(
              message = resources.getString(R.string.governance_isp_import_error, err.message ?: "Read failed"),
              duration = SnackbarDuration.Long,
            )
          }
        },
      )
    }

  Scaffold(
    modifier =
      modifier
        .fillMaxSize()
        .testTag("catalog_governance_screen"),
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.statusBars,
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    topBar = {
      Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          TopAppBar(
            title = {
              Text(
                text = stringResource(R.string.governance_title),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            },
            navigationIcon = {
              IconButton(
                onClick = onNavigateBack,
                modifier =
                  Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("governance_back_button"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                  contentDescription = stringResource(R.string.action_back),
                )
              }
            },
            actions = {
              IconButton(
                onClick = { ispProbeDocumentLauncher.launch(arrayOf("application/json", "text/plain")) },
                modifier =
                  Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("governance_import_isp_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.UploadFile,
                  contentDescription = stringResource(R.string.governance_import_isp_probes),
                )
              }
            },
            colors =
              TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
              ),
          )
          HorizontalDivider(
            thickness = BdixBorder.hairline,
            color = MaterialTheme.colorScheme.outlineVariant,
          )
        }
      }
    },
  ) { innerPadding ->
    LazyColumn(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .testTag("governance_list"),
      contentPadding =
        PaddingValues(
          start = BdixSpacing.lg,
          end = BdixSpacing.lg,
          top = BdixSpacing.md,
          bottom = BdixSpacing.xxl,
        ),
      verticalArrangement = Arrangement.spacedBy(BdixSpacing.md),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      item(key = "overview_card") {
        GovernanceOverviewCard(
          counts = uiState.counts,
          progress = uiState.runProgress,
          onRunClick = {
            val needsPermission =
              Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            if (needsPermission) {
              notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
              onRunVerification()
            }
          },
          onImportClick = { ispProbeDocumentLauncher.launch(arrayOf("application/json", "text/plain")) },
        )
      }

      item(key = "search_bar") {
        BdixSearchBar(
          query = uiState.searchQuery,
          onQueryChange = onSearchQueryChange,
          testTag = "governance_search_input",
          placeholderRes = R.string.governance_search_placeholder,
        )
      }

      item(key = "filter_chips") {
        GovernanceFilterRow(
          selectedFilter = uiState.selectedFilter,
          counts = uiState.counts,
          onSelectFilter = onSelectFilter,
        )
      }

      if (uiState.items.isEmpty()) {
        item(key = "empty_state") {
          EmptyStateView(
            icon = Icons.Rounded.Shield,
            title = stringResource(R.string.governance_empty_title),
            description = stringResource(R.string.governance_empty_desc),
            tag = "governance_empty_state",
            modifier = Modifier.widthIn(max = 720.dp),
          )
        }
      } else {
        items(
          items = uiState.items,
          key = { it.endpoint.id },
        ) { item ->
          GovernanceEndpointCard(
            item = item,
            onClick = { onOpenEvidence(item.endpoint.id) },
            onQuickRestore = {
              onRestore(item.endpoint.id)
              scope.launch {
                snackbarHostState.showSnackbar(restoreMsg, duration = SnackbarDuration.Short)
              }
            },
          )
        }
      }
    }
  }

  val inspectedItem = uiState.selectedItem
  if (inspectedItem != null) {
    val eventsFlow = remember(inspectedItem.endpoint.id) { observeEventsFor(inspectedItem.endpoint.id) }
    val events by eventsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    GovernanceEvidenceSheet(
      endpoint = inspectedItem.endpoint,
      record = inspectedItem.record,
      events = events,
      onDismiss = onDismissEvidence,
      onVerifySingle = onVerifySingle,
      onMarkCandidate = { id ->
        onMarkCandidate(id)
        scope.launch { snackbarHostState.showSnackbar(candidateMsg, duration = SnackbarDuration.Short) }
      },
      onArchive = { id ->
        onArchive(id)
        scope.launch { snackbarHostState.showSnackbar(archiveMsg, duration = SnackbarDuration.Short) }
      },
      onRestore = { id ->
        onRestore(id)
        scope.launch { snackbarHostState.showSnackbar(restoreMsg, duration = SnackbarDuration.Short) }
      },
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GovernanceOverviewCard(
  counts: GovernanceOverviewCounts,
  progress: VerificationRunProgress,
  onRunClick: () -> Unit,
  onImportClick: () -> Unit,
) {
  val semantic = LocalBdixSemanticColors.current
  Surface(
    modifier =
      Modifier
        .fillMaxWidth()
        .widthIn(max = 720.dp)
        .testTag("governance_overview_card"),
    shape = BdixCardShapes.richCard,
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(BdixBorder.hairline, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(
      modifier = Modifier.padding(BdixSpacing.lg),
      verticalArrangement = Arrangement.spacedBy(BdixSpacing.md),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BdixSpacing.sm),
      ) {
        Icon(
          imageVector = Icons.Rounded.Shield,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = stringResource(R.string.governance_overview_title),
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface,
        )
      }

      Text(
        text = stringResource(R.string.governance_policy_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(BdixSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(BdixSpacing.xs),
      ) {
        OverviewStatBadge(
          label = stringResource(R.string.governance_filter_active, counts.activeCount),
          color = semantic.reachableContainer,
          contentColor = semantic.onReachableContainer,
        )
        OverviewStatBadge(
          label = stringResource(R.string.governance_filter_candidates, counts.removalCandidateCount),
          color = semantic.unreachableContainer,
          contentColor = semantic.onUnreachableContainer,
        )
        OverviewStatBadge(
          label = stringResource(R.string.governance_filter_archived, counts.archivedCount),
          color = semantic.neutralContainer,
          contentColor = semantic.onNeutralContainer,
        )
      }

      if (progress.isRunning) {
        Column(verticalArrangement = Arrangement.spacedBy(BdixSpacing.xs)) {
          val fraction =
            if (progress.totalCount > 0) {
              progress.processedCount.toFloat() / progress.totalCount.toFloat()
            } else {
              0f
            }
          LinearProgressIndicator(
            progress = { fraction },
            modifier =
              Modifier
                .fillMaxWidth()
                .testTag("governance_progress_bar"),
          )
          Text(
            text =
              stringResource(
                R.string.governance_running_progress,
                progress.processedCount,
                progress.totalCount,
                progress.currentEndpointName ?: "",
              ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BdixSpacing.sm),
      ) {
        Button(
          onClick = onRunClick,
          enabled = !progress.isRunning,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
          modifier =
            Modifier
              .weight(1f)
              .heightIn(min = 48.dp)
              .testTag("governance_run_verification_button"),
        ) {
          Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(BdixSpacing.xs))
          Text(
            text = stringResource(R.string.governance_run_verification),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        OutlinedButton(
          onClick = onImportClick,
          modifier =
            Modifier
              .heightIn(min = 48.dp)
              .testTag("governance_import_isp_cta"),
        ) {
          Icon(imageVector = Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
        }
      }
    }
  }
}

@Composable
private fun OverviewStatBadge(
  label: String,
  color: androidx.compose.ui.graphics.Color,
  contentColor: androidx.compose.ui.graphics.Color,
) {
  Surface(shape = BdixCardShapes.chip, color = color) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
      color = contentColor,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}

@Composable
private fun GovernanceFilterRow(
  selectedFilter: GovernanceFilter,
  counts: GovernanceOverviewCounts,
  onSelectFilter: (GovernanceFilter) -> Unit,
) {
  val semantic = LocalBdixSemanticColors.current
  val chips =
    listOf(
      Triple(GovernanceFilter.ALL, stringResource(R.string.governance_filter_all, counts.totalCatalogCount), "governance_filter_all"),
      Triple(GovernanceFilter.REMOVAL_CANDIDATES, stringResource(R.string.governance_filter_candidates, counts.removalCandidateCount), "governance_filter_candidates"),
      Triple(GovernanceFilter.EXPIRED_SUSPECTED, stringResource(R.string.governance_filter_expired_suspected, counts.expiredSuspectedCount), "governance_filter_expired_suspected"),
      Triple(GovernanceFilter.ARCHIVED, stringResource(R.string.governance_filter_archived, counts.archivedCount), "governance_filter_archived"),
      Triple(GovernanceFilter.ACTIVE, stringResource(R.string.governance_filter_active, counts.activeCount), "governance_filter_active"),
      Triple(GovernanceFilter.ISP_RESTRICTED, stringResource(R.string.governance_filter_isp_restricted, counts.ispRestrictedCount), "governance_filter_isp_restricted"),
      Triple(GovernanceFilter.UNREACHABLE, stringResource(R.string.governance_filter_unreachable, counts.unreachableCount), "governance_filter_unreachable"),
      Triple(GovernanceFilter.TEMPORARILY_UNAVAILABLE, stringResource(R.string.governance_filter_temp_unavailable, counts.temporarilyUnavailableCount), "governance_filter_temp_unavailable"),
      Triple(GovernanceFilter.UNVERIFIED, stringResource(R.string.governance_filter_unverified, counts.unverifiedCount), "governance_filter_unverified"),
    )

  LazyRow(
    modifier =
      Modifier
        .fillMaxWidth()
        .widthIn(max = 720.dp)
        .testTag("governance_filter_bar"),
    horizontalArrangement = Arrangement.spacedBy(BdixSpacing.sm),
  ) {
    items(chips, key = { it.first.name }) { (filter, label, tag) ->
      val selected = filter == selectedFilter
      FilterChip(
        selected = selected,
        onClick = { onSelectFilter(filter) },
        label = { Text(label, maxLines = 1) },
        colors =
          FilterChipDefaults.filterChipColors(
            selectedContainerColor = semantic.filterChipSelected,
            selectedLabelColor = semantic.onFilterChipSelected,
            containerColor = semantic.filterChipUnselected,
          ),
        modifier =
          Modifier
            .heightIn(min = 48.dp)
            .testTag(tag),
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GovernanceEndpointCard(
  item: GovernanceListItem,
  onClick: () -> Unit,
  onQuickRestore: () -> Unit,
) {
  val semantic = LocalBdixSemanticColors.current
  Surface(
    modifier =
      Modifier
        .fillMaxWidth()
        .widthIn(max = 720.dp)
        .clickable(onClick = onClick)
        .testTag("governance_item_${item.endpoint.id}"),
    shape = BdixCardShapes.richCard,
    color = semantic.cardBackground,
    border = BorderStroke(BdixBorder.hairline, semantic.cardBorder),
  ) {
    Column(
      modifier = Modifier.padding(BdixSpacing.lg),
      verticalArrangement = Arrangement.spacedBy(BdixSpacing.xs),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = item.endpoint.name,
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(BdixSpacing.sm))
        LifecycleStatusPill(status = item.record.lifecycleStatus)
      }

      LtrContent {
        Text(
          text = item.endpoint.originalUrl,
          style = MonoUrlStyle,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(BdixSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(BdixSpacing.xs),
      ) {
        ConfidencePill(confidence = item.record.confidence)
        Surface(
          shape = BdixCardShapes.chip,
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
          Text(
            text = "DNS: ${item.record.dnsSummary.wireName}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
        Surface(
          shape = BdixCardShapes.chip,
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
          Text(
            text = "RDAP: ${item.record.registrationStatus.wireName}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }

      Text(
        text = item.record.reason,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )

      if (item.record.lifecycleStatus == VerificationLifecycleStatus.ARCHIVED) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
          OutlinedButton(
            onClick = onQuickRestore,
            modifier =
              Modifier
                .heightIn(min = 48.dp)
                .testTag("governance_quick_restore_${item.endpoint.id}"),
          ) {
            Icon(imageVector = Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(BdixSpacing.xs))
            Text(stringResource(R.string.governance_action_restore))
          }
        }
      }
    }
  }
}
