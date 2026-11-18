package com.jubayerhossain.bdixtester

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jubayerhossain.bdixtester.core.model.AppThemePreset
import com.jubayerhossain.bdixtester.core.model.ThemePreference
import com.jubayerhossain.bdixtester.data.preferences.SettingsDataStoreRepository
import com.jubayerhossain.bdixtester.ui.theme.BdixThemeRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class ThemeSystemContrastAndMigrationTest {
  @get:Rule
  val tempFolder = TemporaryFolder()

  private fun Color.relativeLuminance(): Double {
    fun linearize(channel: Float): Double {
      val c = channel.toDouble()
      return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * linearize(red) + 0.7152 * linearize(green) + 0.0722 * linearize(blue)
  }

  private fun contrastRatio(
    fg: Color,
    bg: Color,
  ): Double {
    val l1 = fg.relativeLuminance()
    val l2 = bg.relativeLuminance()
    val lighter = max(l1, l2)
    val darker = min(l1, l2)
    return (lighter + 0.05) / (darker + 0.05)
  }

  private fun assertContrast(
    themeLabel: String,
    pairLabel: String,
    fg: Color,
    bg: Color,
    minRatio: Double,
  ) {
    val ratio = contrastRatio(fg, bg)
    assertTrue(
      String.format(
        Locale.US,
        "[%s] %s contrast %.2f:1 is below required %.1f:1",
        themeLabel,
        pairLabel,
        ratio,
        minRatio,
      ),
      ratio >= minRatio,
    )
  }

  @Test
  fun allEightThemeConfigurationsMeetWcag22AaContrastRequirements() {
    assertEquals(8, BdixThemeRegistry.allEightSpecs.size)

    val reportLines = mutableListOf<String>()
    BdixThemeRegistry.allEightSpecs.forEach { spec ->
      val mode = if (spec.isDark) "Dark" else "Light"
      val label = "${spec.preset.displayLabel} ($mode)"
      val cs = spec.colorScheme
      val sem = spec.semantic

      // Text / Surface Pairs >= 4.5:1
      assertContrast(label, "onBackground / background", cs.onBackground, cs.background, 4.5)
      assertContrast(label, "onSurface / surface", cs.onSurface, cs.surface, 4.5)
      assertContrast(label, "onSurface / surfaceContainerLow", cs.onSurface, cs.surfaceContainerLow, 4.5)
      assertContrast(label, "onSurface / surfaceContainer", cs.onSurface, cs.surfaceContainer, 4.5)
      assertContrast(label, "onSurface / cardBackground", cs.onSurface, sem.cardBackground, 4.5)
      assertContrast(label, "onSurfaceVariant / background", cs.onSurfaceVariant, cs.background, 4.5)
      assertContrast(label, "onSurfaceVariant / surfaceContainer", cs.onSurfaceVariant, cs.surfaceContainer, 4.5)
      assertContrast(label, "onSurfaceVariant / cardBackground", cs.onSurfaceVariant, sem.cardBackground, 4.5)

      // Primary / Secondary / Tertiary / Error >= 4.5:1
      assertContrast(label, "primary / background", cs.primary, cs.background, 4.5)
      assertContrast(label, "primary / surfaceContainer", cs.primary, cs.surfaceContainer, 4.5)
      assertContrast(label, "primary / navBackground", cs.primary, sem.navBackground, 4.5)
      assertContrast(label, "primary / navIndicator", cs.primary, sem.navIndicator, 4.5)
      assertContrast(label, "onPrimary / primary", cs.onPrimary, cs.primary, 4.5)
      assertContrast(
        label,
        "onPrimaryContainer / primaryContainer",
        cs.onPrimaryContainer,
        cs.primaryContainer,
        4.5,
      )
      assertContrast(label, "onSecondary / secondary", cs.onSecondary, cs.secondary, 4.5)
      assertContrast(
        label,
        "onSecondaryContainer / secondaryContainer",
        cs.onSecondaryContainer,
        cs.secondaryContainer,
        4.5,
      )
      assertContrast(label, "onTertiary / tertiary", cs.onTertiary, cs.tertiary, 4.5)
      assertContrast(
        label,
        "onTertiaryContainer / tertiaryContainer",
        cs.onTertiaryContainer,
        cs.tertiaryContainer,
        4.5,
      )
      assertContrast(label, "onError / error", cs.onError, cs.error, 4.5)
      assertContrast(label, "onErrorContainer / errorContainer", cs.onErrorContainer, cs.errorContainer, 4.5)

      // Semantic Status Badges >= 4.5:1
      assertContrast(label, "reachable / reachableContainer", sem.reachable, sem.reachableContainer, 4.5)
      assertContrast(label, "reachable / cardBackground", sem.reachable, sem.cardBackground, 4.5)
      assertContrast(label, "onReachable / reachable", sem.onReachable, sem.reachable, 4.5)
      assertContrast(
        label,
        "onReachableContainer / reachableContainer",
        sem.onReachableContainer,
        sem.reachableContainer,
        4.5,
      )

      assertContrast(label, "restricted / restrictedContainer", sem.restricted, sem.restrictedContainer, 4.5)
      assertContrast(label, "restricted / cardBackground", sem.restricted, sem.cardBackground, 4.5)
      assertContrast(label, "onRestricted / restricted", sem.onRestricted, sem.restricted, 4.5)
      assertContrast(
        label,
        "onRestrictedContainer / restrictedContainer",
        sem.onRestrictedContainer,
        sem.restrictedContainer,
        4.5,
      )

      assertContrast(label, "unreachable / unreachableContainer", sem.unreachable, sem.unreachableContainer, 4.5)
      assertContrast(label, "unreachable / cardBackground", sem.unreachable, sem.cardBackground, 4.5)
      assertContrast(label, "onUnreachable / unreachable", sem.onUnreachable, sem.unreachable, 4.5)
      assertContrast(
        label,
        "onUnreachableContainer / unreachableContainer",
        sem.onUnreachableContainer,
        sem.unreachableContainer,
        4.5,
      )

      assertContrast(label, "information / informationContainer", sem.information, sem.informationContainer, 4.5)
      assertContrast(label, "neutral / neutralContainer", sem.neutral, sem.neutralContainer, 4.5)

      // Gradient Worst-Case Contrast (Start, Mid, End) >= 4.5:1
      val ctaMid = lerp(sem.ctaGradientStart, sem.ctaGradientEnd, 0.5f)
      assertContrast(label, "ctaContent / ctaGradientStart", sem.ctaContent, sem.ctaGradientStart, 4.5)
      assertContrast(label, "ctaContent / ctaGradientMid", sem.ctaContent, ctaMid, 4.5)
      assertContrast(label, "ctaContent / ctaGradientEnd", sem.ctaContent, sem.ctaGradientEnd, 4.5)

      val stopMid = lerp(sem.stopGradientStart, sem.stopGradientEnd, 0.5f)
      assertContrast(label, "stopContent / stopGradientStart", sem.stopContent, sem.stopGradientStart, 4.5)
      assertContrast(label, "stopContent / stopGradientMid", sem.stopContent, stopMid, 4.5)
      assertContrast(label, "stopContent / stopGradientEnd", sem.stopContent, sem.stopGradientEnd, 4.5)

      // Component & Interaction Tokens >= 4.5:1 (Text) / >= 3.0:1 (Non-text UI)
      assertContrast(label, "onSecondaryCta / secondaryCta", sem.onSecondaryCta, sem.secondaryCta, 4.5)
      assertContrast(
        label,
        "onFilterChipSelected / filterChipSelected",
        sem.onFilterChipSelected,
        sem.filterChipSelected,
        4.5,
      )
      assertContrast(label, "onSnackbar / snackbarBackground", sem.onSnackbar, sem.snackbarBackground, 4.5)
      assertContrast(label, "ipAccent / surfaceContainer", sem.ipAccent, cs.surfaceContainer, 4.5)
      assertContrast(label, "disabledContent / disabled", sem.disabledContent, sem.disabled, 4.5)

      assertContrast(label, "outline / background (Non-text UI)", cs.outline, cs.background, 3.0)
      assertContrast(
        label,
        "inputBorder / inputBackground (Non-text UI)",
        sem.inputBorder,
        sem.inputBackground,
        3.0,
      )
      assertContrast(
        label,
        "ringGradientStart / background (Non-text UI)",
        sem.ringGradientStart,
        cs.background,
        3.0,
      )
      assertContrast(label, "ringGradientEnd / background (Non-text UI)", sem.ringGradientEnd, cs.background, 3.0)

      val ctaWorst =
        min(
          contrastRatio(sem.ctaContent, sem.ctaGradientStart),
          contrastRatio(sem.ctaContent, sem.ctaGradientEnd),
        )
      reportLines +=
        String.format(
          Locale.US,
          "%s | Text/Bg=%.2f | Primary/Nav=%.2f | Reachable=%.2f | Restricted=%.2f | Unreachable=%.2f | CTA_Worst=%.2f | Outline=%.2f",
          label,
          contrastRatio(cs.onBackground, cs.background),
          contrastRatio(cs.primary, sem.navIndicator),
          contrastRatio(sem.reachable, sem.reachableContainer),
          contrastRatio(sem.restricted, sem.restrictedContainer),
          contrastRatio(sem.unreachable, sem.unreachableContainer),
          ctaWorst,
          contrastRatio(cs.outline, cs.background),
        )
    }
    println("=== WCAG 2.2 AA VERIFICATION SUMMARY ===")
    reportLines.forEach(::println)
  }

  @Test
  fun legacyAndCorruptedThemePreferencesMigrateSafelyToMidnightIndigoAndSystem() =
    runBlocking {
      val dataStoreFile = File(tempFolder.root, "legacy_prefs.preferences_pb")
      val dataStore = PreferenceDataStoreFactory.create(produceFile = { dataStoreFile })

      // Simulate legacy Cyan/Ocean or unknown persisted values from an older release
      dataStore.edit { prefs ->
        prefs[stringPreferencesKey("theme_preset")] = "LEGACY_CYAN_OCEAN"
        prefs[stringPreferencesKey("theme_preference")] = "UNKNOWN_LEGACY_MODE"
      }

      val repo = SettingsDataStoreRepository(dataStore)
      val migrated = repo.settings.first()
      assertEquals(AppThemePreset.MIDNIGHT_INDIGO, migrated.themePreset)
      assertEquals(ThemePreference.SYSTEM, migrated.themePreference)

      // Verify valid preset & appearance updates persist and survive reads
      repo.setThemePreset(AppThemePreset.WARM_PREMIUM)
      repo.setThemePreference(ThemePreference.DARK)
      val updated = repo.settings.first()
      assertEquals(AppThemePreset.WARM_PREMIUM, updated.themePreset)
      assertEquals(ThemePreference.DARK, updated.themePreference)

      // Verify reset restores Midnight Indigo + System Default
      repo.resetSettings()
      val afterReset = repo.settings.first()
      assertEquals(AppThemePreset.MIDNIGHT_INDIGO, afterReset.themePreset)
      assertEquals(ThemePreference.SYSTEM, afterReset.themePreference)
    }
}
