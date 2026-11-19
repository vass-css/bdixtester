package com.jubayerhossain.bdixtester.data.verification

import com.jubayerhossain.bdixtester.core.model.DnsEvidence
import com.jubayerhossain.bdixtester.core.model.DnsSummary
import com.jubayerhossain.bdixtester.core.model.ResolverProbeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

data class DohResolverEndpoint(
  val name: String,
  val jsonEndpointUrl: String,
)

class DohDnsResolver(
  baseClient: OkHttpClient = OkHttpClient(),
  private val resolvers: List<DohResolverEndpoint> = DEFAULT_RESOLVERS,
  private val clock: () -> Long = { System.currentTimeMillis() },
) {
  private val client =
    baseClient
      .newBuilder()
      .connectTimeout(5, TimeUnit.SECONDS)
      .readTimeout(5, TimeUnit.SECONDS)
      .callTimeout(7, TimeUnit.SECONDS)
      .followRedirects(false)
      .build()

  suspend fun lookupDomain(domain: String): DnsEvidence =
    withContext(Dispatchers.IO) {
      val now = clock()
      val resolverResults = mutableListOf<ResolverProbeResult>()
      for (resolver in resolvers) {
        resolverResults += querySingleResolver(resolver, domain, "A", now)
      }

      val nsResult = resolvers.firstOrNull()?.let { querySingleResolver(it, domain, "NS", now) }
      val nsRecords = nsResult?.answers?.distinct().orEmpty()
      val soaResult = resolvers.firstOrNull()?.let { querySingleResolver(it, domain, "SOA", now) }
      val soaRecords = soaResult?.answers?.distinct().orEmpty()

      val summary = summarizeResults(resolverResults)
      DnsEvidence(
        domain = domain,
        resolverResults = resolverResults,
        authoritativeResults = emptyList(),
        nsRecords = nsRecords,
        soaRecords = soaRecords,
        summary = summary,
        checkedAtEpochMs = now,
      )
    }

  private fun querySingleResolver(
    resolver: DohResolverEndpoint,
    domain: String,
    recordType: String,
    checkedAtMs: Long,
  ): ResolverProbeResult {
    val parsedUrl =
      resolver.jsonEndpointUrl
        .toHttpUrlOrNull()
        ?.newBuilder()
        ?.addQueryParameter("name", domain)
        ?.addQueryParameter("type", recordType)
        ?.build()
        ?: return ResolverProbeResult(
          resolverName = resolver.name,
          resolverEndpoint = resolver.jsonEndpointUrl,
          recordType = recordType,
          rcode = "ERROR",
          error = "Invalid DoH endpoint URL",
          checkedAtEpochMs = checkedAtMs,
        )

    val req =
      Request
        .Builder()
        .url(parsedUrl)
        .header("Accept", "application/dns-json")
        .header("User-Agent", "BDIX-Tester/1.0")
        .get()
        .build()

    return try {
      client.newCall(req).execute().use { res ->
        if (!res.isSuccessful) {
          return ResolverProbeResult(
            resolverName = resolver.name,
            resolverEndpoint = resolver.jsonEndpointUrl,
            recordType = recordType,
            rcode = "ERROR",
            error = "HTTP ${res.code}",
            checkedAtEpochMs = checkedAtMs,
          )
        }
        val bodyStr = res.body.string()
        val json = JSONObject(bodyStr)
        val statusInt = json.optInt("Status", -1)
        val rcode =
          when (statusInt) {
            0 -> "NOERROR"
            2 -> "SERVFAIL"
            3 -> "NXDOMAIN"
            5 -> "REFUSED"
            else -> "ERROR"
          }
        val answersJson = json.optJSONArray("Answer")
        val answers = mutableListOf<String>()
        var minTtl: Int? = null
        if (answersJson != null) {
          for (i in 0 until answersJson.length()) {
            val item = answersJson.optJSONObject(i) ?: continue
            val data = item.optString("data").trim()
            if (data.isNotEmpty()) {
              answers += data
            }
            if (item.has("TTL")) {
              val ttl = item.optInt("TTL")
              minTtl = if (minTtl == null) ttl else minOf(minTtl, ttl)
            }
          }
        }
        ResolverProbeResult(
          resolverName = resolver.name,
          resolverEndpoint = resolver.jsonEndpointUrl,
          recordType = recordType,
          rcode = rcode,
          answers = answers,
          ttlSeconds = minTtl,
          checkedAtEpochMs = checkedAtMs,
        )
      }
    } catch (_: SocketTimeoutException) {
      ResolverProbeResult(
        resolverName = resolver.name,
        resolverEndpoint = resolver.jsonEndpointUrl,
        recordType = recordType,
        rcode = "TIMEOUT",
        error = "Resolver timed out",
        checkedAtEpochMs = checkedAtMs,
      )
    } catch (e: IOException) {
      ResolverProbeResult(
        resolverName = resolver.name,
        resolverEndpoint = resolver.jsonEndpointUrl,
        recordType = recordType,
        rcode = "ERROR",
        error = e.message ?: "Network error",
        checkedAtEpochMs = checkedAtMs,
      )
    } catch (e: Exception) {
      ResolverProbeResult(
        resolverName = resolver.name,
        resolverEndpoint = resolver.jsonEndpointUrl,
        recordType = recordType,
        rcode = "ERROR",
        error = e.message ?: "Malformed DoH response",
        checkedAtEpochMs = checkedAtMs,
      )
    }
  }

  companion object {
    val DEFAULT_RESOLVERS =
      listOf(
        DohResolverEndpoint("Cloudflare", "https://cloudflare-dns.com/dns-query"),
        DohResolverEndpoint("Google", "https://dns.google/resolve"),
        DohResolverEndpoint("Quad9", "https://dns9.quad9.net:5053/dns-query"),
      )

    fun summarizeResults(results: List<ResolverProbeResult>): DnsSummary {
      if (results.isEmpty()) return DnsSummary.NOT_CHECKED
      val nxCount = results.count { it.rcode == "NXDOMAIN" }
      val noErrorWithAnswers = results.count { it.rcode == "NOERROR" && it.answers.isNotEmpty() }
      val noErrorEmpty = results.count { it.rcode == "NOERROR" && it.answers.isEmpty() }
      val servfailCount = results.count { it.rcode == "SERVFAIL" || it.rcode == "REFUSED" }
      val timeoutCount = results.count { it.rcode == "TIMEOUT" || it.rcode == "ERROR" }

      if (noErrorWithAnswers > 0 && nxCount > 0) {
        return DnsSummary.DISAGREED
      }
      if (noErrorWithAnswers > 0) {
        return DnsSummary.HEALTHY
      }
      if (noErrorEmpty > 0 && nxCount == 0) {
        return DnsSummary.NODATA
      }
      if (nxCount >= 2 && servfailCount == 0 && noErrorWithAnswers == 0 && noErrorEmpty == 0) {
        return DnsSummary.MULTI_NXDOMAIN
      }
      if (nxCount >= 1) {
        return if (noErrorEmpty > 0 || noErrorWithAnswers > 0) {
          DnsSummary.DISAGREED
        } else {
          DnsSummary.SINGLE_NXDOMAIN
        }
      }
      if (servfailCount > 0) {
        return DnsSummary.SERVFAIL
      }
      if (timeoutCount == results.size) {
        return DnsSummary.TIMEOUT
      }
      return DnsSummary.NOT_CHECKED
    }
  }
}
