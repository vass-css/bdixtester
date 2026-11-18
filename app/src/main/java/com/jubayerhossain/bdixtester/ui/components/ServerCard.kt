package com.jubayerhossain.bdixtester.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.jubayerhossain.bdixtester.core.model.CatalogSourceType
import com.jubayerhossain.bdixtester.core.model.ServerCategory
import com.jubayerhossain.bdixtester.core.model.ServerProtocol
import com.jubayerhossain.bdixtester.core.model.TestStatus
import com.jubayerhossain.bdixtester.core.model.VerificationState
import com.jubayerhossain.bdixtester.core.validation.UrlParser
import com.jubayerhossain.bdixtester.domain.model.ServerEndpoint
import com.jubayerhossain.bdixtester.domain.model.ServerTestResult
import com.jubayerhossain.bdixtester.ui.theme.BdixBorder
import com.jubayerhossain.bdixtester.ui.theme.BdixIconSize
import com.jubayerhossain.bdixtester.ui.theme.BdixSpacing
import com.jubayerhossain.bdixtester.ui.theme.BdixTesterTheme
import com.jubayerhossain.bdixtester.ui.theme.MonoBadgeStyle
import com.jubayerhossain.bdixtester.ui.theme.MonoUrlStyle
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ServerCard(
  endpoint: ServerEndpoint,
  result: ServerTestResult?,
  onVisitWebsiteClick: () -> Unit,
  modifier: Modifier = Modifier,
  isFavorite: Boolean = false,
  isPinned: Boolean = false,
  onToggleFavorite: (() -> Unit)? = null,
  onTogglePin: (() -> Unit)? = null,
  onEditCustomClick: (() -> Unit)? = null,
  onDeleteCustomClick: (() -> Unit)? = null,
) {
  val haptic = LocalHapticFeedback.current
  val cardInteractionSource = remember { MutableInteractionSource() }
  val status = result?.status ?: TestStatus.NOT_TESTED
  val urlA11y = stringResource(R.string.servers_open_url_a11y, endpoint.name)

  Card(
    modifier =
      modifier
        .fillMaxWidth()
        .springPressScale(interactionSource = cardInteractionSource, pressedScale = 0.975f)
        .testTag("server_card_${endpoint.id}"),
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    border = BorderStroke(BdixBorder.hairline, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // Card info area opens external browser directly (no in-app Server Metadata page)
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .clickable(
              interactionSource = cardInteractionSource,
              indication = ripple(),
              role = Role.Button,
              onClickLabel = urlA11y,
              onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onVisitWebsiteClick()
              },
            ).padding(
              start = BdixSpacing.lg,
              end = BdixSpacing.md,
              top = BdixSpacing.md,
              bottom = BdixSpacing.sm,
            ),
        verticalArrangement = Arrangement.spacedBy(BdixSpacing.sm),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            if (isPinned) {
              Icon(
                imageVector = Icons.Rounded.PushPin,
                contentDescription = stringResource(R.string.action_pin),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(BdixIconSize.xs),
              )
            }
            Text(
              text = endpoint.name,
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            if (endpoint.isFeatured) {
              Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.extraSmall,
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(12.dp),
                  )
                  Text(
                    text = stringResource(R.string.label_featured),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                  )
                }
              }
            }

            if (onTogglePin != null) {
              IconButton(
                onClick = {
                  haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                  onTogglePin()
                },
                modifier =
                  Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("pin_toggle_${endpoint.id}"),
              ) {
                AnimatedPinIcon(
                  isPinned = isPinned,
                  contentDescription =
                    stringResource(
                      if (isPinned) R.string.action_unpin else R.string.action_pin,
                    ),
                )
              }
            }

            if (onToggleFavorite != null) {
              IconButton(
                onClick = {
                  haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                  onToggleFavorite()
                },
                modifier =
                  Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("favorite_toggle_${endpoint.id}"),
              ) {
                AnimatedBookmarkIcon(
                  isFavorite = isFavorite,
                  contentDescription =
                    stringResource(
                      if (isFavorite) R.string.action_remove_favorite else R.string.action_add_favorite,
                    ),
                )
              }
            }
          }
        }

        val hostOrIpLabel = UrlParser.hostAndPortLabel(endpoint.host, endpoint.explicitPort)
        val isIpHost = UrlParser.isValidIpv4Host(endpoint.host)
        val hostBadgeText =
          stringResource(
            if (isIpHost) R.string.server_badge_ip else R.string.server_badge_host,
            hostOrIpLabel,
          )

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          StatusBadge(status = status)
          MetadataPill(label = endpoint.category.displayName)
          Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = MaterialTheme.shapes.extraSmall,
          ) {
            LtrContent {
              Text(
                text = hostBadgeText,
                style = MonoBadgeStyle,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
          MetadataPill(label = endpoint.protocol.displayLabel)
          if (endpoint.sourceType != CatalogSourceType.BUILT_IN) {
            MetadataPill(label = endpoint.sourceType.displayLabel)
          } else if (endpoint.sourceIndex != null) {
            LtrContent {
              MetadataPill(label = "#${endpoint.sourceIndex + 1}")
            }
          }
          if (endpoint.isPrivateIp) {
            MetadataPill(label = stringResource(R.string.label_private_ip))
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            val latency = result?.primaryLatencyMs
            val throughput = result?.downloadMbps
            if (latency != null) {
              LtrContent {
                Text(
                  text = stringResource(R.string.label_latency_ms, latency),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            if (throughput != null) {
              LtrContent {
                Text(
                  text =
                    stringResource(
                      R.string.label_throughput_mbps,
                      "%.2f".format(Locale.US, throughput),
                    ),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }
            if (latency == null && throughput == null) {
              Text(
                text = result?.failureMessage ?: stringResource(R.string.label_not_yet_tested),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            if (onEditCustomClick != null) {
              IconButton(
                onClick = onEditCustomClick,
                modifier =
                  Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("edit_custom_${endpoint.id}"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Edit,
                  contentDescription = stringResource(R.string.action_edit),
                )
              }
            }
            if (onDeleteCustomClick != null) {
              IconButton(
                onClick = onDeleteCustomClick,
                modifier =
                  Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("delete_custom_${endpoint.id}"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.DeleteOutline,
                  contentDescription = stringResource(R.string.action_delete),
                )
              }
            }
          }
        }
      }

      HorizontalDivider(
        thickness = BdixBorder.hairline,
        color = MaterialTheme.colorScheme.outlineVariant,
      )

      // Isolated full-width >= 48dp External Browser URL action row
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
                onVisitWebsiteClick()
              },
            ).semantics { contentDescription = urlA11y }
            .padding(horizontal = BdixSpacing.lg, vertical = BdixSpacing.sm)
            .testTag("visit_website_button_${endpoint.id}"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        LtrContent {
          Text(
            text = endpoint.originalUrl,
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

@Composable
private fun MetadataPill(label: String) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant,
    shape = MaterialTheme.shapes.extraSmall,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun ServerCardPreview() {
  BdixTesterTheme {
    ServerCard(
      endpoint =
        ServerEndpoint(
          id = "builtin:0",
          name = "sample-bdix.local",
          originalUrl = "http://sample-bdix.local/",
          host = "sample-bdix.local",
          explicitPort = null,
          protocol = ServerProtocol.HTTP,
          category = ServerCategory.UNCATEGORIZED,
          isFeatured = false,
          sourceType = CatalogSourceType.BUILT_IN,
          sourceIndex = 0,
          verificationState = VerificationState.UNKNOWN,
          lastReviewedAtEpochMs = null,
          speedTestUrl = null,
          isPrivateIp = false,
        ),
      result = null,
      onVisitWebsiteClick = {},
      modifier = Modifier.padding(16.dp),
    )
  }
}
