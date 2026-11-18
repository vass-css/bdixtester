package com.jubayerhossain.bdixtester.core.export

import com.jubayerhossain.bdixtester.domain.model.FavoriteInfo
import com.jubayerhossain.bdixtester.domain.model.ScanProgressState
import com.jubayerhossain.bdixtester.domain.model.ServerEndpoint
import com.jubayerhossain.bdixtester.domain.model.ServerTestResult
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class ExportFormat(
  val extension: String,
  val mimeType: String,
) {
  CSV("csv", "text/csv"),
  JSON("json", "application/json"),
  TXT("txt", "text/plain"),
}

enum class ExportDataset(
  val filePrefix: String,
) {
  CATALOG("bdix_catalog"),
  FAVORITES("bdix_favorites"),
  CUSTOM_SERVERS("bdix_custom_servers"),
  HISTORY("bdix_test_history"),
  SCAN_SUMMARY("bdix_scan_summary"),
}

object CatalogExportSerializer {
  fun suggestedFileName(
    dataset: ExportDataset,
    format: ExportFormat,
    nowEpochMs: Long = System.currentTimeMillis(),
  ): String {
    val stamp =
      SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        .apply {
          timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(nowEpochMs))
    return "${dataset.filePrefix}_$stamp.${format.extension}"
  }

  /**
   * Neutralizes CSV formula injection for spreadsheet applications (Excel / Sheets / LibreOffice)
   * by prefixing cells beginning with `=`, `+`, `-`, `@`, `\t`, `\r` (or leading whitespace followed
   * by formula triggers) with a single quote `'`, then applying RFC 4180 CSV quoting.
   */
  fun neutralizeCsvCell(rawValue: String?): String {
    val raw = rawValue.orEmpty()
    val trimmedFirst = raw.trimStart().firstOrNull()
    val needsPrefix =
      raw.isNotEmpty() &&
        (
          raw[0] in FORMULA_TRIGGERS ||
            (trimmedFirst != null && trimmedFirst in FORMULA_TRIGGERS_TRIMMED)
        )
    val safe = if (needsPrefix) "'$raw" else raw
    val mustQuote = safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' } || safe.startsWith('\'')
    return if (mustQuote) {
      "\"${safe.replace("\"", "\"\"")}\""
    } else {
      safe
    }
  }

  fun exportEndpoints(
    endpoints: List<ServerEndpoint>,
    latestResults: Map<String, ServerTestResult>,
    favorites: Map<String, FavoriteInfo>,
    format: ExportFormat,
    dataset: ExportDataset,
  ): String =
    when (format) {
      ExportFormat.CSV -> exportEndpointsCsv(endpoints, latestResults, favorites)
      ExportFormat.JSON -> exportEndpointsJson(endpoints, latestResults, favorites, dataset)
      ExportFormat.TXT -> exportEndpointsTxt(endpoints, latestResults, favorites, dataset)
    }

  fun exportHistory(
    history: List<ServerTestResult>,
    format: ExportFormat,
  ): String =
    when (format) {
      ExportFormat.CSV -> exportHistoryCsv(history)
      ExportFormat.JSON -> exportHistoryJson(history)
      ExportFormat.TXT -> exportHistoryTxt(history)
    }

  fun exportScanSummary(
    progress: ScanProgressState,
    resultsForRun: List<ServerTestResult>,
    format: ExportFormat,
  ): String =
    when (format) {
      ExportFormat.CSV -> exportHistoryCsv(resultsForRun)
      ExportFormat.JSON -> {
        val root =
          JSONObject().apply {
            put("dataset", ExportDataset.SCAN_SUMMARY.name)
            put("scanRunId", progress.scanRunId ?: JSONObject.NULL)
            put("sessionState", progress.sessionState.name)
            put("scanMode", progress.scanMode?.name ?: JSONObject.NULL)
            put("scanScope", progress.scanScope.name)
            put("totalCatalogEntries", progress.totalCatalogEntries)
            put("uniqueExactUrlCount", progress.uniqueExactUrlCount)
            put("executedUniqueProbeCount", progress.executedUniqueProbeCount)
            put("mappedSourceEntryCount", progress.mappedSourceEntryCount)
            put("reachableCount", progress.reachableCount)
            put("unreachableCount", progress.unreachableCount)
            put("restrictedOrErrorCount", progress.restrictedOrErrorCount)
            put("unsupportedCount", progress.unsupportedCount)
            put("cancelledCount", progress.cancelledCount)
            put("elapsedMs", progress.elapsedMs)
            put("results", buildHistoryJsonArray(resultsForRun))
          }
        root.toString(2)
      }
      ExportFormat.TXT ->
        buildString {
          appendLine("BDIX Tester Scan Summary")
          appendLine("Scan Run ID: ${progress.scanRunId ?: "None"}")
          appendLine("Session State: ${progress.sessionState.name}")
          appendLine(
            "Mode: ${progress.scanMode?.displayLabel ?: "N/A"} | Scope: ${progress.scanScope.displayLabel}",
          )
          appendLine(
            "Total Entries: ${progress.totalCatalogEntries} | Unique URLs: ${progress.uniqueExactUrlCount}",
          )
          appendLine(
            "Executed Probes: ${progress.executedUniqueProbeCount} | Mapped Results: ${progress.mappedSourceEntryCount}",
          )
          appendLine(
            "Reachable: ${progress.reachableCount} | Restricted: ${progress.restrictedOrErrorCount} | Unreachable: ${progress.unreachableCount} | Unsupported: ${progress.unsupportedCount} | Cancelled: ${progress.cancelledCount}",
          )
          appendLine("Elapsed: ${progress.elapsedMs} ms")
          appendLine()
          resultsForRun.forEach { result ->
            appendLine(
              "${result.serverId} | ${result.urlSnapshot} | ${result.status.name} | latency=${result.primaryLatencyMs ?: "-"}ms | mbps=${result.downloadMbps?.let {
                "%.2f"
                  .format(
                    Locale.US,
                    it,
                  )
              } ?: "-"}",
            )
          }
        }
    }

  private fun exportEndpointsCsv(
    endpoints: List<ServerEndpoint>,
    latestResults: Map<String, ServerTestResult>,
    favorites: Map<String, FavoriteInfo>,
  ): String {
    val sb = StringBuilder()
    sb.appendLine(
      "id,sourceType,sourceIndex,name,originalUrl,host,explicitPort,protocol,category,verificationState,isPrivateIp,isFavorite,isPinned,latestStatus,primaryLatencyMs,downloadMbps,transferredBytes,speedTestState,networkProfileLabel",
    )
    endpoints.forEach { ep ->
      val latest = latestResults[ep.id]
      val fav = favorites[ep.id]
      val row =
        listOf(
          ep.id,
          ep.sourceType.name,
          ep.sourceIndex?.toString().orEmpty(),
          ep.name,
          ep.originalUrl,
          ep.host,
          ep.explicitPort?.toString().orEmpty(),
          ep.protocol.name,
          ep.category.name,
          ep.verificationState.name,
          ep.isPrivateIp.toString(),
          (fav != null).toString(),
          (fav?.isPinned == true).toString(),
          latest?.status?.name.orEmpty(),
          latest?.primaryLatencyMs?.toString().orEmpty(),
          latest?.downloadMbps?.let { "%.3f".format(Locale.US, it) }.orEmpty(),
          latest?.transferredBytes?.toString().orEmpty(),
          latest?.speedTestState?.name.orEmpty(),
          latest?.networkProfileLabel.orEmpty(),
        ).joinToString(",") { neutralizeCsvCell(it) }
      sb.appendLine(row)
    }
    return sb.toString()
  }

  private fun exportEndpointsJson(
    endpoints: List<ServerEndpoint>,
    latestResults: Map<String, ServerTestResult>,
    favorites: Map<String, FavoriteInfo>,
    dataset: ExportDataset,
  ): String {
    val items = JSONArray()
    endpoints.forEach { ep ->
      val latest = latestResults[ep.id]
      val fav = favorites[ep.id]
      items.put(
        JSONObject().apply {
          put("id", ep.id)
          put("sourceType", ep.sourceType.name)
          put("sourceIndex", ep.sourceIndex ?: JSONObject.NULL)
          put("name", ep.name)
          put("originalUrl", ep.originalUrl)
          put("host", ep.host)
          put("explicitPort", ep.explicitPort ?: JSONObject.NULL)
          put("protocol", ep.protocol.name)
          put("category", ep.category.name)
          put("isFeatured", ep.isFeatured)
          put("verificationState", ep.verificationState.name)
          put("isPrivateIp", ep.isPrivateIp)
          put("isFavorite", fav != null)
          put("isPinned", fav?.isPinned == true)
          if (latest != null) {
            put("latestResult", historyItemToJson(latest))
          } else {
            put("latestResult", JSONObject.NULL)
          }
        },
      )
    }
    return JSONObject()
      .apply {
        put("dataset", dataset.name)
        put("count", endpoints.size)
        put("items", items)
      }.toString(2)
  }

  private fun exportEndpointsTxt(
    endpoints: List<ServerEndpoint>,
    latestResults: Map<String, ServerTestResult>,
    favorites: Map<String, FavoriteInfo>,
    dataset: ExportDataset,
  ): String =
    buildString {
      appendLine("BDIX Tester Export — ${dataset.name} (${endpoints.size} items)")
      endpoints.forEach { ep ->
        val latest = latestResults[ep.id]
        val favMark =
          if (favorites[ep.id]?.isPinned ==
            true
          ) {
            "[PIN]"
          } else if (favorites.containsKey(ep.id)) {
            "[FAV]"
          } else {
            ""
          }
        appendLine(
          "${ep.id} $favMark | ${ep.name} | ${ep.originalUrl} | ${ep.protocol.name} | ${ep.category.name} | status=${latest?.status?.name ?: "NOT_TESTED"} | latency=${latest?.primaryLatencyMs ?: "-"}ms",
        )
      }
    }

  private fun exportHistoryCsv(history: List<ServerTestResult>): String {
    val sb = StringBuilder()
    sb.appendLine(
      "historyId,serverId,scanRunId,probeGroupId,isCoalescedReplica,serverNameSnapshot,urlSnapshot,testedAtEpochMs,scanMode,protocol,status,errorClassification,dnsTimeMs,tcpConnectTimeMs,tlsHandshakeTimeMs,httpResponseLatencyMs,ftpConnectLatencyMs,ftpGreetingCode,httpStatusCode,finalUrl,downloadMbps,downloadMibPerSec,transferredBytes,speedElapsedNanos,speedTestState,speedTerminationReason,networkProfileLabel,isQuickScanInconclusive,failureMessage",
    )
    history.forEach { item ->
      val row =
        listOf(
          item.historyId.toString(),
          item.serverId,
          item.scanRunId.orEmpty(),
          item.probeGroupId.orEmpty(),
          item.isCoalescedReplica.toString(),
          item.serverNameSnapshot,
          item.urlSnapshot,
          item.testedAtEpochMs.toString(),
          item.scanMode.name,
          item.protocol.name,
          item.status.name,
          item.errorClassification.name,
          item.dnsTimeMs?.toString().orEmpty(),
          item.tcpConnectTimeMs?.toString().orEmpty(),
          item.tlsHandshakeTimeMs?.toString().orEmpty(),
          item.httpResponseLatencyMs?.toString().orEmpty(),
          item.ftpConnectLatencyMs?.toString().orEmpty(),
          item.ftpGreetingCode?.toString().orEmpty(),
          item.httpStatusCode?.toString().orEmpty(),
          item.finalUrl.orEmpty(),
          item.downloadMbps?.let { "%.3f".format(Locale.US, it) }.orEmpty(),
          item.downloadMibPerSec?.let { "%.3f".format(Locale.US, it) }.orEmpty(),
          item.transferredBytes?.toString().orEmpty(),
          item.speedElapsedNanos?.toString().orEmpty(),
          item.speedTestState?.name.orEmpty(),
          item.speedTerminationReason?.name.orEmpty(),
          item.networkProfileLabel.orEmpty(),
          item.isQuickScanInconclusive.toString(),
          item.failureMessage.orEmpty(),
        ).joinToString(",") { neutralizeCsvCell(it) }
      sb.appendLine(row)
    }
    return sb.toString()
  }

  private fun exportHistoryJson(history: List<ServerTestResult>): String =
    JSONObject()
      .apply {
        put("dataset", ExportDataset.HISTORY.name)
        put("count", history.size)
        put("items", buildHistoryJsonArray(history))
      }.toString(2)

  private fun buildHistoryJsonArray(history: List<ServerTestResult>): JSONArray {
    val array = JSONArray()
    history.forEach { array.put(historyItemToJson(it)) }
    return array
  }

  private fun historyItemToJson(item: ServerTestResult): JSONObject =
    JSONObject().apply {
      put("historyId", item.historyId)
      put("serverId", item.serverId)
      put("scanRunId", item.scanRunId ?: JSONObject.NULL)
      put("probeGroupId", item.probeGroupId ?: JSONObject.NULL)
      put("isCoalescedReplica", item.isCoalescedReplica)
      put("serverNameSnapshot", item.serverNameSnapshot)
      put("urlSnapshot", item.urlSnapshot)
      put("testedAtEpochMs", item.testedAtEpochMs)
      put("scanMode", item.scanMode.name)
      put("protocol", item.protocol.name)
      put("status", item.status.name)
      put("errorClassification", item.errorClassification.name)
      put("dnsTimeMs", item.dnsTimeMs ?: JSONObject.NULL)
      put("tcpConnectTimeMs", item.tcpConnectTimeMs ?: JSONObject.NULL)
      put("tlsHandshakeTimeMs", item.tlsHandshakeTimeMs ?: JSONObject.NULL)
      put("httpResponseLatencyMs", item.httpResponseLatencyMs ?: JSONObject.NULL)
      put("ftpConnectLatencyMs", item.ftpConnectLatencyMs ?: JSONObject.NULL)
      put("ftpGreetingCode", item.ftpGreetingCode ?: JSONObject.NULL)
      put("httpStatusCode", item.httpStatusCode ?: JSONObject.NULL)
      put("finalUrl", item.finalUrl ?: JSONObject.NULL)
      put("downloadMbps", item.downloadMbps ?: JSONObject.NULL)
      put("downloadMibPerSec", item.downloadMibPerSec ?: JSONObject.NULL)
      put("transferredBytes", item.transferredBytes ?: JSONObject.NULL)
      put("speedElapsedNanos", item.speedElapsedNanos ?: JSONObject.NULL)
      put("speedTestState", item.speedTestState?.name ?: JSONObject.NULL)
      put("speedTerminationReason", item.speedTerminationReason?.name ?: JSONObject.NULL)
      put("networkProfileLabel", item.networkProfileLabel ?: JSONObject.NULL)
      put("isQuickScanInconclusive", item.isQuickScanInconclusive)
      put("failureMessage", item.failureMessage ?: JSONObject.NULL)
    }

  private fun exportHistoryTxt(history: List<ServerTestResult>): String =
    buildString {
      appendLine("BDIX Tester History Export (${history.size} records)")
      history.forEach { item ->
        appendLine(
          "${item.testedAtEpochMs} | ${item.serverId} | ${item.urlSnapshot} | ${item.scanMode.name} | ${item.status.name} | latency=${item.primaryLatencyMs ?: "-"}ms | mbps=${item.downloadMbps?.let {
            "%.2f"
              .format(
                Locale.US,
                it,
              )
          } ?: "-"} | label=${item.networkProfileLabel ?: "-"}",
        )
      }
    }

  private val FORMULA_TRIGGERS = charArrayOf('=', '+', '-', '@', '\t', '\r')
  private val FORMULA_TRIGGERS_TRIMMED = charArrayOf('=', '+', '-', '@')
}
