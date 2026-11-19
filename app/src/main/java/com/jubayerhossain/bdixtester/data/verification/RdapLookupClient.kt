package com.jubayerhossain.bdixtester.data.verification

import com.jubayerhossain.bdixtester.core.model.RegistrationEvidence
import com.jubayerhossain.bdixtester.core.model.RegistrationLookupStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class RdapLookupClient(
  baseClient: OkHttpClient = OkHttpClient(),
  private val bootstrapUrl: String = IANA_RDAP_BOOTSTRAP_URL,
  private val clock: () -> Long = { System.currentTimeMillis() },
) {
  private val client =
    baseClient
      .newBuilder()
      .connectTimeout(5, TimeUnit.SECONDS)
      .readTimeout(6, TimeUnit.SECONDS)
      .callTimeout(8, TimeUnit.SECONDS)
      .followRedirects(true)
      .followSslRedirects(false)
      .build()

  @Volatile private var cachedBootstrap: Map<String, String>? = null

  suspend fun lookupDomain(domain: String): RegistrationEvidence =
    withContext(Dispatchers.IO) {
      val now = clock()
      val cleanDomain = domain.trim().trimEnd('.').lowercase()

      if (DomainExtractor.isDotBdDomain(cleanDomain)) {
        return@withContext RegistrationEvidence(
          domain = cleanDomain,
          lookupStatus = RegistrationLookupStatus.REGISTRATION_LOOKUP_UNAVAILABLE,
          limitationNote = ".bd / BTCL registry has no public ICANN RDAP bootstrap endpoint; manual check on BTCL portal required.",
          checkedAtEpochMs = now,
        )
      }

      val tld = cleanDomain.substringAfterLast('.', "")
      if (tld.isEmpty()) {
        return@withContext RegistrationEvidence(
          domain = cleanDomain,
          lookupStatus = RegistrationLookupStatus.NOT_APPLICABLE,
          checkedAtEpochMs = now,
        )
      }

      val bootstrap = loadBootstrapOrNull()
      if (bootstrap == null) {
        return@withContext RegistrationEvidence(
          domain = cleanDomain,
          lookupStatus = RegistrationLookupStatus.LOOKUP_ERROR,
          limitationNote = "Failed to fetch IANA RDAP bootstrap index.",
          checkedAtEpochMs = now,
        )
      }

      val baseUrl = bootstrap[tld]
      if (baseUrl == null) {
        return@withContext RegistrationEvidence(
          domain = cleanDomain,
          lookupStatus = RegistrationLookupStatus.REGISTRATION_LOOKUP_UNAVAILABLE,
          limitationNote = "TLD '.$tld' is not listed in IANA RDAP bootstrap.",
          checkedAtEpochMs = now,
        )
      }

      val queryUrl = "${baseUrl.trimEnd('/')}/domain/$cleanDomain"
      val req =
        Request
          .Builder()
          .url(queryUrl)
          .header("Accept", "application/rdap+json, application/json")
          .header("User-Agent", "BDIX-Tester/1.0")
          .get()
          .build()

      try {
        client.newCall(req).execute().use { res ->
          when (res.code) {
            404 ->
              RegistrationEvidence(
                domain = cleanDomain,
                lookupStatus = RegistrationLookupStatus.UNREGISTERED_404,
                authoritativeSource = queryUrl,
                checkedAtEpochMs = now,
              )
            200 -> parseRdapSuccess(cleanDomain, queryUrl, res.body.string(), now)
            else ->
              RegistrationEvidence(
                domain = cleanDomain,
                lookupStatus = RegistrationLookupStatus.LOOKUP_ERROR,
                authoritativeSource = queryUrl,
                limitationNote = "RDAP returned HTTP ${res.code}.",
                checkedAtEpochMs = now,
              )
          }
        }
      } catch (e: IOException) {
        RegistrationEvidence(
          domain = cleanDomain,
          lookupStatus = RegistrationLookupStatus.LOOKUP_ERROR,
          authoritativeSource = queryUrl,
          limitationNote = e.message ?: "RDAP network failure.",
          checkedAtEpochMs = now,
        )
      }
    }

  private fun parseRdapSuccess(
    domain: String,
    sourceUrl: String,
    rawBody: String,
    nowEpochMs: Long,
  ): RegistrationEvidence {
    val json =
      runCatching { JSONObject(rawBody) }.getOrNull()
        ?: return RegistrationEvidence(
          domain = domain,
          lookupStatus = RegistrationLookupStatus.LOOKUP_ERROR,
          authoritativeSource = sourceUrl,
          limitationNote = "Malformed RDAP JSON payload.",
          checkedAtEpochMs = nowEpochMs,
        )

    val statuses = mutableListOf<String>()
    json.optJSONArray("status")?.let { arr ->
      for (i in 0 until arr.length()) {
        val v = arr.optString(i).trim()
        if (v.isNotEmpty()) statuses += v
      }
    }

    var createdIso: String? = null
    var expiresIso: String? = null
    var updatedIso: String? = null
    json.optJSONArray("events")?.let { events ->
      for (i in 0 until events.length()) {
        val ev = events.optJSONObject(i) ?: continue
        val action = ev.optString("eventAction").lowercase()
        val date = ev.optString("eventDate").takeIf { it.isNotBlank() }
        when (action) {
          "registration" -> createdIso = date
          "expiration" -> expiresIso = date
          "last changed", "last update of rdap database" -> if (updatedIso == null) updatedIso = date
        }
      }
    }

    val registrar = extractRegistrar(json.optJSONArray("entities"))
    val normalizedStatuses = statuses.map { it.lowercase() }

    val lookupStatus =
      when {
        normalizedStatuses.any { "pending delete" in it || "pendingdelete" in it } ->
          RegistrationLookupStatus.PENDING_DELETE
        normalizedStatuses.any { "redemption" in it } ->
          RegistrationLookupStatus.REDEMPTION_PERIOD
        normalizedStatuses.any { "hold" in it } ->
          RegistrationLookupStatus.HOLD
        isTimestampPast(expiresIso, nowEpochMs) ->
          RegistrationLookupStatus.EXPIRED_GRACE
        else -> RegistrationLookupStatus.REGISTERED
      }

    return RegistrationEvidence(
      domain = domain,
      lookupStatus = lookupStatus,
      authoritativeSource = sourceUrl,
      eppStatuses = statuses,
      registrar = registrar,
      createdAtIso = createdIso,
      expiresAtIso = expiresIso,
      updatedAtIso = updatedIso,
      checkedAtEpochMs = nowEpochMs,
    )
  }

  private fun extractRegistrar(entities: JSONArray?): String? {
    if (entities == null) return null
    for (i in 0 until entities.length()) {
      val ent = entities.optJSONObject(i) ?: continue
      val roles = ent.optJSONArray("roles") ?: continue
      var isRegistrar = false
      for (r in 0 until roles.length()) {
        if (roles.optString(r).equals("registrar", ignoreCase = true)) {
          isRegistrar = true
          break
        }
      }
      if (!isRegistrar) continue
      val vcard = ent.optJSONArray("vcardArray")?.optJSONArray(1) ?: continue
      for (v in 0 until vcard.length()) {
        val prop = vcard.optJSONArray(v) ?: continue
        if (prop.optString(0).equals("fn", ignoreCase = true)) {
          val fnVal = prop.optString(3).trim()
          if (fnVal.isNotEmpty()) return fnVal
        }
      }
    }
    return null
  }

  private fun loadBootstrapOrNull(): Map<String, String>? {
    cachedBootstrap?.let { return it }
    val req =
      Request
        .Builder()
        .url(bootstrapUrl)
        .header("Accept", "application/json")
        .header("User-Agent", "BDIX-Tester/1.0")
        .get()
        .build()

    val parsed =
      runCatching {
        client.newCall(req).execute().use { res ->
          if (!res.isSuccessful) return@use null
          val root = JSONObject(res.body.string())
          val services = root.optJSONArray("services") ?: return@use null
          val map = mutableMapOf<String, String>()
          for (i in 0 until services.length()) {
            val pair = services.optJSONArray(i) ?: continue
            val tlds = pair.optJSONArray(0) ?: continue
            val urls = pair.optJSONArray(1) ?: continue
            val firstHttps =
              (0 until urls.length())
                .mapNotNull { urls.optString(it).takeIf { u -> u.startsWith("https://") } }
                .firstOrNull()
                ?: urls.optString(0).takeIf { it.isNotBlank() }
                ?: continue
            for (t in 0 until tlds.length()) {
              val tld = tlds.optString(t).trim().lowercase()
              if (tld.isNotEmpty()) {
                map[tld] = firstHttps
              }
            }
          }
          map
        }
      }.getOrNull()

    if (parsed != null) {
      cachedBootstrap = parsed
    }
    return parsed
  }

  private fun isTimestampPast(
    iso: String?,
    nowEpochMs: Long,
  ): Boolean {
    if (iso.isNullOrBlank()) return false
    val formats =
      listOf(
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd",
      )
    for (pattern in formats) {
      val parsed =
        runCatching {
          SimpleDateFormat(pattern, Locale.US)
            .apply {
              timeZone = TimeZone.getTimeZone("UTC")
              isLenient = false
            }.parse(iso.trim())
        }.getOrNull()
      if (parsed != null) {
        return parsed.time < nowEpochMs
      }
    }
    return false
  }

  companion object {
    const val IANA_RDAP_BOOTSTRAP_URL = "https://data.iana.org/rdap/dns.json"
  }
}
