package com.jubayerhossain.bdixtester

import com.jubayerhossain.bdixtester.core.model.BdIspProbeRecord
import com.jubayerhossain.bdixtester.core.model.CatalogSourceType
import com.jubayerhossain.bdixtester.core.model.ConfidenceLevel
import com.jubayerhossain.bdixtester.core.model.DnsEvidence
import com.jubayerhossain.bdixtester.core.model.DnsSummary
import com.jubayerhossain.bdixtester.core.model.ErrorClassification
import com.jubayerhossain.bdixtester.core.model.FtpProbeEvidence
import com.jubayerhossain.bdixtester.core.model.HttpProbeEvidence
import com.jubayerhossain.bdixtester.core.model.PriorGovernanceObservation
import com.jubayerhossain.bdixtester.core.model.RegistrationEvidence
import com.jubayerhossain.bdixtester.core.model.RegistrationLookupStatus
import com.jubayerhossain.bdixtester.core.model.ResolverProbeResult
import com.jubayerhossain.bdixtester.core.model.ScanMode
import com.jubayerhossain.bdixtester.core.model.ServerCategory
import com.jubayerhossain.bdixtester.core.model.ServerProtocol
import com.jubayerhossain.bdixtester.core.model.TestStatus
import com.jubayerhossain.bdixtester.core.model.VerificationLifecycleStatus
import com.jubayerhossain.bdixtester.core.model.VerificationState
import com.jubayerhossain.bdixtester.core.validation.UrlParser
import com.jubayerhossain.bdixtester.data.network.EndpointProbeConfig
import com.jubayerhossain.bdixtester.data.network.HttpEndpointTester
import com.jubayerhossain.bdixtester.data.network.SsrfCheckResult
import com.jubayerhossain.bdixtester.data.network.SsrfGuard
import com.jubayerhossain.bdixtester.data.verification.ConservativeDecisionEngine
import com.jubayerhossain.bdixtester.data.verification.DohDnsResolver
import com.jubayerhossain.bdixtester.data.verification.DohResolverEndpoint
import com.jubayerhossain.bdixtester.data.verification.DomainExtractor
import com.jubayerhossain.bdixtester.data.verification.IspProbeJsonParser
import com.jubayerhossain.bdixtester.data.verification.RdapLookupClient
import com.jubayerhossain.bdixtester.domain.model.ServerEndpoint
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GovernanceDecisionEngineTest {
  private val nowMs = 1_780_000_000_000L

  private fun makeEndpoint(
    url: String = "http://ftp.example.net",
    protocol: ServerProtocol? = null,
    isPrivate: Boolean? = null,
  ): ServerEndpoint {
    val parsed = UrlParser.parse(url)
    return ServerEndpoint(
      id = "test:${url.hashCode()}",
      name = "Test Endpoint",
      originalUrl = url,
      host = parsed.host ?: url,
      explicitPort = parsed.explicitPort,
      protocol = protocol ?: parsed.protocol,
      category = ServerCategory.FTP_FILES,
      isFeatured = false,
      sourceType = CatalogSourceType.BUILT_IN,
      verificationState = VerificationState.UNKNOWN,
      lastReviewedAtEpochMs = null,
      speedTestUrl = null,
      isPrivateIp = isPrivate ?: parsed.isPrivateIp,
    )
  }

  private fun makeIspProbe(
    ispId: String,
    connectionResult: String,
    httpOrFtpResult: String = "200",
    isStale: Boolean = false,
  ) = BdIspProbeRecord(
    ispId = ispId,
    ispName = ispId.uppercase(),
    region = "Dhaka",
    probedAtUtc = "2026-11-18T12:00:00Z",
    endpointUrl = "http://ftp.example.net",
    protocol = "http",
    dnsResult = "NOERROR",
    connectionResult = connectionResult,
    httpOrFtpResult = httpOrFtpResult,
    responseTimeMs = 4L,
    isStale = isStale,
  )

  @Test
  fun unreachableOnCurrentIspWithHealthyDnsNeverMarksDead() {
    val ep = makeEndpoint("http://ftp.example.net")
    val decision =
      ConservativeDecisionEngine.evaluate(
        endpoint = ep,
        dns = DnsEvidence(domain = "example.net", summary = DnsSummary.HEALTHY),
        http = HttpProbeEvidence(outcome = "timeout"),
        nowEpochMs = nowMs,
      )
    assertEquals(VerificationLifecycleStatus.UNREACHABLE_FROM_CURRENT_ISP, decision.status)
    assertFalse(decision.removalEligible)
  }

  @Test
  fun reachableFromAnotherBdIspOverridesUnreachability() {
    val ep = makeEndpoint("http://ftp.example.net")
    val decision =
      ConservativeDecisionEngine.evaluate(
        endpoint = ep,
        dns = DnsEvidence(domain = "example.net", summary = DnsSummary.HEALTHY),
        http = HttpProbeEvidence(outcome = "connection_refused"),
        ispProbes =
          listOf(
            makeIspProbe("amberit", "unreachable", "timeout"),
            makeIspProbe("link3", "reachable", "200"),
          ),
        nowEpochMs = nowMs,
      )
    assertEquals(VerificationLifecycleStatus.ISP_RESTRICTED, decision.status)
    assertFalse(decision.removalEligible)
  }

  @Test
  fun multiIspPositiveProbesConfirmActive() {
    val ep = makeEndpoint("http://ftp.example.net")
    val decision =
      ConservativeDecisionEngine.evaluate(
        endpoint = ep,
        ispProbes =
          listOf(
            makeIspProbe("amberit", "reachable", "200"),
            makeIspProbe("carnival", "reachable", "200"),
          ),
        nowEpochMs = nowMs,
      )
    assertEquals(VerificationLifecycleStatus.ACTIVE, decision.status)
    assertEquals(ConfidenceLevel.HIGH, decision.confidence)
  }

  @Test
  fun privateRfc1918RangesExemptedFromDeadDomainDetection() {
    listOf("http://10.16.100.244", "http://172.16.50.217", "http://192.168.10.2").forEach { url ->
      val ep = makeEndpoint(url)
      val decision =
        ConservativeDecisionEngine.evaluate(
          endpoint = ep,
          http = HttpProbeEvidence(outcome = "timeout"),
          nowEpochMs = nowMs,
        )
      assertEquals("Private IP $url must remain UNKNOWN", VerificationLifecycleStatus.UNKNOWN, decision.status)
      assertFalse(decision.removalEligible)
    }
  }

  @Test
  fun dnsTemporaryFailuresAndSingleNxdomainStaySafe() {
    val ep = makeEndpoint("http://bdix.example.org")
    assertEquals(
      VerificationLifecycleStatus.DNS_TEMPORARY_FAILURE,
      ConservativeDecisionEngine
        .evaluate(
          endpoint = ep,
          dns = DnsEvidence(domain = "example.org", summary = DnsSummary.TIMEOUT),
          nowEpochMs = nowMs,
        ).status,
    )
    assertEquals(
      VerificationLifecycleStatus.DNS_TEMPORARY_FAILURE,
      ConservativeDecisionEngine
        .evaluate(
          endpoint = ep,
          dns = DnsEvidence(domain = "example.org", summary = DnsSummary.SERVFAIL),
          nowEpochMs = nowMs,
        ).status,
    )
    val singleNx =
      ConservativeDecisionEngine.evaluate(
        endpoint = ep,
        dns = DnsEvidence(domain = "example.org", summary = DnsSummary.SINGLE_NXDOMAIN),
        nowEpochMs = nowMs,
      )
    assertEquals(VerificationLifecycleStatus.DNS_NXDOMAIN_SUSPECTED, singleNx.status)
    assertFalse(singleNx.removalEligible)

    assertEquals(
      VerificationLifecycleStatus.UNKNOWN,
      ConservativeDecisionEngine
        .evaluate(
          endpoint = ep,
          dns = DnsEvidence(domain = "example.org", summary = DnsSummary.DISAGREED),
          nowEpochMs = nowMs,
        ).status,
    )
  }

  @Test
  fun httpRestrictionAndTransientStatusCodesNeverMarkDead() {
    val ep = makeEndpoint("http://media.example.net")
    assertEquals(
      VerificationLifecycleStatus.ISP_RESTRICTED,
      ConservativeDecisionEngine.evaluate(ep, http = HttpProbeEvidence("auth_401", 401)).status,
    )
    assertEquals(
      VerificationLifecycleStatus.ISP_RESTRICTED,
      ConservativeDecisionEngine.evaluate(ep, http = HttpProbeEvidence("forbidden_403", 403)).status,
    )
    listOf(
      HttpProbeEvidence("rate_limited_429", 429),
      HttpProbeEvidence("server_error_5xx", 503),
      HttpProbeEvidence("tls_error", tlsError = "Cert expired"),
      HttpProbeEvidence("not_found_404", 404),
    ).forEach { http ->
      val d = ConservativeDecisionEngine.evaluate(ep, http = http, nowEpochMs = nowMs)
      assertEquals(VerificationLifecycleStatus.TEMPORARILY_UNAVAILABLE, d.status)
      assertFalse(d.removalEligible)
    }

    // Single HTTP 410 without multi-ISP corroboration stays temporarily unavailable
    val singleGone = ConservativeDecisionEngine.evaluate(ep, http = HttpProbeEvidence("gone_410", 410), nowEpochMs = nowMs)
    assertEquals(VerificationLifecycleStatus.TEMPORARILY_UNAVAILABLE, singleGone.status)
  }

  @Test
  fun restorableRegistryStatusesAreMarkedExpiredSuspectedOnly() {
    val ep = makeEndpoint("http://old-ftp.example.net")
    listOf(
      RegistrationLookupStatus.HOLD,
      RegistrationLookupStatus.REDEMPTION_PERIOD,
      RegistrationLookupStatus.PENDING_DELETE,
      RegistrationLookupStatus.EXPIRED_GRACE,
    ).forEach { status ->
      val d =
        ConservativeDecisionEngine.evaluate(
          endpoint = ep,
          dns = DnsEvidence(domain = "example.net", summary = DnsSummary.MULTI_NXDOMAIN),
          registration = RegistrationEvidence(domain = "example.net", lookupStatus = status),
          nowEpochMs = nowMs,
        )
      assertEquals(VerificationLifecycleStatus.DOMAIN_EXPIRED_SUSPECTED, d.status)
      assertFalse(d.removalEligible)
    }
  }

  @Test
  fun strongDeadDomainRequiresCorroboratedRdap404MultiNxdomainAndRecheckWindow() {
    val ep = makeEndpoint("http://dead-site.example")
    val dns = DnsEvidence(domain = "dead-site.example", summary = DnsSummary.MULTI_NXDOMAIN)
    val reg = RegistrationEvidence(domain = "dead-site.example", lookupStatus = RegistrationLookupStatus.UNREGISTERED_404)

    // First observation waits recheck window
    val firstPass = ConservativeDecisionEngine.evaluate(ep, dns = dns, registration = reg, nowEpochMs = nowMs)
    assertEquals(VerificationLifecycleStatus.DOMAIN_EXPIRED_CONFIRMED, firstPass.status)
    assertFalse(firstPass.removalEligible)

    // After 24h recheck window with 0 positive BD ISP probes -> REMOVAL_CANDIDATE (still never auto-archived)
    val prior =
      PriorGovernanceObservation(
        status = VerificationLifecycleStatus.DOMAIN_EXPIRED_CONFIRMED,
        checkedAtEpochMs = nowMs - 25 * 3_600_000L,
        dnsSummary = DnsSummary.MULTI_NXDOMAIN,
        registrationStatus = RegistrationLookupStatus.UNREGISTERED_404,
      )
    val secondPass = ConservativeDecisionEngine.evaluate(ep, dns = dns, registration = reg, prior = prior, nowEpochMs = nowMs)
    assertEquals(VerificationLifecycleStatus.REMOVAL_CANDIDATE, secondPass.status)
    assertTrue(secondPass.removalEligible)
  }

  @Test
  fun dotBdAndFtpAndArchivedRulesWorkConservatively() {
    assertEquals("example.com.bd", DomainExtractor.extractNormalizedDomain("ftp.sub.example.com.bd"))
    assertTrue(DomainExtractor.isDotBdDomain("example.com.bd"))

    val bdEp = makeEndpoint("http://portal.example.com.bd")
    val bdDecision =
      ConservativeDecisionEngine.evaluate(
        endpoint = bdEp,
        dns = DnsEvidence("example.com.bd", summary = DnsSummary.MULTI_NXDOMAIN),
        registration = RegistrationEvidence("example.com.bd", lookupStatus = RegistrationLookupStatus.REGISTRATION_LOOKUP_UNAVAILABLE),
        nowEpochMs = nowMs,
      )
    assertEquals(VerificationLifecycleStatus.UNKNOWN, bdDecision.status)
    assertFalse(bdDecision.removalEligible)

    val ftpEp = makeEndpoint("ftp://files.example.net")
    assertEquals(
      VerificationLifecycleStatus.ISP_RESTRICTED,
      ConservativeDecisionEngine.evaluate(ftpEp, ftp = FtpProbeEvidence("auth_required", 530), nowEpochMs = nowMs).status,
    )

    val archivedPreserved =
      ConservativeDecisionEngine.evaluate(
        endpoint = makeEndpoint("http://active.example.net"),
        http = HttpProbeEvidence("reachable_2xx", 200),
        currentLifecycleOverride = VerificationLifecycleStatus.ARCHIVED,
        nowEpochMs = nowMs,
      )
    assertEquals(VerificationLifecycleStatus.ARCHIVED, archivedPreserved.status)
  }

  @Test
  fun ssrfGuardBlocksPublicToPrivateMetadataAndHttpTesterMaps410And429AndSsrf() =
    runTest {
      val pub = "https://public-bdix.example".toHttpUrl()
      assertTrue(SsrfGuard.validateRedirectTarget(pub, "http://127.0.0.1/admin".toHttpUrl()) is SsrfCheckResult.Blocked)
      assertTrue(SsrfGuard.validateRedirectTarget(pub, "http://169.254.169.254/latest".toHttpUrl()) is SsrfCheckResult.Blocked)
      assertTrue(SsrfGuard.validateRedirectTarget(pub, "http://10.0.0.1/internal".toHttpUrl()) is SsrfCheckResult.Blocked)
      assertTrue(SsrfGuard.validateRedirectTarget(pub, "http://100.64.0.1/cgnat".toHttpUrl()) is SsrfCheckResult.Blocked)
      assertTrue(SsrfGuard.validateRedirectTarget(pub, "http://[fd00::1]/ula".toHttpUrl()) is SsrfCheckResult.Blocked)
      assertTrue(SsrfGuard.validateRedirectTarget(pub, "https://user:pass@other.example/".toHttpUrl()) is SsrfCheckResult.Blocked)
      assertTrue(SsrfGuard.validateRedirectTarget(pub, "https://mirror.example.net/".toHttpUrl()) is SsrfCheckResult.Allowed)

      val server = MockWebServer()
      server.start()
      try {
        val tester = HttpEndpointTester(OkHttpClient())
        val cfg = EndpointProbeConfig(ScanMode.FULL_SCAN, 2_000, 2_000, 5_000)

        server.enqueue(MockResponse().setResponseCode(410))
        val goneRes = tester.probe(makeEndpoint(server.url("/gone").toString()), cfg)
        assertEquals(ErrorClassification.HTTP_GONE, goneRes.errorClassification)
        assertEquals("gone_410", ConservativeDecisionEngine.toHttpEvidence(goneRes).outcome)

        server.enqueue(MockResponse().setResponseCode(429))
        val rateRes = tester.probe(makeEndpoint(server.url("/rate").toString()), cfg)
        assertEquals(TestStatus.RESTRICTED, rateRes.status)
        assertEquals(ErrorClassification.HTTP_RATE_LIMITED, rateRes.errorClassification)
        assertEquals("rate_limited_429", ConservativeDecisionEngine.toHttpEvidence(rateRes).outcome)

        server.enqueue(MockResponse().setResponseCode(302).addHeader("Location", "http://169.254.169.254/latest/meta-data"))
        val ssrfRes = tester.probe(makeEndpoint(server.url("/redir").toString()), cfg)
        assertEquals(TestStatus.UNREACHABLE, ssrfRes.status)
        assertEquals(ErrorClassification.SSRF_REDIRECT_BLOCKED, ssrfRes.errorClassification)
        assertEquals("ssrf_blocked", ConservativeDecisionEngine.toHttpEvidence(ssrfRes).outcome)
      } finally {
        server.shutdown()
      }
    }

  @Test
  fun dohRdapAndIspJsonParserHandleLiveWirePayloadsAndStaleness() =
    runTest {
      val server = MockWebServer()
      server.dispatcher =
        object : Dispatcher() {
          override fun dispatch(request: RecordedRequest): MockResponse {
            val path = request.path ?: ""
            return when {
              path.startsWith("/dns-nx") -> MockResponse().setResponseCode(200).setBody("""{"Status":3,"Answer":[]}""")
              path.startsWith("/bootstrap.json") ->
                MockResponse().setResponseCode(200).setBody(
                  """{"services":[[["example"],["${server.url("/rdap").toString().trimEnd('/')}"]]]}""",
                )
              path.startsWith("/rdap/domain/missing.example") -> MockResponse().setResponseCode(404)
              path.startsWith("/rdap/domain/hold.example") ->
                MockResponse().setResponseCode(200).setBody("""{"status":["clientHold"],"events":[]}""")
              else -> MockResponse().setResponseCode(404)
            }
          }
        }
      server.start()
      try {
        val doh =
          DohDnsResolver(
            resolvers =
              listOf(
                DohResolverEndpoint("R1", server.url("/dns-nx").toString()),
                DohResolverEndpoint("R2", server.url("/dns-nx").toString()),
              ),
          )
        val dnsEvidence = doh.lookupDomain("missing.example")
        assertEquals(DnsSummary.MULTI_NXDOMAIN, dnsEvidence.summary)

        val rdap = RdapLookupClient(bootstrapUrl = server.url("/bootstrap.json").toString())
        assertEquals(RegistrationLookupStatus.UNREGISTERED_404, rdap.lookupDomain("missing.example").lookupStatus)
        assertEquals(RegistrationLookupStatus.HOLD, rdap.lookupDomain("hold.example").lookupStatus)
        assertEquals(RegistrationLookupStatus.REGISTRATION_LOOKUP_UNAVAILABLE, rdap.lookupDomain("portal.gov.bd").lookupStatus)

        assertEquals(
          DnsSummary.DISAGREED,
          DohDnsResolver.summarizeResults(
            listOf(
              ResolverProbeResult("Cloudflare", "https://cf", "A", "NOERROR", listOf("1.2.3.4")),
              ResolverProbeResult("Google", "https://gg", "A", "NXDOMAIN", emptyList()),
            ),
          ),
        )

        val ispJson =
          """
          {
            "probes": [
              {
                "isp_id": "amberit",
                "isp_name": "AmberIT",
                "region": "Dhaka",
                "probed_at_utc": "2026-11-18T10:00:00Z",
                "endpoint_url": "http://ftp.example.net",
                "protocol": "http",
                "dns_result": "NOERROR",
                "connection_result": "reachable",
                "http_or_ftp_result": "200"
              },
              {
                "isp_id": "oldisp",
                "isp_name": "OldISP",
                "region": "Dhaka",
                "probed_at_utc": "2026-10-01T10:00:00Z",
                "endpoint_url": "http://ftp.example.net",
                "protocol": "http",
                "dns_result": "NOERROR",
                "connection_result": "unreachable",
                "http_or_ftp_result": "timeout"
              }
            ]
          }
          """.trimIndent()
        val parsedProbes =
          IspProbeJsonParser.parseJson(
            rawJson = ispJson,
            freshnessTtlHours = 168,
            nowEpochMs = 1_794_996_000_000L, // ~2026-11-18T10:00:00Z + some hours
          ).getOrThrow()
        assertEquals(1, parsedProbes.freshCount)
        assertEquals(1, parsedProbes.staleCount)
      } finally {
        server.shutdown()
      }
    }
}
