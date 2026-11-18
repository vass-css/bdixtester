package com.jubayerhossain.bdixtester.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jubayerhossain.bdixtester.R
import com.jubayerhossain.bdixtester.ui.theme.BdixBorder
import com.jubayerhossain.bdixtester.ui.theme.BdixCardShapes
import com.jubayerhossain.bdixtester.ui.theme.BdixSpacing
import com.jubayerhossain.bdixtester.ui.theme.MonoUrlStyle

/**
 * Shared 12dp squircle icon badge used consistently across Servers, OTT, Saved, and History cards.
 */
@Composable
fun BdixCategoryIconBadge(
  icon: ImageVector,
  modifier: Modifier = Modifier,
  size: Dp = 44.dp,
  iconSize: Dp = 22.dp,
) {
  Surface(
    shape = BdixCardShapes.iconSquircle,
    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f),
    modifier = modifier.size(size),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(iconSize),
      )
    }
  }
}

/**
 * Shared elevated rich-card footer strip with LTR monospace URL and Open pill.
 * Tap always launches external browser directly; optional [onLongClick] copies URL or opens quick actions.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RichCardUrlFooter(
  url: String,
  urlA11y: String,
  testTag: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  onLongClick: (() -> Unit)? = null,
) {
  val haptic = LocalHapticFeedback.current
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainer,
    modifier =
      modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .combinedClickable(
          role = Role.Button,
          onClickLabel = urlA11y,
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
          },
          onLongClick =
            onLongClick?.let { longAction ->
              {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                longAction()
              }
            },
        ).semantics { contentDescription = urlA11y }
        .testTag(testTag),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      HorizontalDivider(
        thickness = BdixBorder.hairline,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
      )
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(horizontal = BdixSpacing.lg, vertical = BdixSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        LtrContent {
          Text(
            text = url,
            style = MonoUrlStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier =
              Modifier
                .weight(1f, fill = false)
                .padding(end = BdixSpacing.md),
          )
        }
        Surface(
          shape = BdixCardShapes.pill,
          color = MaterialTheme.colorScheme.primaryContainer,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = stringResource(R.string.action_open),
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              maxLines = 1,
            )
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(13.dp),
            )
          }
        }
      }
    }
  }
}
