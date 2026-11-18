package com.jubayerhossain.bdixtester.data.catalog

import com.jubayerhossain.bdixtester.core.model.CatalogAuditSummary
import com.jubayerhossain.bdixtester.core.model.CatalogSourceType
import com.jubayerhossain.bdixtester.core.model.ServerCategory
import com.jubayerhossain.bdixtester.core.model.ServerProtocol
import com.jubayerhossain.bdixtester.core.model.VerificationState
import com.jubayerhossain.bdixtester.core.validation.UrlParser
import com.jubayerhossain.bdixtester.domain.model.ServerEndpoint

object BuiltInCatalog {
  fun builtInIdForIndex(sourceIndex: Int): String = "builtin:$sourceIndex"

  /**
   * Loads the curated multi-ISP BDIX catalog (FTP, Movies, Live TV) with zero duplicates
   * allowed (deduplicated by [UrlParser.canonicalKey]).
   */
  fun loadCuratedCatalog(entries: List<BuiltInCatalogEntry> = BundledBdixCatalog.entries): List<ServerEndpoint> {
    val result = ArrayList<ServerEndpoint>(entries.size)
    val seenKeys = HashSet<String>(entries.size)

    entries.forEach { entry ->
      val cleanedUrl = entry.url.trim()
      if (cleanedUrl.isEmpty()) return@forEach

      val canonicalKey = UrlParser.canonicalKey(cleanedUrl)
      if (canonicalKey.isEmpty() || !seenKeys.add(canonicalKey)) {
        return@forEach
      }

      val parsed = UrlParser.parse(cleanedUrl)
      if (!parsed.isValid || parsed.host == null) {
        return@forEach
      }

      val sourceIndex = result.size
      val ispDisplayName =
        entry.ispName.trim().ifEmpty {
          buildDisplayName(parsed.host, parsed.explicitPort)
        }

      result.add(
        ServerEndpoint(
          id = builtInIdForIndex(sourceIndex),
          name = ispDisplayName,
          originalUrl = cleanedUrl,
          host = parsed.host,
          explicitPort = parsed.explicitPort,
          protocol = parsed.protocol,
          category = entry.category,
          isFeatured = false,
          sourceType = CatalogSourceType.BUILT_IN,
          sourceIndex = sourceIndex,
          verificationState = VerificationState.UNKNOWN,
          lastReviewedAtEpochMs = null,
          description = null,
          sourceAttribution = ispDisplayName,
          speedTestUrl = null,
          speedTestMaxBytes = null,
          isPrivateIp = parsed.isPrivateIp,
        ),
      )
    }
    return result
  }

  fun computeCuratedAuditSummary(
    entries: List<BuiltInCatalogEntry> = BundledBdixCatalog.entries,
  ): CatalogAuditSummary {
    val curated = loadCuratedCatalog(entries)
    val parsedList = curated.map { UrlParser.parse(it.originalUrl) }
    val validEntries = parsedList.filter { it.isValid }
    val rawSourceTotal =
      if (entries === BundledBdixCatalog.entries) {
        RAW_JSON_SOURCE_COUNT
      } else {
        entries.size
      }

    return CatalogAuditSummary(
      expectedSourceCount = rawSourceTotal,
      originalEndpointCount = rawSourceTotal,
      discrepancyCount = 0,
      successfullyParsedCount = validEntries.size,
      uniqueEndpointCount = curated.size,
      exactDuplicateCount = (rawSourceTotal - curated.size).coerceAtLeast(0),
      invalidUrlCount = parsedList.count { !it.isValid },
      httpEndpointCount = validEntries.count { it.protocol == ServerProtocol.HTTP },
      httpsEndpointCount = validEntries.count { it.protocol == ServerProtocol.HTTPS },
      ftpEndpointCount = validEntries.count { it.protocol == ServerProtocol.FTP },
      ftpsEndpointCount = validEntries.count { it.protocol == ServerProtocol.FTPS },
      unsupportedSchemeCount =
        parsedList.count {
          it.isValid && it.protocol == ServerProtocol.UNSUPPORTED
        },
      hostnameEndpointCount = validEntries.count { !it.isIpv4 },
      publicIpv4EndpointCount = validEntries.count { it.isIpv4 && !it.isPrivateIp },
      privateIpv4EndpointCount = validEntries.count { it.isPrivateIp },
      explicitPortCount = validEntries.count { it.explicitPort != null },
      nonRootPathCount = validEntries.count { it.hasNonRootPath },
      manualReviewCount = parsedList.count { it.requiresManualReview },
    )
  }

  /**
   * Parses raw URL strings into [ServerEndpoint] records while preserving exact order and items
   * for explicit raw-list audit & probe-coalescing tests.
   */
  fun parseSourceCatalog(rawLinks: List<String> = FtpLinks.links): List<ServerEndpoint> {
    val result = ArrayList<ServerEndpoint>(rawLinks.size)

    rawLinks.forEachIndexed { index, rawUrl ->
      val parsed = UrlParser.parse(rawUrl)
      val fallbackHost = parsed.host ?: rawUrl
      val displayName = buildDisplayName(fallbackHost, parsed.explicitPort)

      result.add(
        ServerEndpoint(
          id = builtInIdForIndex(index),
          name = displayName,
          originalUrl = rawUrl,
          host = fallbackHost,
          explicitPort = parsed.explicitPort,
          protocol = if (parsed.isValid) parsed.protocol else ServerProtocol.UNSUPPORTED,
          category = ServerCategory.UNCATEGORIZED,
          isFeatured = false,
          sourceType = CatalogSourceType.BUILT_IN,
          sourceIndex = index,
          verificationState = VerificationState.UNKNOWN,
          lastReviewedAtEpochMs = null,
          description = null,
          sourceAttribution = "User-supplied FtpLinks source (entry #$index)",
          speedTestUrl = null,
          speedTestMaxBytes = null,
          isPrivateIp = parsed.isPrivateIp,
        ),
      )
    }
    return result
  }

  fun parseRuntimeCatalog(): List<ServerEndpoint> = loadCuratedCatalog()

  fun parseRuntimeCatalog(rawLinks: List<String>): List<ServerEndpoint> = parseSourceCatalog(rawLinks)

  fun computeAuditSummary(rawLinks: List<String> = FtpLinks.links): CatalogAuditSummary {
    val parsedList = rawLinks.map { UrlParser.parse(it) }
    val uniqueCount = rawLinks.toSet().size
    val duplicateCount = rawLinks.size - uniqueCount
    val validEntries = parsedList.filter { it.isValid }

    return CatalogAuditSummary(
      expectedSourceCount = FtpLinks.EXPECTED_SOURCE_COUNT,
      originalEndpointCount = rawLinks.size,
      discrepancyCount = FtpLinks.EXPECTED_SOURCE_COUNT - rawLinks.size,
      successfullyParsedCount = validEntries.size,
      uniqueEndpointCount = uniqueCount,
      exactDuplicateCount = duplicateCount,
      invalidUrlCount = parsedList.count { !it.isValid },
      httpEndpointCount = validEntries.count { it.protocol == ServerProtocol.HTTP },
      httpsEndpointCount = validEntries.count { it.protocol == ServerProtocol.HTTPS },
      ftpEndpointCount = validEntries.count { it.protocol == ServerProtocol.FTP },
      ftpsEndpointCount = validEntries.count { it.protocol == ServerProtocol.FTPS },
      unsupportedSchemeCount =
        parsedList.count {
          it.isValid && it.protocol == ServerProtocol.UNSUPPORTED
        },
      hostnameEndpointCount = validEntries.count { !it.isIpv4 },
      publicIpv4EndpointCount = validEntries.count { it.isIpv4 && !it.isPrivateIp },
      privateIpv4EndpointCount = validEntries.count { it.isPrivateIp },
      explicitPortCount = validEntries.count { it.explicitPort != null },
      nonRootPathCount = validEntries.count { it.hasNonRootPath },
      manualReviewCount = parsedList.count { it.requiresManualReview },
    )
  }

  fun buildDisplayName(
    host: String,
    explicitPort: Int?,
  ): String = if (explicitPort != null) "$host:$explicitPort" else host

  const val DEFAULT_CURATED_COUNT = 752
  private const val RAW_JSON_SOURCE_COUNT = 758
}
