package com.jubayerhossain.bdixtester.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FolderShared
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jubayerhossain.bdixtester.R
import com.jubayerhossain.bdixtester.core.model.CatalogSourceType
import com.jubayerhossain.bdixtester.core.model.TestStatus
import com.jubayerhossain.bdixtester.core.util.ServerDisplayCategory
import com.jubayerhossain.bdixtester.core.util.ServerDisplayClassifier
import com.jubayerhossain.bdixtester.core.validation.UrlParser
import com.jubayerhossain.bdixtester.domain.model.ServerEndpoint
import com.jubayerhossain.bdixtester.domain.model.ServerTestResult
import com.jubayerhossain.bdixtester.ui.theme.BdixBorder
import com.jubayerhossain.bdixtester.ui.theme.BdixCardShapes
import com.jubayerhossain.bdixtester.ui.theme.BdixIconSize
import com.jubayerhossain.bdixtester.ui.theme.BdixSpacing
import com.jubayerhossain.bdixtester.ui.theme.LocalBdixSemanticColors
import com.jubayerhossain.bdixtester.ui.theme.MonoUrlStyle
import java.util.Locale

/**
 * Lightweight Quick-Inspect ModalBottomSheet that surfaces secondary technical metadata
 * (protocol, source index, private IP, stage timings, copy/open actions) without cluttering
 * the 2-tier server card.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ServerDetailBottomSheet(
  endpoint: ServerEndpoint,
  result: ServerTestResult?,
  onDismiss: () -> Unit,
  onOpenUrl: () -> Unit,
  onCopyUrl: () -> Unit,
  onTestNow: (() -> Unit)? = null,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val semantic = LocalBdixSemanticColors.current
  val status = result?.status ?: TestStatus.NOT_TESTED
  val categoryIcon =
    when (ServerDisplayClassifier.classify(endpoint)) {
      ServerDisplayCategory.TV -> Icons.Rounded.LiveTv
      ServerDisplayCategory.FTP -> Icons.Rounded.FolderShared
      ServerDisplayCategory.MOVIE -> Icons.Rounded.Movie
    }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = semantic.bottomSheetBackground,
    modifier = Modifier.testTag("server_quick_inspect_sheet"),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = BdixSpacing.lg, vertical = BdixSpacing.sm),
      verticalArrangement = Arrangement.spacedBy(BdixSpacing.md),
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BdixSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        BdixCategoryIconBadge(icon = categoryIcon)
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = endpoint.name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
          StatusBadge(status = status, latencyMs = result?.primaryLatencyMs)
        }
      }

      // Monospace URL container
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = BdixCardShapes.input,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(BdixBorder.hairline, MaterialTheme.colorScheme.outlineVariant),
      ) {
        LtrContent {
          Text(
            text = endpoint.originalUrl,
            style = MonoUrlStyle,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(BdixSpacing.md),
          )
        }
      }

      // Secondary Technical Metadata Grid
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = BdixCardShapes.richCard,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(BdixBorder.hairline, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(
          modifier =
            Modifier
              .fillMaxWidth()
              .padding(BdixSpacing.md),
          verticalArrangement = Arrangement.spacedBy(BdixSpacing.sm),
        ) {
          Text(
            text = stringResource(R.string.servers_diagnostics_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
          )

          DetailKeyValueRow(
            label = stringResource(R.string.details_host_label),
            value = UrlParser.hostAndPortLabel(endpoint.host, endpoint.explicitPort),
            isMonospace = true,
          )
          DetailKeyValueRow(
            label = stringResource(R.string.details_protocol_label),
            value = endpoint.protocol.displayLabel,
          )
          DetailKeyValueRow(
            label = stringResource(R.string.details_category_label),
            value = ServerDisplayClassifier.displayLabel(endpoint),
          )
          val sourceLabel =
            if (endpoint.sourceType == CatalogSourceType.BUILT_IN && endpoint.sourceIndex != null) {
              "${endpoint.sourceType.displayLabel} #${endpoint.sourceIndex + 1}"
            } else {
              endpoint.sourceType.displayLabel
            }
          DetailKeyValueRow(
            label = stringResource(R.string.details_source_label),
            value = sourceLabel,
          )
          result?.primaryLatencyMs?.let { ms ->
            DetailKeyValueRow(
              label = stringResource(R.string.details_latency_label),
              value = stringResource(R.string.servers_latency_chip, ms),
              isMonospace = true,
            )
          }
          result?.downloadMbps?.let { mbps ->
            DetailKeyValueRow(
              label = stringResource(R.string.details_throughput_label),
              value = "${"%.2f".format(Locale.US, mbps)} Mbps",
              isMonospace = true,
            )
          }
          result?.httpStatusCode?.let { code ->
            DetailKeyValueRow(
              label = stringResource(R.string.diagnostics_http_status),
              value = code.toString(),
              isMonospace = true,
            )
          }
          result?.ftpGreetingCode?.let { code ->
            DetailKeyValueRow(
              label = stringResource(R.string.diagnostics_ftp_code),
              value = code.toString(),
              isMonospace = true,
            )
          }

          val hasBadges = endpoint.isPrivateIp || endpoint.isFeatured
          if (hasBadges) {
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(BdixSpacing.sm),
              verticalArrangement = Arrangement.spacedBy(BdixSpacing.xs),
            ) {
              if (endpoint.isPrivateIp) {
                Surface(
                  color = MaterialTheme.colorScheme.surfaceContainerHigh,
                  shape = BdixCardShapes.chip,
                ) {
                  Text(
                    text = stringResource(R.string.label_private_ip),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  )
                }
              }
              if (endpoint.isFeatured) {
                Surface(
                  color = MaterialTheme.colorScheme.primaryContainer,
                  shape = BdixCardShapes.chip,
                ) {
                  Text(
                    text = stringResource(R.string.label_featured),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  )
                }
              }
            }
          }

          if (!result?.failureMessage.isNullOrBlank()) {
            HorizontalDivider(
              thickness = BdixBorder.hairline,
              color = MaterialTheme.colorScheme.outlineVariant,
            )
            Text(
              text = result.failureMessage.orEmpty(),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      // Action Buttons Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BdixSpacing.sm),
      ) {
        FilledTonalButton(
          onClick = {
            onCopyUrl()
            onDismiss()
          },
          modifier =
            Modifier
              .weight(1f)
              .heightIn(min = 48.dp)
              .testTag("sheet_copy_url_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.ContentCopy,
            contentDescription = null,
            modifier = Modifier.size(BdixIconSize.sm),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = stringResource(R.string.servers_copy_url),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Button(
          onClick = {
            onDismiss()
            onOpenUrl()
          },
          modifier =
            Modifier
              .weight(1f)
              .heightIn(min = 48.dp)
              .testTag("sheet_open_url_button"),
        ) {
          Text(
            text = stringResource(R.string.action_open),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.width(6.dp))
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
            contentDescription = null,
            modifier = Modifier.size(BdixIconSize.sm),
          )
        }
      }

      if (onTestNow != null) {
        FilledTonalButton(
          onClick = {
            onTestNow()
            onDismiss()
          },
          modifier =
            Modifier
              .fillMaxWidth()
              .heightIn(min = 48.dp),
        ) {
          Text(stringResource(R.string.action_test_server))
        }
      }

      Spacer(modifier = Modifier.height(BdixSpacing.xs))
    }
  }
}

@Composable
private fun DetailKeyValueRow(
  label: String,
  value: String,
  isMonospace: Boolean = false,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(end = BdixSpacing.md),
    )
    if (isMonospace) {
      LtrContent {
        Text(
          text = value,
          style = MonoUrlStyle,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    } else {
      Text(
        text = value,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
