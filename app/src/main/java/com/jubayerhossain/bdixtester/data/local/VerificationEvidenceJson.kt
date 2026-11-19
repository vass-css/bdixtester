package com.jubayerhossain.bdixtester.data.local

import com.jubayerhossain.bdixtester.core.model.BdIspProbeRecord
import com.jubayerhossain.bdixtester.core.model.DnsEvidence
import com.jubayerhossain.bdixtester.core.model.DnsSummary
import com.jubayerhossain.bdixtester.core.model.FtpProbeEvidence
import com.jubayerhossain.bdixtester.core.model.HttpProbeEvidence
import com.jubayerhossain.bdixtester.core.model.RegistrationEvidence
import com.jubayerhossain.bdixtester.core.model.RegistrationLookupStatus
import com.jubayerhossain.bdixtester.core.model.ResolverProbeResult
import org.json.JSONArray
import org.json.JSONObject

internal data class StoredEvidencePayload(
  val observedEvidence: List<String> = emptyList(),
  val dns: DnsEvidence? = null,
  val registration: RegistrationEvidence? = null,
  val http: HttpProbeEvidence? = null,
  val ftp: FtpProbeEvidence? = null,
  val ispProbes: List<BdIspProbeRecord> = emptyList(),
)

internal object VerificationEvidenceJson {
  fun encode(payload: StoredEvidencePayload): String {
    val root = JSONObject()
    root.put("observed", JSONArray(payload.observedEvidence))
    payload.dns?.let { d ->
      root.put(
        "dns",
        JSONObject().apply {
          put("domain", d.domain)
          put("summary", d.summary.wireName)
          put("checkedAtEpochMs", d.checkedAtEpochMs)
          put("nsRecords", JSONArray(d.nsRecords))
          put("soaRecords", JSONArray(d.soaRecords))
          put("resolverResults", encodeResolvers(d.resolverResults))
          put("authoritativeResults", encodeResolvers(d.authoritativeResults))
        },
      )
    }
    payload.registration?.let { r ->
      root.put(
        "registration",
        JSONObject().apply {
          put("domain", r.domain)
          put("lookupStatus", r.lookupStatus.wireName)
          put("authoritativeSource", r.authoritativeSource ?: JSONObject.NULL)
          put("eppStatuses", JSONArray(r.eppStatuses))
          put("registrar", r.registrar ?: JSONObject.NULL)
          put("createdAtIso", r.createdAtIso ?: JSONObject.NULL)
          put("expiresAtIso", r.expiresAtIso ?: JSONObject.NULL)
          put("updatedAtIso", r.updatedAtIso ?: JSONObject.NULL)
          put("limitationNote", r.limitationNote ?: JSONObject.NULL)
          put("checkedAtEpochMs", r.checkedAtEpochMs)
        },
      )
    }
    payload.http?.let { h ->
      root.put(
        "http",
        JSONObject().apply {
          put("outcome", h.outcome)
          put("statusCode", h.statusCode ?: JSONObject.NULL)
          put("finalUrl", h.finalUrl ?: JSONObject.NULL)
          put("redirectChain", JSONArray(h.redirectChain))
          put("responseTimeMs", h.responseTimeMs ?: JSONObject.NULL)
          put("tlsError", h.tlsError ?: JSONObject.NULL)
          put("errorDetail", h.errorDetail ?: JSONObject.NULL)
          put("checkedAtEpochMs", h.checkedAtEpochMs)
        },
      )
    }
    payload.ftp?.let { f ->
      root.put(
        "ftp",
        JSONObject().apply {
          put("outcome", f.outcome)
          put("greetingCode", f.greetingCode ?: JSONObject.NULL)
          put("bannerExcerpt", f.bannerExcerpt ?: JSONObject.NULL)
          put("responseTimeMs", f.responseTimeMs ?: JSONObject.NULL)
          put("errorDetail", f.errorDetail ?: JSONObject.NULL)
          put("checkedAtEpochMs", f.checkedAtEpochMs)
        },
      )
    }
    if (payload.ispProbes.isNotEmpty()) {
      val arr = JSONArray()
      payload.ispProbes.forEach { p ->
        arr.put(
          JSONObject().apply {
            put("ispId", p.ispId)
            put("ispName", p.ispName)
            put("region", p.region)
            put("probedAtUtc", p.probedAtUtc)
            put("endpointUrl", p.endpointUrl)
            put("protocol", p.protocol)
            put("dnsResult", p.dnsResult)
            put("connectionResult", p.connectionResult)
            put("httpOrFtpResult", p.httpOrFtpResult)
            put("responseTimeMs", p.responseTimeMs ?: JSONObject.NULL)
            put("errorInfo", p.errorInfo ?: JSONObject.NULL)
            put("probeSoftwareVersion", p.probeSoftwareVersion)
            put("isStale", p.isStale)
          },
        )
      }
      root.put("ispProbes", arr)
    }
    return root.toString()
  }

  fun decode(rawJson: String?): StoredEvidencePayload {
    if (rawJson.isNullOrBlank()) return StoredEvidencePayload()
    return runCatching {
      val root = JSONObject(rawJson)
      val observed = decodeStringArray(root.optJSONArray("observed"))
      val dnsObj = root.optJSONObject("dns")
      val dns =
        dnsObj?.let { d ->
          DnsEvidence(
            domain = d.optString("domain"),
            resolverResults = decodeResolvers(d.optJSONArray("resolverResults")),
            authoritativeResults = decodeResolvers(d.optJSONArray("authoritativeResults")),
            nsRecords = decodeStringArray(d.optJSONArray("nsRecords")),
            soaRecords = decodeStringArray(d.optJSONArray("soaRecords")),
            summary = DnsSummary.fromWire(d.optString("summary")),
            checkedAtEpochMs = d.optLong("checkedAtEpochMs", 0L),
          )
        }
      val regObj = root.optJSONObject("registration")
      val registration =
        regObj?.let { r ->
          RegistrationEvidence(
            domain = r.optString("domain"),
            lookupStatus = RegistrationLookupStatus.fromWire(r.optString("lookupStatus")),
            authoritativeSource = r.optNullableString("authoritativeSource"),
            eppStatuses = decodeStringArray(r.optJSONArray("eppStatuses")),
            registrar = r.optNullableString("registrar"),
            createdAtIso = r.optNullableString("createdAtIso"),
            expiresAtIso = r.optNullableString("expiresAtIso"),
            updatedAtIso = r.optNullableString("updatedAtIso"),
            limitationNote = r.optNullableString("limitationNote"),
            checkedAtEpochMs = r.optLong("checkedAtEpochMs", 0L),
          )
        }
      val httpObj = root.optJSONObject("http")
      val http =
        httpObj?.let { h ->
          HttpProbeEvidence(
            outcome = h.optString("outcome"),
            statusCode = if (h.has("statusCode") && !h.isNull("statusCode")) h.optInt("statusCode") else null,
            finalUrl = h.optNullableString("finalUrl"),
            redirectChain = decodeStringArray(h.optJSONArray("redirectChain")),
            responseTimeMs = if (h.has("responseTimeMs") && !h.isNull("responseTimeMs")) h.optLong("responseTimeMs") else null,
            tlsError = h.optNullableString("tlsError"),
            errorDetail = h.optNullableString("errorDetail"),
            checkedAtEpochMs = h.optLong("checkedAtEpochMs", 0L),
          )
        }
      val ftpObj = root.optJSONObject("ftp")
      val ftp =
        ftpObj?.let { f ->
          FtpProbeEvidence(
            outcome = f.optString("outcome"),
            greetingCode = if (f.has("greetingCode") && !f.isNull("greetingCode")) f.optInt("greetingCode") else null,
            bannerExcerpt = f.optNullableString("bannerExcerpt"),
            responseTimeMs = if (f.has("responseTimeMs") && !f.isNull("responseTimeMs")) f.optLong("responseTimeMs") else null,
            errorDetail = f.optNullableString("errorDetail"),
            checkedAtEpochMs = f.optLong("checkedAtEpochMs", 0L),
          )
        }
      val probes = mutableListOf<BdIspProbeRecord>()
      root.optJSONArray("ispProbes")?.let { arr ->
        for (i in 0 until arr.length()) {
          val p = arr.optJSONObject(i) ?: continue
          probes +=
            BdIspProbeRecord(
              ispId = p.optString("ispId"),
              ispName = p.optString("ispName"),
              region = p.optString("region"),
              probedAtUtc = p.optString("probedAtUtc"),
              endpointUrl = p.optString("endpointUrl"),
              protocol = p.optString("protocol"),
              dnsResult = p.optString("dnsResult"),
              connectionResult = p.optString("connectionResult"),
              httpOrFtpResult = p.optString("httpOrFtpResult"),
              responseTimeMs = if (p.has("responseTimeMs") && !p.isNull("responseTimeMs")) p.optLong("responseTimeMs") else null,
              errorInfo = p.optNullableString("errorInfo"),
              probeSoftwareVersion = p.optString("probeSoftwareVersion", "unknown"),
              isStale = p.optBoolean("isStale", false),
            )
        }
      }
      StoredEvidencePayload(
        observedEvidence = observed,
        dns = dns,
        registration = registration,
        http = http,
        ftp = ftp,
        ispProbes = probes,
      )
    }.getOrElse { StoredEvidencePayload() }
  }

  private fun encodeResolvers(list: List<ResolverProbeResult>): JSONArray {
    val arr = JSONArray()
    list.forEach { r ->
      arr.put(
        JSONObject().apply {
          put("resolverName", r.resolverName)
          put("resolverEndpoint", r.resolverEndpoint)
          put("recordType", r.recordType)
          put("rcode", r.rcode)
          put("answers", JSONArray(r.answers))
          put("ttlSeconds", r.ttlSeconds ?: JSONObject.NULL)
          put("error", r.error ?: JSONObject.NULL)
          put("checkedAtEpochMs", r.checkedAtEpochMs)
        },
      )
    }
    return arr
  }

  private fun decodeResolvers(arr: JSONArray?): List<ResolverProbeResult> {
    if (arr == null) return emptyList()
    val out = mutableListOf<ResolverProbeResult>()
    for (i in 0 until arr.length()) {
      val obj = arr.optJSONObject(i) ?: continue
      out +=
        ResolverProbeResult(
          resolverName = obj.optString("resolverName"),
          resolverEndpoint = obj.optString("resolverEndpoint"),
          recordType = obj.optString("recordType"),
          rcode = obj.optString("rcode"),
          answers = decodeStringArray(obj.optJSONArray("answers")),
          ttlSeconds = if (obj.has("ttlSeconds") && !obj.isNull("ttlSeconds")) obj.optInt("ttlSeconds") else null,
          error = obj.optNullableString("error"),
          checkedAtEpochMs = obj.optLong("checkedAtEpochMs", 0L),
        )
    }
    return out
  }

  private fun decodeStringArray(arr: JSONArray?): List<String> {
    if (arr == null) return emptyList()
    val out = mutableListOf<String>()
    for (i in 0 until arr.length()) {
      val item = arr.optString(i).trim()
      if (item.isNotEmpty()) out += item
    }
    return out
  }

  private fun JSONObject.optNullableString(key: String): String? =
    if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
}
