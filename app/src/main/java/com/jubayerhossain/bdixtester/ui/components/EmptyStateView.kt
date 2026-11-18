package com.jubayerhossain.bdixtester.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jubayerhossain.bdixtester.ui.theme.BdixSpacing

@Composable
fun EmptyStateView(
  icon: ImageVector,
  title: String,
  description: String,
  modifier: Modifier = Modifier,
  actionLabel: String? = null,
  onActionClick: (() -> Unit)? = null,
  tag: String = "empty_state_view",
) {
  Surface(
    modifier =
      modifier
        .fillMaxWidth()
        .testTag(tag),
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(horizontal = BdixSpacing.xl, vertical = BdixSpacing.xxl),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      FloatingMotionBox {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
          modifier = Modifier.size(76.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(38.dp),
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(BdixSpacing.md))
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(BdixSpacing.xs))
      Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      if (actionLabel != null && onActionClick != null) {
        Spacer(modifier = Modifier.height(BdixSpacing.lg))
        val interactionSource = remember { MutableInteractionSource() }
        FilledTonalButton(
          onClick = onActionClick,
          interactionSource = interactionSource,
          modifier =
            Modifier
              .springPressScale(interactionSource = interactionSource, pressedScale = 0.95f)
              .heightIn(min = 48.dp)
              .testTag("${tag}_action"),
        ) {
          Text(actionLabel)
        }
      }
    }
  }
}
