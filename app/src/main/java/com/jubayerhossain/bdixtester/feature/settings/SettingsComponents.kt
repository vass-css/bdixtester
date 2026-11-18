package com.jubayerhossain.bdixtester.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jubayerhossain.bdixtester.R
import com.jubayerhossain.bdixtester.ui.components.LtrContent
import com.jubayerhossain.bdixtester.ui.components.springPressScale
import com.jubayerhossain.bdixtester.ui.theme.BdixBorder
import com.jubayerhossain.bdixtester.ui.theme.BdixCardShapes
import com.jubayerhossain.bdixtester.ui.theme.BdixSpacing
import com.jubayerhossain.bdixtester.ui.theme.MonoUrlStyle

@Composable
internal fun SettingsSectionHeader(
  title: String,
  modifier: Modifier = Modifier,
) {
  Text(
    text = title,
    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    color = MaterialTheme.colorScheme.primary,
    modifier =
      modifier
        .fillMaxWidth()
        .widthIn(max = 720.dp)
        .padding(start = BdixSpacing.sm, end = BdixSpacing.sm, top = BdixSpacing.sm, bottom = BdixSpacing.xs),
  )
}

@Composable
internal fun SettingsGroupCard(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  Surface(
    modifier =
      modifier
        .fillMaxWidth()
        .widthIn(max = 720.dp),
    shape = BdixCardShapes.richCard,
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(BdixBorder.hairline, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      content = content,
    )
  }
}

@Composable
internal fun SettingsRowDivider() {
  HorizontalDivider(
    modifier = Modifier.padding(start = 68.dp, end = BdixSpacing.lg),
    thickness = BdixBorder.hairline,
    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
  )
}

@Composable
internal fun SettingsIconBadge(
  modifier: Modifier = Modifier,
  icon: ImageVector,
  isDestructive: Boolean = false,
) {
  val containerColor =
    if (isDestructive) {
      MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.75f)
    } else {
      MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
    }
  val iconTint =
    if (isDestructive) {
      MaterialTheme.colorScheme.error
    } else {
      MaterialTheme.colorScheme.primary
    }

  Surface(
    modifier = modifier.size(36.dp),
    shape = RoundedCornerShape(10.dp),
    color = containerColor,
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = iconTint,
        modifier = Modifier.size(20.dp),
      )
    }
  }
}

@Composable
internal fun SettingsNavigationRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier,
  isDestructive: Boolean = false,
  subtitleMonospace: Boolean = false,
) {
  val haptic = LocalHapticFeedback.current
  val interactionSource = remember { MutableInteractionSource() }
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .springPressScale(interactionSource = interactionSource, pressedScale = 0.98f)
        .heightIn(min = 64.dp)
        .clickable(
          interactionSource = interactionSource,
          indication = ripple(),
          role = Role.Button,
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
          },
        ).padding(horizontal = BdixSpacing.lg, vertical = BdixSpacing.md)
        .testTag(testTag),
    horizontalArrangement = Arrangement.spacedBy(BdixSpacing.md),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    SettingsIconBadge(icon = icon, isDestructive = isDestructive)

    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
        color =
          if (isDestructive) {
            MaterialTheme.colorScheme.error
          } else {
            MaterialTheme.colorScheme.onSurface
          },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      if (subtitleMonospace) {
        LtrContent {
          Text(
            text = subtitle,
            style = MonoUrlStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      } else {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    Icon(
      imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
      contentDescription = null,
      tint =
        if (isDestructive) {
          MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
        } else {
          MaterialTheme.colorScheme.onSurfaceVariant
        },
      modifier = Modifier.size(20.dp),
    )
  }
}

@Composable
internal fun SettingsSwitchRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String,
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current
  val interactionSource = remember { MutableInteractionSource() }
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .springPressScale(interactionSource = interactionSource, pressedScale = 0.985f)
        .heightIn(min = 64.dp)
        .clickable(
          interactionSource = interactionSource,
          indication = ripple(),
          role = Role.Switch,
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onCheckedChange(!checked)
          },
        ).padding(horizontal = BdixSpacing.lg, vertical = BdixSpacing.md),
    horizontalArrangement = Arrangement.spacedBy(BdixSpacing.md),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    SettingsIconBadge(icon = icon)

    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }

    Switch(
      checked = checked,
      onCheckedChange = {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onCheckedChange(it)
      },
      colors =
        SwitchDefaults.colors(
          checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
          checkedTrackColor = MaterialTheme.colorScheme.primary,
          uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
          uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
          uncheckedBorderColor = MaterialTheme.colorScheme.outline,
        ),
      modifier = Modifier.testTag(testTag),
    )
  }
}

@Composable
internal fun SettingsInfoRow(
  icon: ImageVector,
  title: String,
  value: String,
  modifier: Modifier = Modifier,
  valueMonospace: Boolean = false,
) {
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .heightIn(min = 56.dp)
        .padding(horizontal = BdixSpacing.lg, vertical = BdixSpacing.md),
    horizontalArrangement = Arrangement.spacedBy(BdixSpacing.md),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    SettingsIconBadge(icon = icon)
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      if (valueMonospace) {
        LtrContent {
          Text(
            text = value,
            style = MonoUrlStyle,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
      } else {
        Text(
          text = value,
          style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

internal data class RadioPickerOption<T>(
  val value: T,
  val label: String,
  val description: String? = null,
  val testTag: String,
)

@Composable
internal fun <T> SettingsRadioPickerDialog(
  title: String,
  options: List<RadioPickerOption<T>>,
  selectedValue: T,
  onSelect: (T) -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    title = {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { option ->
          val selected = option.value == selectedValue
          Surface(
            shape = BdixCardShapes.iconSquircle,
            color =
              if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
              } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
              },
            modifier =
              Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clickable(role = Role.RadioButton) {
                  onSelect(option.value)
                  onDismiss()
                }.testTag(option.testTag),
          ) {
            Row(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .padding(horizontal = BdixSpacing.md, vertical = BdixSpacing.sm),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(BdixSpacing.md),
            ) {
              RadioButton(
                selected = selected,
                onClick = null,
                colors =
                  RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                  ),
              )
              Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
              ) {
                Text(
                  text = option.label,
                  style =
                    MaterialTheme.typography.bodyLarge.copy(
                      fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                  color = MaterialTheme.colorScheme.onSurface,
                )
                if (!option.description.isNullOrBlank()) {
                  Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.heightIn(min = 48.dp),
      ) {
        Text(
          text = stringResource(R.string.action_cancel),
          color = MaterialTheme.colorScheme.primary,
        )
      }
    },
  )
}

@Composable
internal fun SettingsTextInputDialog(
  title: String,
  initialValue: String,
  placeholder: String,
  inputTestTag: String,
  saveTestTag: String,
  isLtr: Boolean = false,
  onSave: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  var draft by remember(initialValue) { mutableStateOf(initialValue) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    title = {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
    },
    text = {
      val field: @Composable () -> Unit = {
        OutlinedTextField(
          value = draft,
          onValueChange = { draft = it },
          placeholder = { Text(placeholder) },
          singleLine = true,
          trailingIcon = {
            if (draft.isNotEmpty()) {
              IconButton(
                onClick = { draft = "" },
                modifier = Modifier.minimumInteractiveComponentSize(),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Clear,
                  contentDescription = stringResource(R.string.action_clear_filters),
                )
              }
            }
          },
          shape = BdixCardShapes.input,
          modifier =
            Modifier
              .fillMaxWidth()
              .testTag(inputTestTag),
        )
      }
      if (isLtr) {
        LtrContent { field() }
      } else {
        field()
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(draft)
          onDismiss()
        },
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
        modifier =
          Modifier
            .heightIn(min = 48.dp)
            .testTag(saveTestTag),
      ) {
        Text(stringResource(R.string.action_save))
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.heightIn(min = 48.dp),
      ) {
        Text(
          text = stringResource(R.string.action_cancel),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    },
  )
}
