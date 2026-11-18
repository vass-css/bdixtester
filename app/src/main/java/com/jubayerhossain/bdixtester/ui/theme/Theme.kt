package com.jubayerhossain.bdixtester.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import com.jubayerhossain.bdixtester.core.model.AppThemePreset
import com.jubayerhossain.bdixtester.core.model.ThemePreference
import com.jubayerhossain.bdixtester.ui.components.LocalReducedMotion
import com.jubayerhossain.bdixtester.ui.components.rememberReducedMotionEnabled

@Composable
fun resolveUseDarkTheme(themePreference: ThemePreference): Boolean {
  val systemDark = isSystemInDarkTheme()
  return when (themePreference) {
    ThemePreference.SYSTEM -> systemDark
    ThemePreference.LIGHT -> false
    ThemePreference.DARK -> true
  }
}

@Composable
fun BdixTesterTheme(
  themePreset: AppThemePreset = AppThemePreset.MIDNIGHT_INDIGO,
  themePreference: ThemePreference = ThemePreference.SYSTEM,
  reduceAnimations: Boolean = false,
  content: @Composable () -> Unit,
) {
  val useDarkTheme = resolveUseDarkTheme(themePreference)
  val themeSpec = BdixThemeRegistry.specFor(preset = themePreset, isDark = useDarkTheme)
  val currentLocale = LocalConfiguration.current.locales.get(0)
  val activeTypography =
    if (currentLocale?.language == "bn") {
      BdixTypographyBengali
    } else {
      BdixTypography
    }
  val effectiveReducedMotion = rememberReducedMotionEnabled(userReduceAnimations = reduceAnimations)

  CompositionLocalProvider(
    LocalBdixSemanticColors provides themeSpec.semantic,
    LocalReducedMotion provides effectiveReducedMotion,
  ) {
    MaterialTheme(
      colorScheme = themeSpec.colorScheme,
      typography = activeTypography,
      shapes = BdixShapes,
      content = content,
    )
  }
}
