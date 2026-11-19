package com.jubayerhossain.bdixtester.data.verification

import com.jubayerhossain.bdixtester.core.model.BdIspProbeRecord
import com.jubayerhossain.bdixtester.core.validation.UrlParser
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class IspProbeLoadResult(
  val totalParsed: Int,
  val freshCount: Int,
  val staleCount: Int,
  val probesByCanonicalUrl: Map<String, List<BdIspProbeRecord>>,
)

object IspProbeJsonParser {
  private val REQUIRED_FIELDS =
    listOf(
      "isp_id",
      "isp_name",
      "region",
      "probed_at_utc",
      "endpoint_url",
      "protocol",
      "dns_result",
      "connection_result",
      "http_or_ftp_result",
    )

  fun parseJson(
    rawJson: String,
    freshnessTtlHours: Int = 168,
    nowEpochMs: Long = System.currentTimeMillis(),
  ): Result<IspProbeLoadResult> =
    runCatching {
      val trimmed = rawJson.trim()
      val itemsArray: JSONArray =
        if (trimmed.startsWith("[")) {
          JSONArray(trimmed)
        } else {
          val obj = JSONObject(trimmed)
          obj.optJSONArray("probes")
            ?: throw IllegalArgumentException("Expected top-level 'probes' array or JSON array.")
        }

      val cutoffMs = nowEpochMs - freshnessTtlHours.coerceAtLeast(1) * 3_600_000L
      val grouped = mutableMapOf<String, MutableList<BdIspProbeRecord>>()
      var fresh = 0
      var stale = 0

      for (i in 0 until itemsArray.length()) {
        val obj =
          itemsArray.optJSONObject(i)
            ?: throw IllegalArgumentException("Probe entry #$i is not a JSON object.")
        for (field in REQUIRED_FIELDS) {
          if (!obj.has(field) || obj.isNull(field) || obj.optString(field).isBlank()) {
            throw IllegalArgumentException("Probe entry #$i missing required field '$field'.")
          }
        }
        val probedAtUtc = obj.getString("probed_at_utc").trim()
        val probedMs =
          parseUtcIsoMillis(probedAtUtc)
            ?: throw IllegalArgumentException("Probe entry #$i has invalid UTC ISO-8601 timestamp '$probedAtUtc'.")
        val isStale = probedMs < cutoffMs
        if (isStale) stale++ else fresh++

        val endpointUrl = obj.getString("endpoint_url").trim()
        val canonicalKey = UrlParser.canonicalKey(endpointUrl)
        val record =
          BdIspProbeRecord(
            ispId = obj.getString("isp_id").trim(),
            ispName = obj.getString("isp_name").trim(),
            region = obj.getString("region").trim(),
            probedAtUtc = probedAtUtc,
            endpointUrl = endpointUrl,
            protocol = obj.getString("protocol").trim(),
            dnsResult = obj.getString("dns_result").trim(),
            connectionResult = obj.getString("connection_result").trim().lowercase(),
            httpOrFtpResult = obj.getString("http_or_ftp_result").trim(),
            responseTimeMs = if (obj.has("response_time_ms") && !obj.isNull("response_time_ms")) obj.optLong("response_time_ms") else null,
            errorInfo = obj.optString("error_info").takeIf { it.isNotBlank() && it != "null" },
            probeSoftwareVersion = obj.optString("probe_software_version").ifBlank { "unknown" },
            isStale = isStale,
          )
        grouped.getOrPut(canonicalKey) { mutableListOf() }.add(record)
      }

      IspProbeLoadResult(
        totalParsed = fresh + stale,
        freshCount = fresh,
        staleCount = stale,
        probesByCanonicalUrl = grouped,
      )
    }

  private fun parseUtcIsoMillis(iso: String): Long? {
    val clean = iso.trim().replace("+00:00", "Z")
    val patterns = listOf("yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    for (p in patterns) {
      val parsed =
        runCatching {
          SimpleDateFormat(p, Locale.US)
            .apply {
              timeZone = TimeZone.getTimeZone("UTC")
              isLenient = false
            }.parse(clean)
        }.getOrNull()
      if (parsed != null) return parsed.time
    }
    return null
  }
}
