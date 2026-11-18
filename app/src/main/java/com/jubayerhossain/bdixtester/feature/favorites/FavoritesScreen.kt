package com.jubayerhossain.bdixtester.feature.favorites

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FolderShared
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Http
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jubayerhossain.bdixtester.R
import com.jubayerhossain.bdixtester.core.export.ExportDataset
import com.jubayerhossain.bdixtester.core.export.ExportFormat
import com.jubayerhossain.bdixtester.core.model.ServerProtocol
import com.jubayerhossain.bdixtester.core.model.TestStatus
import com.jubayerhossain.bdixtester.domain.model.ServerEndpoint
import com.jubayerhossain.bdixtester.domain.model.ServerTestResult
import com.jubayerhossain.bdixtester.ui.components.BdixCategoryIconBadge
import com.jubayerhossain.bdixtester.ui.components.BdixSearchBar
import com.jubayerhossain.bdixtester.ui.components.EmptyStateView
import com.jubayerhossain.bdixtester.ui.components.ExportMenuButton
import com.jubayerhossain.bdixtester.ui.components.LtrContent
import com.jubayerhossain.bdixtester.ui.components.RichCardUrlFooter
import com.jubayerhossain.bdixtester.ui.components.ServerCard
import com.jubayerhossain.bdixtester.ui.components.ServerDetailBottomSheet
import com.jubayerhossain.bdixtester.ui.components.StatusBadge
import com.jubayerhossain.bdixtester.ui.components.springPressScale
import com.jubayerhossain.bdixtester.ui.theme.BdixBorder
import com.jubayerhossain.bdixtester.ui.theme.BdixCardShapes
import com.jubayerhossain.bdixtester.ui.theme.BdixElevation
import com.jubayerhossain.bdixtester.ui.theme.BdixSpacing
import com.jubayerhossain.bdixtester.ui.theme.BdixTesterTheme
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedServersScreen(
  uiState: FavoritesUiState,
  onSearchQueryChange: (String) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onSetPinned: (String, Boolean) -> Unit,
  onOpenUrl: (String) -> Unit,
  onExportFavorites: (ExportFormat) -> String,
  modifier: Modifier = Modifier,
  onCopyUrl: (String) -> Unit = {},
  onTestSingleServer: ((ServerEndpoint) -> Unit)? = null,
) {
  var inspectItem by remember { mutableStateOf<FavoriteServerItem?>(null) }

  Scaffold(
    modifier =
      modifier
        .fillMaxSize()
        .testTag("favorites_screen"),
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.statusBars,
    topBar = {
      Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          TopAppBar(
            title = {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BdixSpacing.sm),
              ) {
                Text(
                  text = stringResource(R.string.saved_title),
                  style = MaterialTheme.typography.titleLarge,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                if (uiState.favoriteItems.isNotEmpty()) {
                  Surface(
                    shape = BdixCardShapes.pill,
                    color = MaterialTheme.colorScheme.primaryContainer,
                  ) {
                    LtrContent {
                      Text(
                        text = uiState.favoriteItems.size.toString(),
                        style =
                          MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum",
                          ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                      )
                    }
                  }
                }
              }
            },
            actions = {
              ExportMenuButton(
                dataset = ExportDataset.FAVORITES,
                onGeneratePayload = onExportFavorites,
                buttonTestTag = "saved_export_button",
              )
            },
            colors =
              TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface,
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
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(innerPadding),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      BdixSearchBar(
        query = uiState.searchQuery,
        onQueryChange = onSearchQueryChange,
        testTag = "saved_search_input",
        modifier = Modifier.padding(horizontal = BdixSpacing.lg, vertical = BdixSpacing.md),
      )

      val pinnedItems = remember(uiState.favoriteItems) { uiState.favoriteItems.filter { it.favorite.isPinned } }
      val unpinnedItems = remember(uiState.favoriteItems) { uiState.favoriteItems.filter { !it.favorite.isPinned } }
      val showGroupHeaders = pinnedItems.isNotEmpty() && unpinnedItems.isNotEmpty()

      LazyColumn(
        modifier =
          Modifier
            .fillMaxSize()
            .widthIn(max = 720.dp),
        contentPadding =
          PaddingValues(
            start = BdixSpacing.lg,
            end = BdixSpacing.lg,
            bottom = BdixSpacing.xl,
          ),
        verticalArrangement = Arrangement.spacedBy(BdixSpacing.md),
      ) {
        if (uiState.favoriteItems.isEmpty()) {
          item(contentType = "empty_state") {
            EmptyStateView(
              icon = Icons.Rounded.BookmarkBorder,
              title = stringResource(R.string.favorites_empty_title),
              description = stringResource(R.string.favorites_empty_desc),
              tag = "favorites_empty_state",
            )
          }
        } else if (showGroupHeaders) {
          item(key = "header_pinned", contentType = "section_header") {
            SectionGroupLabel(title = stringResource(R.string.saved_section_pinned))
          }
          items(
            items = pinnedItems,
            key = { it.endpoint.id },
            contentType = { "saved_server_card" },
          ) { item ->
            ServerCard(
              endpoint = item.endpoint,
              result = item.latestResult,
              isFavorite = true,
              isPinned = true,
              onToggleFavorite = { onToggleFavorite(item.endpoint.id) },
              onTogglePin = { onSetPinned(item.endpoint.id, false) },
              onInspectClick = { inspectItem = item },
              onVisitWebsiteClick = { onOpenUrl(item.endpoint.originalUrl) },
              onLongClickCopyUrl = { onCopyUrl(item.endpoint.originalUrl) },
              modifier = Modifier.animateItem(),
            )
          }
          item(key = "header_all", contentType = "section_header") {
            SectionGroupLabel(
              title = stringResource(R.string.saved_section_all),
              modifier = Modifier.padding(top = BdixSpacing.xs),
            )
          }
          items(
            items = unpinnedItems,
            key = { it.endpoint.id },
            contentType = { "saved_server_card" },
          ) { item ->
            ServerCard(
              endpoint = item.endpoint,
              result = item.latestResult,
              isFavorite = true,
              isPinned = false,
              onToggleFavorite = { onToggleFavorite(item.endpoint.id) },
              onTogglePin = { onSetPinned(item.endpoint.id, true) },
              onInspectClick = { inspectItem = item },
              onVisitWebsiteClick = { onOpenUrl(item.endpoint.originalUrl) },
              onLongClickCopyUrl = { onCopyUrl(item.endpoint.originalUrl) },
              modifier = Modifier.animateItem(),
            )
          }
        } else {
          items(
            items = uiState.favoriteItems,
            key = { it.endpoint.id },
            contentType = { "saved_server_card" },
          ) { item ->
            ServerCard(
              endpoint = item.endpoint,
              result = item.latestResult,
              isFavorite = true,
              isPinned = item.favorite.isPinned,
              onToggleFavorite = { onToggleFavorite(item.endpoint.id) },
              onTogglePin = { onSetPinned(item.endpoint.id, !item.favorite.isPinned) },
              onInspectClick = { inspectItem = item },
              onVisitWebsiteClick = { onOpenUrl(item.endpoint.originalUrl) },
              onLongClickCopyUrl = { onCopyUrl(item.endpoint.originalUrl) },
              modifier = Modifier.animateItem(),
            )
          }
        }
      }
    }
  }

  inspectItem?.let { item ->
    ServerDetailBottomSheet(
      endpoint = item.endpoint,
      result = item.latestResult,
      onDismiss = { inspectItem = null },
      onOpenUrl = { onOpenUrl(item.endpoint.originalUrl) },
      onCopyUrl = { onCopyUrl(item.endpoint.originalUrl) },
      onTestNow = onTestSingleServer?.let { tester -> { tester(item.endpoint) } },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
  uiState: FavoritesUiState,
  onSearchQueryChange: (String) -> Unit,
  onSelectStatusFilter: (TestStatus?) -> Unit,
  onSelectProtocolFilter: (ServerProtocol?) -> Unit,
  onOpenUrl: (String) -> Unit,
  onDeleteHistoryEntry: (Long) -> Unit,
  onClearAllHistory: () -> Unit,
  onExportHistory: (ExportFormat) -> String,
  modifier: Modifier = Modifier,
  onCopyUrl: (String) -> Unit = {},
) {
  var showClearHistoryDialog by remember { mutableStateOf(false) }

  Scaffold(
    modifier =
      modifier
        .fillMaxSize()
        .testTag("history_screen"),
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.statusBars,
    topBar = {
      Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          TopAppBar(
            title = {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BdixSpacing.sm),
              ) {
                Text(
                  text = stringResource(R.string.history_title),
                  style = MaterialTheme.typography.titleLarge,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                if (uiState.historyItems.isNotEmpty()) {
                  Surface(
                    shape = BdixCardShapes.pill,
                    color = MaterialTheme.colorScheme.primaryContainer,
                  ) {
                    LtrContent {
                      Text(
                        text = uiState.historyItems.size.toString(),
                        style =
                          MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum",
                          ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                      )
                    }
                  }
                }
              }
            },
            actions = {
              if (uiState.historyItems.isNotEmpty()) {
                IconButton(
                  onClick = { showClearHistoryDialog = true },
                  modifier =
                    Modifier
                      .minimumInteractiveComponentSize()
                      .testTag("history_clear_all_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.DeleteSweep,
                    contentDescription = stringResource(R.string.history_clear_all),
                    tint = MaterialTheme.colorScheme.error,
                  )
                }
              }
              ExportMenuButton(
                dataset = ExportDataset.HISTORY,
                onGeneratePayload = onExportHistory,
                buttonTestTag = "history_export_button",
              )
            },
            colors =
              TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface,
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
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(innerPadding),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      BdixSearchBar(
        query = uiState.searchQuery,
        onQueryChange = onSearchQueryChange,
        testTag = "history_search_input",
        modifier = Modifier.padding(horizontal = BdixSpacing.lg, vertical = BdixSpacing.md),
      )

      LazyRow(
        modifier =
          Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
            .padding(bottom = BdixSpacing.sm),
        contentPadding = PaddingValues(horizontal = BdixSpacing.lg),
        horizontalArrangement = Arrangement.spacedBy(BdixSpacing.sm),
      ) {
        items(
          listOf(
            TestStatus.REACHABLE,
            TestStatus.RESTRICTED,
            TestStatus.UNREACHABLE,
            TestStatus.TIMED_OUT,
          ),
        ) { st ->
          val selected = uiState.historyStatusFilter == st
          FilterChip(
            selected = selected,
            onClick = { onSelectStatusFilter(if (selected) null else st) },
            label = { Text(st.displayLabel, maxLines = 1) },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
              ),
            border =
              FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = selected,
                borderColor = MaterialTheme.colorScheme.outlineVariant,
                selectedBorderColor = MaterialTheme.colorScheme.primary,
              ),
            modifier = Modifier.heightIn(min = 48.dp),
          )
        }
        items(ServerProtocol.entries) { proto ->
          val selected = uiState.historyProtocolFilter == proto
          FilterChip(
            selected = selected,
            onClick = { onSelectProtocolFilter(if (selected) null else proto) },
            label = { Text(proto.displayLabel, maxLines = 1) },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
              ),
            border =
              FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = selected,
                borderColor = MaterialTheme.colorScheme.outlineVariant,
                selectedBorderColor = MaterialTheme.colorScheme.primary,
              ),
            modifier = Modifier.heightIn(min = 48.dp),
          )
        }
      }

      LazyColumn(
        modifier =
          Modifier
            .fillMaxSize()
            .widthIn(max = 720.dp),
        contentPadding =
          PaddingValues(
            start = BdixSpacing.lg,
            end = BdixSpacing.lg,
            bottom = BdixSpacing.xl,
          ),
        verticalArrangement = Arrangement.spacedBy(BdixSpacing.md),
      ) {
        if (uiState.historyItems.isEmpty()) {
          item(contentType = "empty_state") {
            EmptyStateView(
              icon = Icons.Rounded.History,
              title = stringResource(R.string.history_empty_title),
              description = stringResource(R.string.history_empty_desc),
              tag = "history_empty_state",
            )
          }
        } else {
          items(
            items = uiState.historyItems,
            key = { it.historyId },
            contentType = { "history_card" },
          ) { entry ->
            HistoryRowCard(
              entry = entry,
              onOpenUrl = { onOpenUrl(entry.urlSnapshot) },
              onCopyUrl = { onCopyUrl(entry.urlSnapshot) },
              onDelete = { onDeleteHistoryEntry(entry.historyId) },
              modifier = Modifier.animateItem(),
            )
          }
        }
      }
    }
  }

  if (showClearHistoryDialog) {
    AlertDialog(
      onDismissRequest = { showClearHistoryDialog = false },
      containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
      title = { Text(stringResource(R.string.history_clear_confirm_title)) },
      text = { Text(stringResource(R.string.history_clear_confirm_body)) },
      confirmButton = {
        Button(
          onClick = {
            onClearAllHistory()
            showClearHistoryDialog = false
          },
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.error,
              contentColor = MaterialTheme.colorScheme.onError,
            ),
          modifier = Modifier.heightIn(min = 48.dp),
        ) {
          Text(stringResource(R.string.action_delete))
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showClearHistoryDialog = false },
          modifier = Modifier.heightIn(min = 48.dp),
        ) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    )
  }
}

@Composable
private fun SectionGroupLabel(
  title: String,
  modifier: Modifier = Modifier,
) {
  Text(
    text = title,
    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    color = MaterialTheme.colorScheme.primary,
    modifier = modifier.padding(horizontal = BdixSpacing.xs, vertical = 2.dp),
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryRowCard(
  entry: ServerTestResult,
  onOpenUrl: () -> Unit,
  onCopyUrl: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current
  val interactionSource = remember { MutableInteractionSource() }
  val displayName = entry.serverNameSnapshot.ifBlank { entry.serverId }
  val urlA11y = stringResource(R.string.servers_open_url_a11y, displayName)
  val protocolIcon =
    when (entry.protocol) {
      ServerProtocol.FTP, ServerProtocol.FTPS -> Icons.Rounded.FolderShared
      else -> Icons.Rounded.Http
    }

  Card(
    modifier =
      modifier
        .fillMaxWidth()
        .springPressScale(interactionSource = interactionSource, pressedScale = 0.975f)
        .testTag("history_item_${entry.historyId}"),
    shape = BdixCardShapes.richCard,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    elevation = CardDefaults.cardElevation(defaultElevation = BdixElevation.card),
    border = BorderStroke(BdixBorder.hairline, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // Tier 1: Icon Badge, Name, Consolidated Status+Latency, Timestamp & Protocol/Speed Pill
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .clickable(
              interactionSource = interactionSource,
              indication = ripple(),
              enabled = entry.urlSnapshot.isNotBlank(),
              role = Role.Button,
              onClickLabel = urlA11y,
              onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onOpenUrl()
              },
            ).padding(
              start = BdixSpacing.lg,
              end = BdixSpacing.xs,
              top = BdixSpacing.md,
              bottom = BdixSpacing.md,
            ),
        horizontalArrangement = Arrangement.spacedBy(BdixSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        BdixCategoryIconBadge(icon = protocolIcon)

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = displayName,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            StatusBadge(status = entry.status, latencyMs = entry.primaryLatencyMs)

            Surface(
              color = MaterialTheme.colorScheme.surfaceContainerHigh,
              shape = BdixCardShapes.chip,
            ) {
              LtrContent {
                Text(
                  text =
                    buildString {
                      append("${entry.scanMode.displayLabel} · ${entry.protocol.displayLabel}")
                      entry.downloadMbps?.let { append(" · ${"%.2f".format(Locale.US, it)} Mbps") }
                    },
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  maxLines = 1,
                )
              }
            }
          }

          LtrContent {
            val formattedTime =
              DateFormat
                .getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.US)
                .format(Date(entry.testedAtEpochMs))
            val subtitle =
              if (!entry.networkProfileLabel.isNullOrBlank()) {
                "$formattedTime · ${entry.networkProfileLabel}"
              } else {
                formattedTime
              }
            Text(
              text = subtitle,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        IconButton(
          onClick = onDelete,
          modifier =
            Modifier
              .minimumInteractiveComponentSize()
              .testTag("history_delete_${entry.historyId}"),
        ) {
          Icon(
            imageVector = Icons.Rounded.DeleteOutline,
            contentDescription = stringResource(R.string.action_delete),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // Tier 2: Shared URL Footer with Open Pill & Long-Press Copy
      if (entry.urlSnapshot.isNotBlank()) {
        RichCardUrlFooter(
          url = entry.urlSnapshot,
          urlA11y = urlA11y,
          testTag = "history_url_link_${entry.historyId}",
          onClick = onOpenUrl,
          onLongClick = onCopyUrl,
        )
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun SavedServersScreenPreview() {
  BdixTesterTheme {
    SavedServersScreen(
      uiState = FavoritesUiState(),
      onSearchQueryChange = {},
      onToggleFavorite = {},
      onSetPinned = { _, _ -> },
      onOpenUrl = {},
      onExportFavorites = { "" },
    )
  }
}
