package com.jubayerhossain.bdixtester.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jubayerhossain.bdixtester.R
import com.jubayerhossain.bdixtester.core.model.AppThemePreset
import com.jubayerhossain.bdixtester.ui.components.springPressScale
import com.jubayerhossain.bdixtester.ui.theme.BdixBorder
import com.jubayerhossain.bdixtester.ui.theme.BdixSpacing
import com.jubayerhossain.bdixtester.ui.theme.BdixThemeRegistry

@Composable
internal fun ThemePresetSelectorColumn(
  selectedPreset: AppThemePreset,
  previewDarkMode: Boolean,
  onSelectPreset: (AppThemePreset) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .widthIn(max = 720.dp)
        .testTag("settings_theme_preset_list"),
    verticalArrangement = Arrangement.spacedBy(BdixSpacing.md),
  ) {
    AppThemePreset.entries.forEach { preset ->
      ThemePresetPreviewCard(
        preset = preset,
        isSelected = preset == selectedPreset,
        previewDarkMode = previewDarkMode,
        onClick = { onSelectPreset(preset) },
      )
    }
  }
}

@Composable
internal fun ThemePresetPreviewCard(
  preset: AppThemePreset,
  isSelected: Boolean,
  previewDarkMode: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current
  val interactionSource = remember { MutableInteractionSource() }
  val previewSpec =
    remember(preset, previewDarkMode) {
      BdixThemeRegistry.specFor(preset = preset, isDark = previewDarkMode)
    }
  val previewScheme = previewSpec.colorScheme
  val previewSemantic = previewSpec.semantic

  val title = presetTitle(preset)
  val subtitle = presetDescription(preset)
  val a11yLabel = stringResource(R.string.theme_preview_card_a11y, title, subtitle)

  Surface(
    modifier =
      modifier
        .fillMaxWidth()
        .springPressScale(interactionSource = interactionSource, pressedScale = 0.985f)
        .clip(RoundedCornerShape(20.dp))
        .clickable(
          interactionSource = interactionSource,
          indication = ripple(),
          role = Role.RadioButton,
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
          },
        ).semantics {
          selected = isSelected
          contentDescription = a11yLabel
        }.testTag("theme_preset_card_${preset.name.lowercase()}"),
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border =
      BorderStroke(
        width = if (isSelected) 2.dp else BdixBorder.hairline,
        color =
          if (isSelected) {
            MaterialTheme.colorScheme.primary
          } else {
            MaterialTheme.colorScheme.outlineVariant
          },
      ),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(BdixSpacing.md),
      verticalArrangement = Arrangement.spacedBy(BdixSpacing.md),
    ) {
      // Card Header: Preset Name, Color Swatches & Selection Status
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Spacer(modifier = Modifier.width(BdixSpacing.sm))

        if (isSelected) {
          Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp),
              )
              Text(
                text = stringResource(R.string.theme_preview_selected),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
          }
        } else {
          Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            listOf(
              previewScheme.background,
              previewScheme.primary,
              previewScheme.secondary,
              previewSemantic.reachable,
            ).forEach { swatch ->
              Box(
                modifier =
                  Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(swatch),
              )
            }
          }
        }
      }

      // Rich Mini-UI Preview Stage (Background, Surface, Sample Card, Badge, Buttons, Typography, Nav)
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = previewScheme.background,
        border = BorderStroke(BdixBorder.hairline, previewScheme.outlineVariant),
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          Column(
            modifier =
              Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            // Sample Server Card with Typography & Status Badge
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(10.dp),
              color = previewSemantic.cardBackground,
              border = BorderStroke(BdixBorder.hairline, previewSemantic.cardBorder),
            ) {
              Row(
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(
                  modifier = Modifier.weight(1f),
                  verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                  Text(
                    text = stringResource(R.string.theme_preview_sample_card_title),
                    style =
                      MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                      ),
                    color = previewScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = "http://10.16.100.244",
                    style = MaterialTheme.typography.labelSmall,
                    color = previewScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = previewSemantic.reachableContainer,
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.CheckCircle,
                      contentDescription = null,
                      tint = previewSemantic.reachable,
                      modifier = Modifier.size(11.dp),
                    )
                    Text(
                      text = stringResource(R.string.theme_preview_sample_badge),
                      style =
                        MaterialTheme.typography.labelSmall.copy(
                          fontWeight = FontWeight.SemiBold,
                        ),
                      color = previewSemantic.reachable,
                      maxLines = 1,
                    )
                  }
                }
              }
            }

            // Primary CTA (two-tone gradient) + Secondary CTA (tonal)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Box(
                modifier =
                  Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                      Brush.horizontalGradient(
                        colors =
                          listOf(
                            previewSemantic.ctaGradientStart,
                            previewSemantic.ctaGradientEnd,
                          ),
                      ),
                    ),
                contentAlignment = Alignment.Center,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Radar,
                    contentDescription = null,
                    tint = previewSemantic.ctaContent,
                    modifier = Modifier.size(13.dp),
                  )
                  Text(
                    text = stringResource(R.string.theme_preview_primary_btn),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = previewSemantic.ctaContent,
                    maxLines = 1,
                  )
                }
              }

              Surface(
                modifier =
                  Modifier
                    .weight(1f)
                    .height(32.dp),
                shape = RoundedCornerShape(999.dp),
                color = previewSemantic.secondaryCta,
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = stringResource(R.string.theme_preview_secondary_btn),
                    style =
                      MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                      ),
                    color = previewSemantic.onSecondaryCta,
                    maxLines = 1,
                  )
                }
              }
            }
          }

          // Mini Navigation Bar Strip
          Surface(
            modifier = Modifier.fillMaxWidth(),
            color = previewSemantic.navBackground,
          ) {
            Row(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(
                shape = RoundedCornerShape(999.dp),
                color = previewSemantic.navIndicator,
              ) {
                Icon(
                  imageVector = Icons.Rounded.Home,
                  contentDescription = null,
                  tint = previewScheme.primary,
                  modifier =
                    Modifier
                      .padding(horizontal = 10.dp, vertical = 3.dp)
                      .size(14.dp),
                )
              }
              Icon(
                imageVector = Icons.Rounded.Dns,
                contentDescription = null,
                tint = previewScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
              )
              Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = null,
                tint = previewScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
internal fun presetTitle(preset: AppThemePreset): String =
  when (preset) {
    AppThemePreset.MIDNIGHT_INDIGO -> stringResource(R.string.theme_preset_midnight_indigo)
    AppThemePreset.GRAPHITE_AMBER -> stringResource(R.string.theme_preset_graphite_amber)
    AppThemePreset.DEEP_EMERALD -> stringResource(R.string.theme_preset_deep_emerald)
    AppThemePreset.WARM_PREMIUM -> stringResource(R.string.theme_preset_warm_premium)
  }

@Composable
internal fun presetDescription(preset: AppThemePreset): String =
  when (preset) {
    AppThemePreset.MIDNIGHT_INDIGO -> stringResource(R.string.theme_preset_midnight_indigo_desc)
    AppThemePreset.GRAPHITE_AMBER -> stringResource(R.string.theme_preset_graphite_amber_desc)
    AppThemePreset.DEEP_EMERALD -> stringResource(R.string.theme_preset_deep_emerald_desc)
    AppThemePreset.WARM_PREMIUM -> stringResource(R.string.theme_preset_warm_premium_desc)
  }

@Composable
internal fun appearanceLabel(preference: com.jubayerhossain.bdixtester.core.model.ThemePreference): String =
  when (preference) {
    com.jubayerhossain.bdixtester.core.model.ThemePreference.SYSTEM ->
      stringResource(R.string.settings_appearance_system)
    com.jubayerhossain.bdixtester.core.model.ThemePreference.LIGHT ->
      stringResource(R.string.settings_appearance_light)
    com.jubayerhossain.bdixtester.core.model.ThemePreference.DARK ->
      stringResource(R.string.settings_appearance_dark)
  }
