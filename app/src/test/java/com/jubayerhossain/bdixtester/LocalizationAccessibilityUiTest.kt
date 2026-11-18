package com.jubayerhossain.bdixtester

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.jubayerhossain.bdixtester.core.model.AppLanguage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "bn-rBD-w411dp-h891dp-xxhdpi")
class LocalizationAccessibilityUiTest {
  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @After
  fun resetLanguagePreference() {
    runBlocking {
      val app = ApplicationProvider.getApplicationContext<BdixtesterApplication>()
      app.appContainer.settingsRepository.setAppLanguage(AppLanguage.SYSTEM)
    }
  }

  @Test
  fun banglaLocaleRendersBottomNavigationOttDirectoryAndModernSettingsWithoutCrash() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("dashboard_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_menu_button").assertDoesNotExist()
    composeTestRule.onNodeWithTag("home_scan_mode_selector").assertDoesNotExist()
    composeTestRule.onNodeWithTag("home_network_summary_card").assertDoesNotExist()
    composeTestRule.onNodeWithTag("home_top_ip_pill").assertDoesNotExist()
    composeTestRule.onNodeWithTag("home_category_summary_strip").assertDoesNotExist()
    composeTestRule.onNodeWithTag("home_scan_mode_pill").assertDoesNotExist()
    composeTestRule.onNodeWithTag("home_tester_ring").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_start_test_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("main_bottom_navigation_bar").assertIsDisplayed()

    // Verify all 5 Bangla bottom navigation items render cleanly
    composeTestRule.onNodeWithTag("nav_home").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_servers").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_saved").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_history").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_settings").assertIsDisplayed()

    // Verify Servers & OTT directory render cleanly in Bangla
    composeTestRule.onNodeWithTag("nav_servers").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("servers_screen").assertIsDisplayed()
    val activity = composeTestRule.activity
    composeTestRule
      .onNodeWithTag("servers_category_ftp")
      .assertTextEquals(activity.getString(R.string.servers_bottom_ftp))
    composeTestRule
      .onNodeWithTag("servers_category_movie")
      .assertTextEquals(activity.getString(R.string.servers_bottom_movie))
    composeTestRule
      .onNodeWithTag("servers_category_tv")
      .assertTextEquals(activity.getString(R.string.servers_bottom_tv))
    composeTestRule.onNodeWithTag("servers_category_ott").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("ott_card_ott_chorki").assertIsDisplayed()

    // Verify redesigned Settings header, grouped list & language modal dialog in Bangla
    composeTestRule.onNodeWithTag("nav_settings").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("settings_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("settings_app_header_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("settings_list").performScrollToNode(hasTestTag("settings_language_row"))
    composeTestRule.onNodeWithTag("settings_language_row").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("language_option_bangla").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("nav_home").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("dashboard_screen").assertIsDisplayed()
  }
}
