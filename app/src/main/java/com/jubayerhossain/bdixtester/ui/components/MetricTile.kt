package com.jubayerhossain.bdixtester.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jubayerhossain.bdixtester.ui.theme.BdixSpacing
import com.jubayerhossain.bdixtester.ui.theme.MonoMetricStyle

@Composable
fun MetricTile(
  label: String,
  value: String,
  icon: ImageVector,
  modifier: Modifier = Modifier,
  accentColor: Color = MaterialTheme.colorScheme.primary,
) {
  Surface(
    modifier = modifier,
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
    tonalElevation = 1.dp,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(BdixSpacing.md),
      verticalArrangement = Arrangement.spacedBy(BdixSpacing.xs),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = accentColor,
          modifier = Modifier.size(16.dp),
        )
        Text(
          text = label,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Text(
        text = value,
        style = MonoMetricStyle,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
