package com.jubayerhossain.bdixtester.feature.scan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jubayerhossain.bdixtester.R
import com.jubayerhossain.bdixtester.core.export.ExportDataset
import com.jubayerhossain.bdixtester.core.export.ExportFormat
import com.jubayerhossain.bdixtester.core.model.ScanMode
import com.jubayerhossain.bdixtester.core.model.ScanScope
import com.jubayerhossain.bdixtester.core.model.ScanSessionState
import com.jubayerhossain.bdixtester.core.model.ServerCategory
import com.jubayerhossain.bdixtester.ui.components.ExportMenuButton
import com.jubayerhossain.bdixtester.ui.components.LtrContent
import com.jubayerhossain.bdixtester.ui.components.MetricTile
import com.jubayerhossain.bdixtester.ui.theme.BdixSpacing
import com.jubayerhossain.bdixtester.ui.theme.BdixTesterTheme
import com.jubayerhossain.bdixtester.ui.theme.LocalBdixSemanticColors
import com.jubayerhossain.bdixtester.ui.theme.MonoMetricStyle

@Composable
fun ScanScreen(
  uiState: ScanUiState,
  onSelectMode: (ScanMode) -> Unit,
  onSelectScope: (ScanScope) -> Unit,
  onSelectScopeCategory: (ServerCategory) -> Unit,
  onStartScan: () -> Unit,
  onStopScan: () -> Unit,
  onExportSummary: (ExportFormat) -> String,
  modifier: Modifier = Modifier,
) {
  val semantic = LocalBdixSemanticColors.current
  val progress = uiState.progress

  LazyColumn(
    modifier =
      modifier
        .fillMaxSize()
        .testTag("scan_screen"),
    contentPadding = PaddingValues(BdixSpacing.lg),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(BdixSpacing.lg),
  ) {
    item {
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = stringResource(R.string.scan_title),
          style = MaterialTheme.typography.headlineSmall,
        )
        ExportMenuButton(
          dataset = ExportDataset.SCAN_SUMMARY,
          onGeneratePayload = onExportSummary,
          buttonTestTag = "scan_export_summary_button",
        )
      }
    }

    if (progress.sessionState == ScanSessionState.INTERRUPTED) {
      item {
        Surface(
          modifier =
            Modifier
              .fillMaxWidth()
              .widthIn(max = 720.dp)
              .testTag("scan_interrupted_banner"),
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        ) {
          Row(
            modifier = Modifier.padding(BdixSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(BdixSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(imageVector = Icons.Default.Info, contentDescription = null)
            Text(
              text = stringResource(R.string.scan_interrupted_notice),
              style = MaterialTheme.typography.bodySmall,
            )
          }
        }
      }
    }

    item {
      SingleChoiceSegmentedButtonRow(
        modifier =
          Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
            .testTag("scan_mode_selector"),
      ) {
        ScanMode.entries.forEachIndexed { index, mode ->
          SegmentedButton(
            selected = uiState.selectedMode == mode,
            onClick = { onSelectMode(mode) },
            enabled = !progress.isScanning,
            shape =
              SegmentedButtonDefaults.itemShape(
                index = index,
                count = ScanMode.entries.size,
              ),
            icon = {
              Icon(
                imageVector =
                  if (mode == ScanMode.QUICK_SCAN) {
                    Icons.Default.Bolt
                  } else {
                    Icons.Default.Radar
                  },
                contentDescription = null,
              )
            },
            modifier =
              Modifier
                .heightIn(min = 48.dp)
                .testTag("scan_mode_${mode.name.lowercase()}"),
          ) {
            Text(mode.displayLabel)
          }
        }
      }
    }

    item {
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp),
        verticalArrangement = Arrangement.spacedBy(BdixSpacing.xs),
      ) {
        Text(
          text = stringResource(R.string.scan_scope_title),
          style = MaterialTheme.typography.titleSmall,
        )
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(BdixSpacing.sm),
        ) {
          items(ScanScope.entries) { scope ->
            FilterChip(
              selected = uiState.selectedScope == scope,
              onClick = { if (!progress.isScanning) onSelectScope(scope) },
              enabled = !progress.isScanning,
              label = { Text(scope.displayLabel) },
              modifier =
                Modifier
                  .heightIn(min = 48.dp)
                  .testTag("scan_scope_${scope.name.lowercase()}"),
            )
          }
        }
        if (uiState.selectedScope == ScanScope.SELECTED_CATEGORY) {
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(BdixSpacing.sm),
          ) {
            items(ServerCategory.entries.filter { it != ServerCategory.ALL }) { cat ->
              FilterChip(
                selected = uiState.selectedCategory == cat,
                onClick = { if (!progress.isScanning) onSelectScopeCategory(cat) },
                enabled = !progress.isScanning,
                label = { Text(cat.displayName) },
                modifier = Modifier.heightIn(min = 48.dp),
              )
            }
          }
        }
        LtrContent {
          Text(
            text =
              stringResource(
                R.string.scan_coalescing_caption,
                if (progress.isScanning ||
                  progress.uniqueExactUrlCount > 0
                ) {
                  progress.uniqueExactUrlCount
                } else {
                  uiState.targetUniqueUrlCount
                },
                if (progress.isScanning ||
                  progress.totalCatalogEntries > 0
                ) {
                  progress.totalCatalogEntries
                } else {
                  uiState.targetEntryCount
                },
                progress.executedUniqueProbeCount,
                progress.mappedSourceEntryCount,
              ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    item {
      Surface(
        modifier =
          Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
      ) {
        Column(
          modifier =
            Modifier
              .fillMaxWidth()
              .padding(BdixSpacing.xl),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(BdixSpacing.lg),
        ) {
          val displayedTotal = if (progress.totalCount > 0) progress.totalCount else uiState.targetEntryCount
          ScanProgressRing(
            progressFraction = progress.progressFraction,
            completedCount = progress.completedCount,
            totalCount = displayedTotal,
          )

          Text(
            text =
              if (uiState.selectedMode == ScanMode.QUICK_SCAN) {
                stringResource(R.string.dashboard_quick_scan_desc)
              } else {
                stringResource(R.string.dashboard_full_scan_desc)
              },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          if (progress.elapsedMs > 0L) {
            LtrContent {
              Text(
                text = "Elapsed: ${"%.2f".format(
                  progress.elapsedMs / 1000.0,
                )} s · ${progress.sessionState.name}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BdixSpacing.sm),
          ) {
            Button(
              onClick = onStartScan,
              enabled = !progress.isScanning && uiState.targetEntryCount > 0,
              modifier =
                Modifier
                  .weight(1f)
                  .heightIn(min = 48.dp)
                  .testTag("scan_start_button"),
            ) {
              Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
              Text(
                text = stringResource(R.string.scan_start_label, uiState.selectedMode.displayLabel),
                modifier = Modifier.padding(start = 6.dp),
              )
            }
            OutlinedButton(
              onClick = onStopScan,
              enabled = progress.isScanning,
              modifier =
                Modifier
                  .heightIn(min = 48.dp)
                  .testTag("scan_stop_button"),
            ) {
              Icon(imageVector = Icons.Default.Stop, contentDescription = null)
              Text(
                text = stringResource(R.string.action_stop_scan),
                modifier = Modifier.padding(start = 4.dp),
              )
            }
          }
        }
      }
    }

    item {
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp),
        verticalArrangement = Arrangement.spacedBy(BdixSpacing.sm),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(BdixSpacing.sm),
        ) {
          MetricTile(
            label = stringResource(R.string.scan_metric_active),
            value = progress.activeCount.toString(),
            icon = Icons.Default.Sync,
            modifier = Modifier.weight(1f),
          )
          MetricTile(
            label = stringResource(R.string.scan_metric_queued),
            value = progress.queuedCount.toString(),
            icon = Icons.Default.HourglassEmpty,
            modifier = Modifier.weight(1f),
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(BdixSpacing.sm),
        ) {
          MetricTile(
            label = stringResource(R.string.scan_metric_completed),
            value = "${progress.completedCount}/${progress.totalCount}",
            icon = Icons.Default.CheckCircle,
            modifier = Modifier.weight(1f),
          )
          MetricTile(
            label = stringResource(R.string.scan_metric_remaining),
            value = progress.remainingCount.toString(),
            icon = Icons.Default.HourglassEmpty,
            modifier = Modifier.weight(1f),
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(BdixSpacing.sm),
        ) {
          MetricTile(
            label = stringResource(R.string.scan_metric_reachable),
            value = progress.reachableCount.toString(),
            icon = Icons.Default.CheckCircle,
            accentColor = semantic.reachable,
            modifier = Modifier.weight(1f),
          )
          MetricTile(
            label = stringResource(R.string.scan_metric_error),
            value = progress.restrictedOrErrorCount.toString(),
            icon = Icons.Default.Lock,
            accentColor = semantic.restricted,
            modifier = Modifier.weight(1f),
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(BdixSpacing.sm),
        ) {
          MetricTile(
            label = stringResource(R.string.scan_metric_unreachable),
            value = progress.unreachableCount.toString(),
            icon = Icons.Default.ErrorOutline,
            accentColor = semantic.unreachable,
            modifier = Modifier.weight(1f),
          )
          MetricTile(
            label = stringResource(R.string.scan_metric_unsupported),
            value = progress.unsupportedCount.toString(),
            icon = Icons.Default.Info,
            accentColor = semantic.neutral,
            modifier = Modifier.weight(1f),
          )
        }
      }
    }

    item {
      Surface(
        modifier =
          Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
      ) {
        Column(
          modifier = Modifier.padding(BdixSpacing.lg),
          verticalArrangement = Arrangement.spacedBy(BdixSpacing.xs),
        ) {
          Text(
            text = stringResource(R.string.scan_explain_title),
            style = MaterialTheme.typography.titleSmall,
          )
          Text(
            text = stringResource(R.string.scan_explain_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun ScanProgressRing(
  progressFraction: Float,
  completedCount: Int,
  totalCount: Int,
) {
  val trackColor = MaterialTheme.colorScheme.surfaceVariant
  val activeColor = MaterialTheme.colorScheme.primary

  Box(
    modifier = Modifier.size(168.dp),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val strokePx = 12.dp.toPx()
      val arcSize = Size(size.width - strokePx, size.height - strokePx)
      val topLeft = Offset(strokePx / 2f, strokePx / 2f)

      drawArc(
        color = trackColor,
        startAngle = -90f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = strokePx, cap = StrokeCap.Round),
      )

      if (progressFraction > 0f) {
        drawArc(
          color = activeColor,
          startAngle = -90f,
          sweepAngle = 360f * progressFraction,
          useCenter = false,
          topLeft = topLeft,
          size = arcSize,
          style = Stroke(width = strokePx, cap = StrokeCap.Round),
        )
      }
    }

    LtrContent {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = "${(progressFraction * 100).toInt()}%",
          style = MonoMetricStyle,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = "$completedCount / $totalCount",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun ScanScreenPreview() {
  BdixTesterTheme {
    ScanScreen(
      uiState = ScanUiState(),
      onSelectMode = {},
      onSelectScope = {},
      onSelectScopeCategory = {},
      onStartScan = {},
      onStopScan = {},
      onExportSummary = { "" },
    )
  }
}
