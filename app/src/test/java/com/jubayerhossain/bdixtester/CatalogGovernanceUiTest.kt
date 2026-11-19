package com.jubayerhossain.bdixtester

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CatalogGovernanceUiTest {
  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun governanceScreenEvidenceSheetArchiveConfirmAndRestoreWorkEndToEnd() {
    composeTestRule.waitForIdle()

    // Navigate to Settings -> Catalog Governance & Restore
    composeTestRule.onNodeWithTag("nav_settings").performClick()
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithTag("settings_list")
      .performScrollToNode(hasTestTag("settings_catalog_governance_row"))
    composeTestRule.onNodeWithTag("settings_catalog_governance_row").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    // Verify Governance Screen opens and top-level bottom bar hides on secondary destination
    composeTestRule.onNodeWithTag("catalog_governance_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("governance_overview_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("main_bottom_navigation_bar").assertDoesNotExist()

    // Wait until built-in endpoint cards are seeded and visible
    composeTestRule.waitUntil(timeoutMillis = 5_000L) {
      composeTestRule.onAllNodesWithTag("governance_item_builtin:0").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("governance_item_builtin:0").performClick()
    composeTestRule.waitForIdle()

    // Evidence bottom sheet shows DNS & Registration sections
    composeTestRule.onNodeWithTag("governance_evidence_sheet").assertIsDisplayed()
    composeTestRule.onNodeWithTag("governance_section_dns").assertIsDisplayed()
    composeTestRule.onNodeWithTag("governance_section_registration").assertIsDisplayed()

    // Mark Candidate, then tap Archive and verify confirmation dialog gates archival
    composeTestRule.onNodeWithTag("governance_sheet_candidate_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("governance_sheet_archive_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("governance_archive_confirm_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("governance_archive_confirm_button").performClick()
    composeTestRule.waitForIdle()

    // Verify Archived state shows quick restore chip -> tap Restore -> active catalog retains endpoint
    composeTestRule.onNodeWithTag("governance_sheet_restore_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("governance_sheet_archive_button").assertIsDisplayed()

    // Navigate back to Settings and confirm bottom bar returns
    composeTestRule.onNodeWithTag("governance_back_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("settings_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("main_bottom_navigation_bar").assertIsDisplayed()
  }
}
