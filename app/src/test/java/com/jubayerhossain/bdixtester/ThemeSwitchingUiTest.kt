package com.jubayerhossain.bdixtester

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.jubayerhossain.bdixtester.core.model.AppThemePreset
import com.jubayerhossain.bdixtester.core.model.ThemePreference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class ThemeSwitchingUiTest {
  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @After
  fun resetThemePreferences() {
    runBlocking {
      val app = ApplicationProvider.getApplicationContext<BdixtesterApplication>()
      app.appContainer.settingsRepository.setThemePreset(AppThemePreset.MIDNIGHT_INDIGO)
      app.appContainer.settingsRepository.setThemePreference(ThemePreference.SYSTEM)
    }
  }

  @Test
  fun allFourThemePresetsAndLightDarkAppearancesCanBeSelectedAndPersisted() {
    val app = ApplicationProvider.getApplicationContext<BdixtesterApplication>()
    val activity = composeTestRule.activity
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("nav_settings").performClick()
    composeTestRule.waitForIdle()

    // Default preset is Midnight Indigo
    composeTestRule
      .onNodeWithTag("settings_list")
      .performScrollToNode(hasTestTag("theme_preset_card_midnight_indigo"))
    composeTestRule
      .onNodeWithTag("theme_preset_card_midnight_indigo")
      .assertIsDisplayed()
      .assertIsSelected()

    // Switch through Graphite & Amber, Deep Emerald, and Warm Premium
    val presetsToTest =
      listOf(
        AppThemePreset.GRAPHITE_AMBER to "theme_preset_card_graphite_amber",
        AppThemePreset.DEEP_EMERALD to "theme_preset_card_deep_emerald",
        AppThemePreset.WARM_PREMIUM to "theme_preset_card_warm_premium",
      )

    presetsToTest.forEach { (preset, tag) ->
      composeTestRule.onNodeWithTag("settings_list").performScrollToNode(hasTestTag(tag))
      composeTestRule.onNodeWithTag(tag).assertIsDisplayed().performClick()
      composeTestRule.waitUntil(timeoutMillis = 5_000L) {
        composeTestRule
          .onAllNodes(hasTestTag(tag) and isSelected())
          .fetchSemanticsNodes()
          .isNotEmpty()
      }
      composeTestRule.onNodeWithTag(tag).assertIsSelected()

      val persisted =
        runBlocking {
          app.appContainer.settingsRepository.settings
            .first()
        }
      assertEquals(preset, persisted.themePreset)
    }

    // Switch Appearance Mode: Dark -> Light
    val darkLabel = activity.getString(R.string.settings_appearance_dark)
    composeTestRule
      .onNodeWithTag("settings_list")
      .performScrollToNode(hasTestTag("settings_theme_row"))
    composeTestRule.onNodeWithTag("settings_theme_row").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("theme_option_dark").assertIsDisplayed().performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000L) {
      composeTestRule
        .onAllNodes(hasTestTag("settings_theme_row") and hasText(darkLabel, substring = true))
        .fetchSemanticsNodes()
        .isNotEmpty()
    }
    assertEquals(
      ThemePreference.DARK,
      runBlocking {
        app.appContainer.settingsRepository.settings
          .first()
          .themePreference
      },
    )

    val lightLabel = activity.getString(R.string.settings_appearance_light)
    composeTestRule
      .onNodeWithTag("settings_list")
      .performScrollToNode(hasTestTag("settings_theme_row"))
    composeTestRule.onNodeWithTag("settings_theme_row").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("theme_option_light").assertIsDisplayed().performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000L) {
      composeTestRule
        .onAllNodes(hasTestTag("settings_theme_row") and hasText(lightLabel, substring = true))
        .fetchSemanticsNodes()
        .isNotEmpty()
    }
    assertEquals(
      ThemePreference.LIGHT,
      runBlocking {
        app.appContainer.settingsRepository.settings
          .first()
          .themePreference
      },
    )

    // Verify Home and Servers remain functional after theme and appearance switches
    composeTestRule.onNodeWithTag("nav_home").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("dashboard_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_start_test_button").assertIsDisplayed()

    composeTestRule.onNodeWithTag("nav_servers").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("servers_screen").assertIsDisplayed()
  }
}
