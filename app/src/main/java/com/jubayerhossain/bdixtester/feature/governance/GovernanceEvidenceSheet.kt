package com.jubayerhossain.bdixtester.feature.governance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jubayerhossain.bdixtester.R
import com.jubayerhossain.bdixtester.core.model.ConfidenceLevel
import com.jubayerhossain.bdixtester.core.model.EndpointGovernanceRecord
import com.jubayerhossain.bdixtester.core.model.VerificationEventRecord
import com.jubayerhossain.bdixtester.core.model.VerificationLifecycleStatus
import com.jubayerhossain.bdixtester.domain.model.ServerEndpoint
import com.jubayerhossain.bdixtester.ui.components.LtrContent
import com.jubayerhossain.bdixtester.ui.theme.BdixBorder
import com.jubayerhossain.bdixtester.ui.theme.BdixCardShapes
import com.jubayerhossain.bdixtester.ui.theme.BdixSpacing
import com.jubayerhossain.bdixtester.ui.theme.LocalBdixSemanticColors
import com.jubayerhossain.bdixtester.ui.theme.MonoUrlStyle
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GovernanceEvidenceSheet(
  endpoint: ServerEndpoint,
  record: EndpointGovernanceRecord,
  events: List<VerificationEventRecord>,
  onDismiss: () -> Unit,
  onVerifySingle: (String) -> Unit,
  onMarkCandidate: (String) -> Unit,
  onArchive: (String) -> Unit,
  onRestore: (String) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val semantic = LocalBdixSemanticColors.current
  var showArchiveConfirmDialog by remember { mutableStateOf(false) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = semantic.bottomSheetBackground,
    modifier = Modifier.testTag("governance_evidence_sheet"),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = BdixSpacing.lg, vertical = BdixSpacing.sm)
          .padding(bottom = BdixSpacing.xl),
      verticalArrangement = Arrangement.spacedBy(BdixSpacing.md),
    ) {
      // Header
      Text(
        text = stringResource(R.string.governance_evidence_sheet_title),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
      )
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = endpoint.name,
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurface,
        )
        LtrContent {
          Text(
            text = endpoint.originalUrl,
            style = MonoUrlStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // Status & Confidence badges
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(BdixSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(BdixSpacing.xs),
      ) {
        LifecycleStatusPill(status = record.lifecycleStatus)
        ConfidencePill(confidence = record.confidence)
        record.normalizedDomain?.let { domain ->
          Surface(
            shape = BdixCardShapes.chip,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
          ) {
            LtrContent {
              Text(
                text = domain,
                style = MonoUrlStyle,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              )
            }
          }
        }
      }

      // Section 1: Decision & Confidence
      EvidenceSectionCard(
        title = stringResource(R.string.governance_section_summary),
        modifier = Modifier.testTag("governance_section_summary"),
      ) {
        Text(
          text = record.reason,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = record.recommendedAction,
          style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
          color = MaterialTheme.colorScheme.primary,
        )
        record.lastVerifiedAtEpochMs?.let { ts ->
          val formatted = remember(ts) { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(ts)) }
          Text(
            text = stringResource(R.string.settings_last_synced_at, formatted),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // Section 2: Multi-Resolver DNS
      EvidenceSectionCard(
        title = stringResource(R.string.governance_section_dns),
        modifier = Modifier.testTag("governance_section_dns"),
      ) {
        Text(
          text = "Summary: ${record.dnsSummary.wireName}",
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
          color = MaterialTheme.colorScheme.onSurface,
        )
        val dns = record.dns
        if (dns != null && dns.resolverResults.isNotEmpty()) {
          dns.resolverResults.forEach { probe ->
            LtrContent {
              val answerStr = if (probe.answers.isEmpty()) "[]" else probe.answers.joinToString(", ")
              val errStr = probe.error?.let { " · err=$it" } ?: ""
              Text(
                text = "• ${probe.resolverName} (${probe.recordType}): ${probe.rcode} $answerStr$errStr",
                style = MonoUrlStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          if (dns.nsRecords.isNotEmpty()) {
            LtrContent {
              Text(
                text = "NS: ${dns.nsRecords.joinToString(", ")}",
                style = MonoUrlStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          if (dns.soaRecords.isNotEmpty()) {
            LtrContent {
              Text(
                text = "SOA: ${dns.soaRecords.joinToString(", ")}",
                style = MonoUrlStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        } else {
          Text(
            text = stringResource(R.string.diagnostics_not_available),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // Section 3: Domain Registration (RDAP)
      EvidenceSectionCard(
        title = stringResource(R.string.governance_section_registration),
        modifier = Modifier.testTag("governance_section_registration"),
      ) {
        val reg = record.registration
        Text(
          text = "Status: ${record.registrationStatus.wireName}",
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
          color = MaterialTheme.colorScheme.onSurface,
        )
        reg?.registrar?.let {
          Text(
            text = "Registrar: $it",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        reg?.expiresAtIso?.let {
          LtrContent {
            Text(
              text = "Expires: $it",
              style = MonoUrlStyle,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        if (reg != null && reg.eppStatuses.isNotEmpty()) {
          LtrContent {
            Text(
              text = "EPP: ${reg.eppStatuses.joinToString(", ")}",
              style = MonoUrlStyle,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        reg?.limitationNote?.let { note ->
          Text(
            text = note,
            style = MaterialTheme.typography.bodySmall,
            color = semantic.restricted,
          )
        }
      }

      // Section 4: HTTP / FTP Probe
      EvidenceSectionCard(
        title = stringResource(R.string.governance_section_http_ftp),
        modifier = Modifier.testTag("governance_section_http_ftp"),
      ) {
        val http = record.http
        val ftp = record.ftp
        when {
          http != null -> {
            Text(
              text = "HTTP: ${http.outcome}" + (http.statusCode?.let { " ($it)" } ?: ""),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            http.finalUrl?.let {
              LtrContent {
                Text(
                  text = "Final URL: $it",
                  style = MonoUrlStyle,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            http.errorDetail?.let {
              Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          ftp != null -> {
            Text(
              text = "FTP: ${ftp.outcome}" + (ftp.greetingCode?.let { " ($it)" } ?: ""),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            ftp.errorDetail?.let {
              Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          else -> {
            Text(
              text = stringResource(R.string.diagnostics_not_available),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      // Section 5: Bangladesh ISP Probes
      EvidenceSectionCard(
        title = stringResource(R.string.governance_section_isp_probes),
        modifier = Modifier.testTag("governance_section_isp_probes"),
      ) {
        if (record.ispProbes.isEmpty()) {
          Text(
            text = stringResource(R.string.governance_no_isp_probes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        } else {
          record.ispProbes.forEach { probe ->
            val staleSuffix = if (probe.isStale) " [STALE]" else ""
            Text(
              text = "• ${probe.ispName} (${probe.region}): ${probe.connectionResult} / ${probe.httpOrFtpResult}$staleSuffix",
              style = MaterialTheme.typography.bodySmall,
              color = if (probe.isStale) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }

      // Section 6: Missing Evidence & Limitations
      if (record.missingEvidence.isNotEmpty()) {
        EvidenceSectionCard(
          title = stringResource(R.string.governance_section_missing),
          modifier = Modifier.testTag("governance_section_missing"),
        ) {
          record.missingEvidence.forEach { gap ->
            Text(
              text = "• $gap",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      // Section 7: Audit Timeline
      if (events.isNotEmpty()) {
        EvidenceSectionCard(
          title = stringResource(R.string.governance_section_history),
          modifier = Modifier.testTag("governance_section_history"),
        ) {
          events.take(8).forEachIndexed { index, ev ->
            if (index > 0) {
              HorizontalDivider(
                thickness = BdixBorder.hairline,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
              )
            }
            Text(
              text = "${ev.previousStatus.displayLabel} → ${ev.newStatus.displayLabel}",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "${ev.triggerSource}: ${ev.summary}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      // Action Buttons (all >= 48.dp touch target)
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BdixSpacing.sm),
      ) {
        if (record.lifecycleStatus == VerificationLifecycleStatus.ARCHIVED) {
          Button(
            onClick = { onRestore(endpoint.id) },
            colors =
              ButtonDefaults.buttonColors(
                containerColor = semantic.reachable,
                contentColor = semantic.onReachable,
              ),
            modifier =
              Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("governance_sheet_restore_button"),
          ) {
            Icon(imageVector = Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(BdixSpacing.sm))
            Text(stringResource(R.string.governance_action_restore))
          }
        } else {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BdixSpacing.sm),
          ) {
            OutlinedButton(
              onClick = { onVerifySingle(endpoint.id) },
              modifier =
                Modifier
                  .weight(1f)
                  .heightIn(min = 48.dp)
                  .testTag("governance_sheet_verify_button"),
            ) {
              Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(BdixSpacing.xs))
              Text(stringResource(R.string.governance_action_verify_one), maxLines = 1)
            }
            if (record.lifecycleStatus != VerificationLifecycleStatus.REMOVAL_CANDIDATE) {
              OutlinedButton(
                onClick = { onMarkCandidate(endpoint.id) },
                modifier =
                  Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("governance_sheet_candidate_button"),
              ) {
                Icon(imageVector = Icons.Rounded.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(BdixSpacing.xs))
                Text(stringResource(R.string.governance_action_mark_candidate), maxLines = 1)
              }
            }
          }
          Button(
            onClick = { showArchiveConfirmDialog = true },
            colors =
              ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
              ),
            modifier =
              Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("governance_sheet_archive_button"),
          ) {
            Icon(imageVector = Icons.Rounded.Archive, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(BdixSpacing.sm))
            Text(stringResource(R.string.governance_action_archive))
          }
        }
      }
    }
  }

  if (showArchiveConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showArchiveConfirmDialog = false },
      containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
      modifier = Modifier.testTag("governance_archive_confirm_dialog"),
      title = {
        Text(
          text = stringResource(R.string.governance_archive_dialog_title, endpoint.name),
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
      },
      text = {
        Text(
          text = stringResource(R.string.governance_archive_dialog_body),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showArchiveConfirmDialog = false
            onArchive(endpoint.id)
          },
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.error,
              contentColor = MaterialTheme.colorScheme.onError,
            ),
          modifier =
            Modifier
              .heightIn(min = 48.dp)
              .testTag("governance_archive_confirm_button"),
        ) {
          Text(stringResource(R.string.governance_action_archive))
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showArchiveConfirmDialog = false },
          modifier = Modifier.heightIn(min = 48.dp),
        ) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    )
  }
}

@Composable
private fun EvidenceSectionCard(
  title: String,
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = BdixCardShapes.richCard,
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(BdixBorder.hairline, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(
      modifier = Modifier.padding(BdixSpacing.md),
      verticalArrangement = Arrangement.spacedBy(BdixSpacing.xs),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
      )
      content()
    }
  }
}

@Composable
fun LifecycleStatusPill(
  status: VerificationLifecycleStatus,
  modifier: Modifier = Modifier,
) {
  val semantic = LocalBdixSemanticColors.current
  val (container, content) =
    when (status) {
      VerificationLifecycleStatus.ACTIVE,
      VerificationLifecycleStatus.REDIRECTED,
      -> semantic.reachableContainer to semantic.onReachableContainer
      VerificationLifecycleStatus.ISP_RESTRICTED ->
        semantic.informationContainer to semantic.onInformationContainer
      VerificationLifecycleStatus.UNREACHABLE_FROM_CURRENT_ISP,
      VerificationLifecycleStatus.TEMPORARILY_UNAVAILABLE,
      VerificationLifecycleStatus.DNS_TEMPORARY_FAILURE,
      VerificationLifecycleStatus.DNS_NXDOMAIN_SUSPECTED,
      VerificationLifecycleStatus.DOMAIN_EXPIRED_SUSPECTED,
      -> semantic.restrictedContainer to semantic.onRestrictedContainer
      VerificationLifecycleStatus.REMOVAL_CANDIDATE,
      VerificationLifecycleStatus.DOMAIN_EXPIRED_CONFIRMED,
      VerificationLifecycleStatus.PERMANENTLY_REMOVED,
      -> semantic.unreachableContainer to semantic.onUnreachableContainer
      VerificationLifecycleStatus.ARCHIVED,
      VerificationLifecycleStatus.UNKNOWN,
      VerificationLifecycleStatus.PENDING_VERIFICATION,
      VerificationLifecycleStatus.UNSUPPORTED_PROTOCOL,
      -> semantic.neutralContainer to semantic.onNeutralContainer
    }
  GovernanceBadgePill(label = status.displayLabel, containerColor = container, contentColor = content, modifier = modifier)
}

@Composable
fun ConfidencePill(
  confidence: ConfidenceLevel,
  modifier: Modifier = Modifier,
) {
  val semantic = LocalBdixSemanticColors.current
  val (container, content) =
    when (confidence) {
      ConfidenceLevel.HIGH -> semantic.informationContainer to semantic.onInformationContainer
      ConfidenceLevel.MEDIUM -> semantic.restrictedContainer to semantic.onRestrictedContainer
      ConfidenceLevel.LOW, ConfidenceLevel.NONE -> semantic.neutralContainer to semantic.onNeutralContainer
    }
  GovernanceBadgePill(
    label = "${confidence.displayLabel} conf.",
    containerColor = container,
    contentColor = content,
    modifier = modifier,
  )
}

@Composable
private fun GovernanceBadgePill(
  label: String,
  containerColor: Color,
  contentColor: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(999.dp),
    color = containerColor,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
      color = contentColor,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}
