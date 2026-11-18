package com.jubayerhossain.bdixtester

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.jubayerhossain.bdixtester.core.export.CatalogExportSerializer
import com.jubayerhossain.bdixtester.core.export.ExportDataset
import com.jubayerhossain.bdixtester.core.export.ExportFormat
import com.jubayerhossain.bdixtester.core.model.CatalogSourceType
import com.jubayerhossain.bdixtester.core.model.ServerCategory
import com.jubayerhossain.bdixtester.core.model.ServerProtocol
import com.jubayerhossain.bdixtester.core.model.VerificationState
import com.jubayerhossain.bdixtester.core.util.BrowserLaunchResult
import com.jubayerhossain.bdixtester.core.util.ExternalBrowserHelper
import com.jubayerhossain.bdixtester.domain.model.ServerEndpoint
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExportAndIntentSafetyTest {
  @Test
  fun csvFormulaTriggersAreNeutralizedWhileJsonPreservesVerbatimValues() {
    assertEquals("\"'=CMD('calc')\"", CatalogExportSerializer.neutralizeCsvCell("=CMD('calc')"))
    assertEquals("\"'+1+1\"", CatalogExportSerializer.neutralizeCsvCell("+1+1"))
    assertEquals("\"'-1\"", CatalogExportSerializer.neutralizeCsvCell("-1"))
    assertEquals("\"'@SUM(A1:A2)\"", CatalogExportSerializer.neutralizeCsvCell("@SUM(A1:A2)"))
    assertEquals("\"'\t=A1\"", CatalogExportSerializer.neutralizeCsvCell("\t=A1"))
    assertEquals("\"'\r=A1\"", CatalogExportSerializer.neutralizeCsvCell("\r=A1"))
    assertEquals("\"'  =1\"", CatalogExportSerializer.neutralizeCsvCell("  =1"))

    val endpoint =
      ServerEndpoint(
        id = "builtin:116",
        name = "=DangerousName",
        originalUrl = "http://172.27.27.26//2TB4",
        host = "172.27.27.26",
        explicitPort = null,
        protocol = ServerProtocol.HTTP,
        category = ServerCategory.UNCATEGORIZED,
        isFeatured = false,
        sourceType = CatalogSourceType.BUILT_IN,
        sourceIndex = 116,
        verificationState = VerificationState.UNKNOWN,
        lastReviewedAtEpochMs = null,
        speedTestUrl = null,
        isPrivateIp = true,
      )

    val csv =
      CatalogExportSerializer.exportEndpoints(
        endpoints = listOf(endpoint),
        latestResults = emptyMap(),
        favorites = emptyMap(),
        format = ExportFormat.CSV,
        dataset = ExportDataset.CATALOG,
      )
    assertTrue(csv.contains("\"'=DangerousName\""))

    val jsonText =
      CatalogExportSerializer.exportEndpoints(
        endpoints = listOf(endpoint),
        latestResults = emptyMap(),
        favorites = emptyMap(),
        format = ExportFormat.JSON,
        dataset = ExportDataset.CATALOG,
      )
    val item = JSONObject(jsonText).getJSONArray("items").getJSONObject(0)
    assertEquals("http://172.27.27.26//2TB4", item.getString("originalUrl"))
    assertEquals("=DangerousName", item.getString("name"))
  }

  @Test
  fun externalBrowserHelperAllowsOnlyHttpAndHttpsWithValidHost() {
    assertTrue(ExternalBrowserHelper.isAllowedBrowserUrl("http://ftpbd.net"))
    assertTrue(ExternalBrowserHelper.isAllowedBrowserUrl("https://samftp.com/path"))
    assertFalse(ExternalBrowserHelper.isAllowedBrowserUrl("ftp://10.16.100.244"))
    assertFalse(ExternalBrowserHelper.isAllowedBrowserUrl("ftps://example.com:990"))
    assertFalse(ExternalBrowserHelper.isAllowedBrowserUrl("file:///etc/passwd"))
    assertFalse(ExternalBrowserHelper.isAllowedBrowserUrl("javascript:alert(1)"))
    assertFalse(ExternalBrowserHelper.isAllowedBrowserUrl("http://"))
    assertFalse(ExternalBrowserHelper.isAllowedBrowserUrl("   "))
  }

  @Test
  fun createExternalBrowserIntentBuildsBrowsableActionViewForHttpAndHttpsOnly() {
    val httpIntent = ExternalBrowserHelper.createExternalBrowserIntent("http://ftpbd.net")
    assertNotNull(httpIntent)
    assertEquals(Intent.ACTION_VIEW, httpIntent!!.action)
    assertEquals(Uri.parse("http://ftpbd.net"), httpIntent.data)
    assertTrue(httpIntent.categories.contains(Intent.CATEGORY_BROWSABLE))

    val httpsIntent = ExternalBrowserHelper.createExternalBrowserIntent("https://samftp.com/movies")
    assertNotNull(httpsIntent)
    assertEquals(Intent.ACTION_VIEW, httpsIntent!!.action)
    assertEquals(Uri.parse("https://samftp.com/movies"), httpsIntent.data)
    assertTrue(httpsIntent.categories.contains(Intent.CATEGORY_BROWSABLE))

    assertNull(ExternalBrowserHelper.createExternalBrowserIntent("ftp://10.16.100.244"))
    assertNull(ExternalBrowserHelper.createExternalBrowserIntent("javascript:alert(1)"))
    assertNull(ExternalBrowserHelper.createExternalBrowserIntent("http://"))
  }

  @Test
  fun launchUrlReturnsTypedErrorsSafelyWithoutCrashAndLaunchesWhenBrowserRegistered() {
    val app = ApplicationProvider.getApplicationContext<Application>()

    // Unsupported FTP and invalid schemes are rejected cleanly
    assertEquals(
      BrowserLaunchResult.UnsupportedFtp,
      ExternalBrowserHelper.launchUrl(app, "ftp://10.16.100.244"),
    )
    assertEquals(
      BrowserLaunchResult.InvalidUrl,
      ExternalBrowserHelper.launchUrl(app, "javascript:alert(1)"),
    )

    // When no browser handles http/https, returns NoBrowserAvailable without crashing
    assertEquals(
      BrowserLaunchResult.NoBrowserAvailable,
      ExternalBrowserHelper.launchUrl(app, "https://samftp.com"),
    )

    // Register a mock default external browser in Robolectric PackageManager
    val browserComponent = ComponentName("com.android.chrome", "com.google.android.apps.chrome.Main")
    val shadowPm = shadowOf(app.packageManager)
    shadowPm.addActivityIfNotPresent(browserComponent)
    val filter =
      IntentFilter(Intent.ACTION_VIEW).apply {
        addCategory(Intent.CATEGORY_DEFAULT)
        addCategory(Intent.CATEGORY_BROWSABLE)
        addDataScheme("http")
        addDataScheme("https")
      }
    shadowPm.addIntentFilterForActivity(browserComponent, filter)

    val launchResult = ExternalBrowserHelper.launchUrl(app, "https://samftp.com")
    assertEquals(BrowserLaunchResult.Launched, launchResult)

    val startedIntent = shadowOf(app).nextStartedActivity
    assertNotNull(startedIntent)
    assertEquals(Intent.ACTION_VIEW, startedIntent.action)
    assertEquals(Uri.parse("https://samftp.com"), startedIntent.data)
    assertTrue(startedIntent.categories.contains(Intent.CATEGORY_BROWSABLE))
  }
}
