package com.jubayerhossain.bdixtester

import com.jubayerhossain.bdixtester.core.model.ErrorClassification
import com.jubayerhossain.bdixtester.core.model.ScanMode
import com.jubayerhossain.bdixtester.core.model.ServerCategory
import com.jubayerhossain.bdixtester.core.model.ServerProtocol
import com.jubayerhossain.bdixtester.core.model.TestStatus
import com.jubayerhossain.bdixtester.core.model.VerificationState
import com.jubayerhossain.bdixtester.core.util.ActiveResultMatcher
import com.jubayerhossain.bdixtester.core.validation.UrlParser
import com.jubayerhossain.bdixtester.data.catalog.BuiltInCatalog
import com.jubayerhossain.bdixtester.domain.model.ServerTestResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltInCatalogAuditTest {
  @Test
  fun curatedBdixCatalogDeduplicatesEntriesPreservesIspNamesAndCoversAllCategories() {
    val summary = BuiltInCatalog.computeCuratedAuditSummary()
    val curatedEntries = BuiltInCatalog.loadCuratedCatalog()

    assertEquals(752, curatedEntries.size)
    assertEquals(758, summary.originalEndpointCount)
    assertEquals(752, summary.uniqueEndpointCount)
    assertEquals(6, summary.exactDuplicateCount)
    assertEquals(0, summary.discrepancyCount)
    assertEquals(0, summary.invalidUrlCount)

    // Verify zero duplicate canonical keys
    val canonicalKeys = curatedEntries.map { UrlParser.canonicalKey(it.originalUrl) }
    assertEquals(curatedEntries.size, canonicalKeys.toSet().size)

    // Verify all entries have ISP names, valid hosts/IPs, contiguous IDs, and explicit categories
    curatedEntries.forEachIndexed { index, endpoint ->
      assertEquals("builtin:$index", endpoint.id)
      assertEquals(index, endpoint.sourceIndex)
      assertTrue("Expected ISP name for ${endpoint.originalUrl}", endpoint.name.isNotBlank())
      assertTrue("Expected attribution for ${endpoint.originalUrl}", !endpoint.sourceAttribution.isNullOrBlank())
      assertTrue("Expected host/IP for ${endpoint.originalUrl}", endpoint.host.isNotBlank())
      assertFalse(endpoint.isFeatured)
      assertEquals(VerificationState.UNKNOWN, endpoint.verificationState)
      assertTrue(
        endpoint.category == ServerCategory.FTP_FILES ||
          endpoint.category == ServerCategory.MOVIES ||
          endpoint.category == ServerCategory.LIVE_TV,
      )
    }

    // Verify exact serial category distribution (FTP -> Movies -> TV) with no FtpLinks extras
    assertEquals(542, curatedEntries.count { it.category == ServerCategory.FTP_FILES })
    assertEquals(121, curatedEntries.count { it.category == ServerCategory.MOVIES })
    assertEquals(89, curatedEntries.count { it.category == ServerCategory.LIVE_TV })
    assertFalse(curatedEntries.any { it.host == "fileninja.net" })

    // Verify ISP + IP mapping and distinct scheme preservation (http vs https)
    assertTrue(curatedEntries.any { it.host == "103.58.73.6" && it.name == "Business Network (FTPBD)" })
    assertTrue(curatedEntries.any { it.host == "10.16.100.244" && it.name == "ICC Communication Limited" })
    assertTrue(curatedEntries.any { it.originalUrl == "http://ctgoz.com" && it.category == ServerCategory.MOVIES })
    assertTrue(
      curatedEntries.any { it.originalUrl == "https://ctgoz.com" && it.category == ServerCategory.FTP_FILES },
    )
  }

  @Test
  fun distinctPortsAndPathsOnSameHostAreNeverCollapsed() {
    val sample =
      listOf(
        "http://example.local/",
        "http://example.local:8080/",
        "http://example.local/movies/",
        "http://example.local/",
      )
    val summary = BuiltInCatalog.computeAuditSummary(sample)
    val parsed = BuiltInCatalog.parseSourceCatalog(sample)

    assertEquals(4, summary.originalEndpointCount)
    assertEquals(3, summary.uniqueEndpointCount)
    assertEquals(1, summary.exactDuplicateCount)
    assertEquals(4, parsed.size)
    assertTrue(parsed.any { it.originalUrl == "http://example.local:8080/" })
    assertTrue(parsed.any { it.originalUrl == "http://example.local/movies/" })
  }

  @Test
  fun activeResultMatcherIgnoresStaleUrlsAndDeletedEndpoints() {
    val curated = BuiltInCatalog.loadCuratedCatalog().take(2)
    val first = curated[0]
    val second = curated[1]

    val rawResults =
      mapOf(
        first.id to
          ServerTestResult(
            serverId = first.id,
            serverNameSnapshot = first.name,
            urlSnapshot = first.originalUrl,
            testedAtEpochMs = 1000L,
            scanMode = ScanMode.QUICK_SCAN,
            protocol = ServerProtocol.HTTP,
            status = TestStatus.REACHABLE,
            errorClassification = ErrorClassification.NONE,
          ),
        second.id to
          ServerTestResult(
            serverId = second.id,
            serverNameSnapshot = "Old Replaced Catalog Entry",
            urlSnapshot = "http://legacy-old-catalog.invalid",
            testedAtEpochMs = 1000L,
            scanMode = ScanMode.QUICK_SCAN,
            protocol = ServerProtocol.HTTP,
            status = TestStatus.REACHABLE,
            errorClassification = ErrorClassification.NONE,
          ),
        "custom:deleted" to
          ServerTestResult(
            serverId = "custom:deleted",
            serverNameSnapshot = "Deleted Custom",
            urlSnapshot = "http://10.99.0.1",
            testedAtEpochMs = 1000L,
            scanMode = ScanMode.QUICK_SCAN,
            protocol = ServerProtocol.HTTP,
            status = TestStatus.REACHABLE,
            errorClassification = ErrorClassification.NONE,
          ),
      )

    val matched = ActiveResultMatcher.matchActive(curated, rawResults)
    assertEquals(1, matched.size)
    assertTrue(matched.containsKey(first.id))
    assertFalse(matched.containsKey(second.id))
    assertFalse(matched.containsKey("custom:deleted"))
  }
}
