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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderShared
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jubayerhossain.bdixtester.R
import com.jubayerhossain.bdixtester.core.model.CatalogSourceType
import com.jubayerhossain.bdixtester.core.model.ServerCategory
import com.jubayerhossain.bdixtester.core.model.ServerProtocol
import com.jubayerhossain.bdixtester.core.model.TestStatus
import com.jubayerhossain.bdixtester.core.model.VerificationState
import com.jubayerhossain.bdixtester.core.util.ServerDisplayCategory
import com.jubayerhossain.bdixtester.core.util.ServerDisplayClassifier
import com.jubayerhossain.bdixtester.core.validation.UrlParser
import com.jubayerhossain.bdixtester.domain.model.ServerEndpoint
import com.jubayerhossain.bdixtester.domain.model.ServerTestResult
import com.jubayerhossain.bdixtester.ui.theme.BdixBorder
import com.jubayerhossain.bdixtester.ui.theme.BdixCardShapes
import com.jubayerhossain.bdixtester.ui.theme.BdixElevation
import com.jubayerhossain.bdixtester.ui.theme.BdixIconSize
import com.jubayerhossain.bdixtester.ui.theme.BdixSpacing
import com.jubayerhossain.bdixtester.ui.theme.BdixTesterTheme
import com.jubayerhossain.bdixtester.ui.theme.MonoBadgeStyle

/**
 * Unified 2-tier server card used across Saved Servers and custom server views:
 * - Tier 1: Category squircle, server name, Status+Latency badge & concise Host/Category pill, plus quick actions.
 * - Tier 2: Shared [RichCardUrlFooter] with direct Open pill and long-press URL copy.
 */
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
  onInspectClick: (() -> Unit)? = null,
  onLongClickCopyUrl: (() -> Unit)? = null,
  footerTestTag: String = "visit_website_button_${endpoint.id}",
) {
  val haptic = LocalHapticFeedback.current
  val cardInteractionSource = remember { MutableInteractionSource() }
  val status = result?.status ?: TestStatus.NOT_TESTED
  val urlA11y = stringResource(R.string.servers_open_url_a11y, endpoint.name)
  val categoryIcon =
    when (ServerDisplayClassifier.classify(endpoint)) {
      ServerDisplayCategory.TV -> Icons.Rounded.LiveTv
      ServerDisplayCategory.FTP -> Icons.Rounded.FolderShared
      ServerDisplayCategory.MOVIE -> Icons.Rounded.Movie
    }

  Card(
    modifier =
      modifier
        .fillMaxWidth()
        .springPressScale(interactionSource = cardInteractionSource, pressedScale = 0.975f)
        .testTag("server_card_${endpoint.id}"),
    shape = BdixCardShapes.richCard,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    elevation = CardDefaults.cardElevation(defaultElevation = BdixElevation.card),
    border = BorderStroke(BdixBorder.hairline, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // Tier 1: Essential Server Summary & Actions
      Row(
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
              end = BdixSpacing.xs,
              top = BdixSpacing.md,
              bottom = BdixSpacing.md,
            ),
        horizontalArrangement = Arrangement.spacedBy(BdixSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        BdixCategoryIconBadge(icon = categoryIcon)

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Row(
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
            if (endpoint.isFeatured) {
              Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = stringResource(R.string.label_featured),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(BdixIconSize.xs),
              )
            }
            Text(
              text = endpoint.name,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false),
            )
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
            StatusBadge(status = status, latencyMs = result?.primaryLatencyMs)

            Surface(
              color = MaterialTheme.colorScheme.secondaryContainer,
              shape = BdixCardShapes.chip,
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

            Surface(
              color = MaterialTheme.colorScheme.surfaceContainerHigh,
              shape = BdixCardShapes.chip,
            ) {
              Text(
                text = ServerDisplayClassifier.displayLabel(endpoint),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }

        Column(
          horizontalAlignment = Alignment.End,
          verticalArrangement = Arrangement.Center,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
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

            if (onInspectClick != null) {
              IconButton(
                onClick = {
                  haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                  onInspectClick()
                },
                modifier =
                  Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("inspect_server_${endpoint.id}"),
              ) {
                Icon(
                  imageVector = Icons.Outlined.Info,
                  contentDescription = stringResource(R.string.action_inspect_server, endpoint.name),
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }

          if (onEditCustomClick != null || onDeleteCustomClick != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    tint = MaterialTheme.colorScheme.error,
                  )
                }
              }
            }
          }
        }
      }

      // Tier 2: Shared Monospace URL & Open Footer (long-press copies URL)
      RichCardUrlFooter(
        url = endpoint.originalUrl,
        urlA11y = urlA11y,
        testTag = footerTestTag,
        onClick = onVisitWebsiteClick,
        onLongClick = onLongClickCopyUrl,
      )
    }
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
          category = ServerCategory.MOVIES,
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
