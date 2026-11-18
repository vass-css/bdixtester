package com.jubayerhossain.bdixtester.feature.favorites

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jubayerhossain.bdixtester.R
import com.jubayerhossain.bdixtester.core.export.ExportDataset
import com.jubayerhossain.bdixtester.core.export.ExportFormat
import com.jubayerhossain.bdixtester.core.model.ServerProtocol
import com.jubayerhossain.bdixtester.core.model.TestStatus
import com.jubayerhossain.bdixtester.domain.model.ServerTestResult
import com.jubayerhossain.bdixtester.ui.components.EmptyStateView
import com.jubayerhossain.bdixtester.ui.components.ExportMenuButton
import com.jubayerhossain.bdixtester.ui.components.LtrContent
import com.jubayerhossain.bdixtester.ui.components.ServerCard
import com.jubayerhossain.bdixtester.ui.components.StatusBadge
import com.jubayerhossain.bdixtester.ui.components.springPressScale
import com.jubayerhossain.bdixtester.ui.theme.BdixBorder
import com.jubayerhossain.bdixtester.ui.theme.BdixIconSize
import com.jubayerhossain.bdixtester.ui.theme.BdixSpacing
import com.jubayerhossain.bdixtester.ui.theme.BdixTesterTheme
import com.jubayerhossain.bdixtester.ui.theme.MonoUrlStyle
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
) {
  Scaffold(
    modifier =
      modifier
        .fillMaxSize()
        .testTag("favorites_screen"),
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.statusBars,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = stringResource(R.string.saved_title),
            style = MaterialTheme.typography.titleLarge,
          )
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
    },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(innerPadding),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      SearchFieldRow(
        query = uiState.searchQuery,
        onQueryChange = onSearchQueryChange,
        testTag = "saved_search_input",
      )

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
          item {
            EmptyStateView(
              icon = Icons.Rounded.BookmarkBorder,
              title = stringResource(R.string.favorites_empty_title),
              description = stringResource(R.string.favorites_empty_desc),
              tag = "favorites_empty_state",
            )
          }
        } else {
          items(
            items = uiState.favoriteItems,
            key = { it.endpoint.id },
          ) { item ->
            ServerCard(
              endpoint = item.endpoint,
              result = item.latestResult,
              isFavorite = true,
              isPinned = item.favorite.isPinned,
              onToggleFavorite = { onToggleFavorite(item.endpoint.id) },
              onTogglePin = { onSetPinned(item.endpoint.id, !item.favorite.isPinned) },
              onVisitWebsiteClick = { onOpenUrl(item.endpoint.originalUrl) },
              modifier = Modifier.animateItem(),
            )
          }
        }
      }
    }
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
      TopAppBar(
        title = {
          Text(
            text = stringResource(R.string.history_title),
            style = MaterialTheme.typography.titleLarge,
          )
        },
        actions = {
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
    },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(innerPadding),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      SearchFieldRow(
        query = uiState.searchQuery,
        onQueryChange = onSearchQueryChange,
        testTag = "history_search_input",
      )

      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
            .padding(start = BdixSpacing.lg, end = BdixSpacing.lg, bottom = BdixSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        LazyRow(
          modifier = Modifier.weight(1f),
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
            FilterChip(
              selected = uiState.historyStatusFilter == st,
              onClick = { onSelectStatusFilter(if (uiState.historyStatusFilter == st) null else st) },
              label = { Text(st.displayLabel) },
              modifier = Modifier.heightIn(min = 48.dp),
            )
          }
          items(ServerProtocol.entries) { proto ->
            FilterChip(
              selected = uiState.historyProtocolFilter == proto,
              onClick = {
                onSelectProtocolFilter(
                  if (uiState.historyProtocolFilter ==
                    proto
                  ) {
                    null
                  } else {
                    proto
                  },
                )
              },
              label = { Text(proto.displayLabel) },
              modifier = Modifier.heightIn(min = 48.dp),
            )
          }
        }
        if (uiState.historyItems.isNotEmpty()) {
          OutlinedButton(
            onClick = { showClearHistoryDialog = true },
            modifier =
              Modifier
                .padding(start = BdixSpacing.sm)
                .heightIn(min = 48.dp)
                .testTag("history_clear_all_button"),
          ) {
            Text(stringResource(R.string.history_clear_all))
          }
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
          item {
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
          ) { entry ->
            HistoryRowCard(
              entry = entry,
              onOpenUrl = { onOpenUrl(entry.urlSnapshot) },
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
private fun SearchFieldRow(
  query: String,
  onQueryChange: (String) -> Unit,
  testTag: String,
) {
  OutlinedTextField(
    value = query,
    onValueChange = onQueryChange,
    modifier =
      Modifier
        .fillMaxWidth()
        .widthIn(max = 720.dp)
        .padding(horizontal = BdixSpacing.lg, vertical = BdixSpacing.md)
        .testTag(testTag),
    placeholder = { Text(stringResource(R.string.servers_search_placeholder)) },
    leadingIcon = { Icon(imageVector = Icons.Rounded.Search, contentDescription = null) },
    trailingIcon = {
      if (query.isNotEmpty()) {
        IconButton(
          onClick = { onQueryChange("") },
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Icon(
            imageVector = Icons.Rounded.Clear,
            contentDescription = stringResource(R.string.action_clear_filters),
          )
        }
      }
    },
    singleLine = true,
    shape = MaterialTheme.shapes.medium,
  )
}

@Composable
private fun HistoryRowCard(
  entry: ServerTestResult,
  onOpenUrl: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current
  val interactionSource = remember { MutableInteractionSource() }
  val displayName = entry.serverNameSnapshot.ifBlank { entry.serverId }
  val urlA11y = stringResource(R.string.servers_open_url_a11y, displayName)

  Surface(
    modifier =
      modifier
        .fillMaxWidth()
        .springPressScale(interactionSource = interactionSource, pressedScale = 0.975f)
        .testTag("history_item_${entry.historyId}"),
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(BdixBorder.hairline, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // Tapping anywhere on the card launches the URL directly in external browser
      Column(
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
            ).padding(BdixSpacing.md),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = displayName,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
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
            )
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          StatusBadge(status = entry.status)
          LtrContent {
            Text(
              text =
                DateFormat
                  .getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.US)
                  .format(Date(entry.testedAtEpochMs)),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        LtrContent {
          Text(
            text =
              buildString {
                append("${entry.scanMode.displayLabel} · ${entry.protocol.displayLabel}")
                entry.primaryLatencyMs?.let { append(" · $it ms") }
                entry.downloadMbps?.let { append(" · ${"%.2f".format(Locale.US, it)} Mbps") }
                entry.networkProfileLabel?.let { append(" · [$it]") }
              },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      if (entry.urlSnapshot.isNotBlank()) {
        HorizontalDivider(
          thickness = BdixBorder.hairline,
          color = MaterialTheme.colorScheme.outlineVariant,
        )

        // Isolated full-width >= 48dp external browser URL launch row
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .heightIn(min = 48.dp)
              .clickable(
                role = Role.Button,
                onClickLabel = urlA11y,
                onClick = {
                  haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                  onOpenUrl()
                },
              ).semantics { contentDescription = urlA11y }
              .padding(horizontal = BdixSpacing.md, vertical = BdixSpacing.sm)
              .testTag("history_url_link_${entry.historyId}"),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          LtrContent {
            Text(
              text = entry.urlSnapshot,
              style = MonoUrlStyle,
              color = MaterialTheme.colorScheme.primary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier =
                Modifier
                  .weight(1f, fill = false)
                  .padding(end = BdixSpacing.sm),
            )
          }
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(BdixIconSize.sm),
          )
        }
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
