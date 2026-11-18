package com.jubayerhossain.bdixtester

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.jubayerhossain.bdixtester.data.catalog.BdOttPlatforms
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NavigationUiTest {
  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun homeCleanupBottomNavOttDirectoryAndModernSettingsWorkEndToEnd() {
    composeTestRule.waitForIdle()

    // Home screen displays Brand Top Bar, Hero Ring & Start Test; permanently deleted blocks must NOT exist
    composeTestRule.onNodeWithTag("dashboard_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_menu_button").assertDoesNotExist()
    composeTestRule.onNodeWithTag("home_scan_mode_selector").assertDoesNotExist()
    composeTestRule.onNodeWithTag("home_network_summary_card").assertDoesNotExist()
    composeTestRule.onNodeWithTag("home_top_ip_pill").assertDoesNotExist()
    composeTestRule.onNodeWithTag("home_category_summary_strip").assertDoesNotExist()
    composeTestRule.onNodeWithTag("home_scan_mode_pill").assertDoesNotExist()
    composeTestRule.onNodeWithTag("home_tester_ring").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_start_test_button").assertIsDisplayed()

    // Bottom Navigation Bar displays all 5 primary destinations
    composeTestRule.onNodeWithTag("main_bottom_navigation_bar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_home").assertIsDisplayed().assertIsSelected()
    composeTestRule.onNodeWithTag("nav_servers").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_saved").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_history").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_settings").assertIsDisplayed()

    // Navigate to Servers via Bottom Navigation
    composeTestRule.onNodeWithTag("nav_servers").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_servers").assertIsSelected()
    composeTestRule.onNodeWithTag("servers_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("servers_tab_working").assertIsDisplayed()
    composeTestRule.onNodeWithTag("servers_tab_not_tested").assertIsDisplayed()
    composeTestRule.onNodeWithTag("servers_tab_not_working").assertIsDisplayed()
    val activity = composeTestRule.activity
    composeTestRule.onNodeWithTag("servers_category_bar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("servers_category_all").assertDoesNotExist()
    composeTestRule
      .onNodeWithTag("servers_category_ftp")
      .assertIsDisplayed()
      .assertIsSelected()
      .assertTextEquals(activity.getString(R.string.servers_bottom_ftp))
    composeTestRule
      .onNodeWithTag("servers_category_movie")
      .assertIsDisplayed()
      .assertTextEquals(activity.getString(R.string.servers_bottom_movie))
    composeTestRule
      .onNodeWithTag("servers_category_tv")
      .assertIsDisplayed()
      .assertTextEquals(activity.getString(R.string.servers_bottom_tv))
    composeTestRule
      .onNodeWithTag("servers_category_ott")
      .assertIsDisplayed()
      .assertTextEquals(activity.getString(R.string.servers_bottom_ott))

    // Switch to curated Bangladesh OTT Directory (26 verified platforms)
    assertEquals(26, BdOttPlatforms.all.size)
    assertEquals(17, BdOttPlatforms.bangladeshCount)
    assertEquals(9, BdOttPlatforms.globalCount)
    assertTrue(BdOttPlatforms.all.all { it.url.startsWith("https://") })

    composeTestRule.onNodeWithTag("servers_category_ott").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("ott_region_filter_bar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("ott_directory_list").assertIsDisplayed()
    composeTestRule.onNodeWithTag("ott_card_ott_chorki").assertIsDisplayed()

    // Filter to Global in BD OTTs
    composeTestRule.onNodeWithTag("ott_region_global_in_bd").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("ott_card_ott_netflix").assertIsDisplayed()

    // Return to FTP BDIX servers & toggle Search
    composeTestRule.onNodeWithTag("servers_category_ftp").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("servers_search_toggle").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("servers_search_input").assertIsDisplayed()
    composeTestRule.onNodeWithTag("servers_search_toggle").performClick()
    composeTestRule.waitForIdle()

    // Open Add Custom Server dialog from Servers top bar, verify FTP/Movies/TV chips, and save
    composeTestRule.onNodeWithTag("servers_add_custom_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("custom_server_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("custom_server_category_ftp_files").assertIsDisplayed().assertIsSelected()
    composeTestRule.onNodeWithTag("custom_server_category_movies").assertIsDisplayed()
    composeTestRule.onNodeWithTag("custom_server_category_live_tv").assertIsDisplayed()
    composeTestRule.onNodeWithTag("custom_server_category_other").assertDoesNotExist()
    composeTestRule.onNodeWithTag("custom_server_name_input").performTextInput("My BDIX Node")
    composeTestRule.onNodeWithTag("custom_server_url_input").performTextInput("http://10.99.99.99")
    composeTestRule.onNodeWithTag("custom_server_save_button").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000L) {
      composeTestRule
        .onAllNodes(hasTestTag("servers_tab_not_tested") and hasText("543", substring = true))
        .fetchSemanticsNodes()
        .isNotEmpty()
    }
    composeTestRule
      .onNodeWithTag("servers_category_ftp")
      .assertTextEquals(activity.getString(R.string.servers_bottom_ftp))

    // Navigate to Saved tab
    composeTestRule.onNodeWithTag("nav_saved").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_saved").assertIsSelected()
    composeTestRule.onNodeWithTag("favorites_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("favorites_empty_state").assertIsDisplayed()

    // Navigate to History tab
    composeTestRule.onNodeWithTag("nav_history").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_history").assertIsSelected()
    composeTestRule.onNodeWithTag("history_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("history_empty_state").assertIsDisplayed()

    // Navigate to Settings tab and test App Profile header, Catalog Actions, Modal Pickers, Privacy Policy & Footer
    composeTestRule.onNodeWithTag("nav_settings").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_settings").assertIsSelected()
    composeTestRule.onNodeWithTag("settings_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("settings_app_header_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("settings_update_server_list_row").assertIsDisplayed()

    // Verify New Server Add Request opens the real CustomServerDialog
    composeTestRule
      .onNodeWithTag(
        "settings_list",
      ).performScrollToNode(hasTestTag("settings_add_server_request_row"))
    composeTestRule.onNodeWithTag("settings_add_server_request_row").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("custom_server_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("custom_server_cancel_button").performClick()
    composeTestRule.waitForIdle()

    // Verify Theme, Language & Scan Mode rows
    composeTestRule.onNodeWithTag("settings_list").performScrollToNode(hasTestTag("settings_theme_row"))
    composeTestRule.onNodeWithTag("settings_theme_row").assertIsDisplayed()
    composeTestRule.onNodeWithTag("settings_list").performScrollToNode(hasTestTag("settings_language_row"))
    composeTestRule.onNodeWithTag("settings_language_row").assertIsDisplayed()
    composeTestRule.onNodeWithTag("settings_list").performScrollToNode(hasTestTag("settings_scan_mode_row"))
    composeTestRule.onNodeWithTag("settings_scan_mode_row").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("scan_mode_option_full").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    // Verify Privacy Policy modal, Facebook Community, Share App & Version Footer
    composeTestRule.onNodeWithTag("settings_list").performScrollToNode(hasTestTag("settings_privacy_policy_row"))
    composeTestRule.onNodeWithTag("settings_privacy_policy_row").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("settings_privacy_policy_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("settings_privacy_policy_close_button").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("settings_list").performScrollToNode(hasTestTag("settings_facebook_row"))
    composeTestRule.onNodeWithTag("settings_facebook_row").assertIsDisplayed()
    composeTestRule.onNodeWithTag("settings_list").performScrollToNode(hasTestTag("settings_share_app_row"))
    composeTestRule.onNodeWithTag("settings_share_app_row").assertIsDisplayed()
    composeTestRule.onNodeWithTag("settings_list").performScrollToNode(hasTestTag("settings_version_footer"))
    composeTestRule.onNodeWithTag("settings_version_footer").assertIsDisplayed()

    // Return to Home tab
    composeTestRule.onNodeWithTag("nav_home").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_home").assertIsSelected()
    composeTestRule.onNodeWithTag("dashboard_screen").assertIsDisplayed()
  }

  @Test
  fun tappingServerOrOttCardLaunchesExternalBrowserDirectlyWithoutMetadataPage() {
    val activity = composeTestRule.activity
    val browserComponent = ComponentName("com.android.chrome", "com.google.android.apps.chrome.Main")
    val shadowPm = shadowOf(activity.packageManager)
    shadowPm.addActivityIfNotPresent(browserComponent)
    val filter =
      IntentFilter(Intent.ACTION_VIEW).apply {
        addCategory(Intent.CATEGORY_DEFAULT)
        addCategory(Intent.CATEGORY_BROWSABLE)
        addDataScheme("http")
        addDataScheme("https")
      }
    shadowPm.addIntentFilterForActivity(browserComponent, filter)

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_view_servers_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("servers_screen").assertIsDisplayed()

    // Built-in entry #0 ("http://103.58.73.6") under Not Tested opens external browser directly
    composeTestRule.onNodeWithTag("servers_tab_not_tested").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("server_url_link_builtin:0").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    // Server Metadata / Details page is permanently removed everywhere
    composeTestRule.onNodeWithTag("server_details_screen").assertDoesNotExist()
    composeTestRule.onNodeWithTag("servers_screen").assertIsDisplayed()

    val firstIntent = shadowOf(activity).nextStartedActivity
    assertNotNull(firstIntent)
    assertEquals(Intent.ACTION_VIEW, firstIntent.action)
    assertEquals(Uri.parse("http://103.58.73.6"), firstIntent.data)
    assertTrue(firstIntent.categories.contains(Intent.CATEGORY_BROWSABLE))

    // Tapping an OTT card also launches external browser directly
    composeTestRule.onNodeWithTag("servers_category_ott").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("ott_card_ott_chorki").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("server_details_screen").assertDoesNotExist()
    val ottIntent = shadowOf(activity).nextStartedActivity
    assertNotNull(ottIntent)
    assertEquals(Intent.ACTION_VIEW, ottIntent.action)
    assertEquals(Uri.parse("https://chorki.net"), ottIntent.data)
    assertTrue(ottIntent.categories.contains(Intent.CATEGORY_BROWSABLE))
  }

  @Test
  fun homeStaysMinimalAndServerQuickInspectSheetWorksEndToEnd() {
    composeTestRule.waitForIdle()

    // Home screen keeps Orbital Radar & Start Test cleanly without removed pills or mode toggles
    composeTestRule.onNodeWithTag("home_top_ip_pill").assertDoesNotExist()
    composeTestRule.onNodeWithTag("home_category_summary_strip").assertDoesNotExist()
    composeTestRule.onNodeWithTag("home_scan_mode_pill").assertDoesNotExist()
    composeTestRule.onNodeWithTag("home_tester_ring").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_start_test_button").assertIsDisplayed()

    // Navigate to Servers and verify Quick-Inspect sheet opens from inspect icon without legacy details route
    composeTestRule.onNodeWithTag("home_view_servers_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("servers_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("servers_tab_not_tested").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("server_inspect_builtin:0").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("server_quick_inspect_sheet").assertIsDisplayed()
    composeTestRule.onNodeWithTag("sheet_copy_url_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("sheet_open_url_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("server_details_screen").assertDoesNotExist()
    composeTestRule.onNodeWithTag("sheet_copy_url_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("server_quick_inspect_sheet").assertDoesNotExist()
  }
}
