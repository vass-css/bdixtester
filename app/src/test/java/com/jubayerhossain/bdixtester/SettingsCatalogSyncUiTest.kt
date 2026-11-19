package com.jubayerhossain.bdixtester

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsCatalogSyncUiTest {
  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun syncCatalogButtonWorksWithoutRemoteUrlAndSupportsBuiltInAndRemoteFlows() {
    composeTestRule.waitForIdle()

    // Navigate to Settings and scroll down to Sync Catalog button
    composeTestRule.onNodeWithTag("nav_settings").performClick()
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithTag("settings_list")
      .performScrollToNode(hasTestTag("settings_sync_now_button"))

    // Previously stayed disabled when remoteCatalogUrl was blank; now must be enabled and clickable
    composeTestRule
      .onNodeWithTag("settings_sync_now_button")
      .assertIsDisplayed()
      .assertIsEnabled()
      .performClick()
    composeTestRule.waitForIdle()

    // Guided Catalog Sync options dialog appears with Built-in Refresh & Remote URL setup
    composeTestRule.onNodeWithTag("settings_sync_options_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("sync_option_refresh_builtin").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    // Built-in verification status appears without leaving user stuck
    composeTestRule.waitUntil(timeoutMillis = 5_000L) {
      composeTestRule
        .onAllNodesWithText("Built-in", substring = true, ignoreCase = true)
        .fetchSemanticsNodes()
        .isNotEmpty()
    }

    // Re-open Sync Options -> choose Configure Remote URL -> enter URL and Save & Sync
    composeTestRule.onNodeWithTag("settings_sync_now_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("sync_option_configure_remote").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithTag("settings_remote_url_input")
      .assertIsDisplayed()
      .performTextReplacement("http://insecure.example.invalid/manifest.json")
    composeTestRule.onNodeWithTag("settings_remote_url_save").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    // Sync feedback surfaces clear validation outcome instead of silent failure
    composeTestRule.waitUntil(timeoutMillis = 5_000L) {
      composeTestRule
        .onAllNodes(hasText("https://", substring = true, ignoreCase = true))
        .fetchSemanticsNodes()
        .isNotEmpty() ||
        composeTestRule
          .onAllNodes(hasText("Wi-Fi", substring = true, ignoreCase = true))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
  }
}
