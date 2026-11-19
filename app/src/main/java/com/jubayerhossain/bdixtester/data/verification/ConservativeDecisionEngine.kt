package com.jubayerhossain.bdixtester.data.verification

import com.jubayerhossain.bdixtester.core.model.BdIspProbeRecord
import com.jubayerhossain.bdixtester.core.model.ConfidenceLevel
import com.jubayerhossain.bdixtester.core.model.DnsEvidence
import com.jubayerhossain.bdixtester.core.model.DnsSummary
import com.jubayerhossain.bdixtester.core.model.ErrorClassification
import com.jubayerhossain.bdixtester.core.model.FtpProbeEvidence
import com.jubayerhossain.bdixtester.core.model.GovernanceDecision
import com.jubayerhossain.bdixtester.core.model.HttpProbeEvidence
import com.jubayerhossain.bdixtester.core.model.PriorGovernanceObservation
import com.jubayerhossain.bdixtester.core.model.RegistrationEvidence
import com.jubayerhossain.bdixtester.core.model.RegistrationLookupStatus
import com.jubayerhossain.bdixtester.core.model.ServerProtocol
import com.jubayerhossain.bdixtester.core.model.TestStatus
import com.jubayerhossain.bdixtester.core.model.VerificationLifecycleStatus
import com.jubayerhossain.bdixtester.domain.model.ServerEndpoint
import com.jubayerhossain.bdixtester.domain.model.ServerTestResult

object ConservativeDecisionEngine {
  const val DEFAULT_RECHECK_HOURS = 24
  private const val MILLIS_PER_HOUR = 3_600_000L

  fun evaluate(
    endpoint: ServerEndpoint,
    dns: DnsEvidence? = null,
    registration: RegistrationEvidence? = null,
    http: HttpProbeEvidence? = null,
    ftp: FtpProbeEvidence? = null,
    ispProbes: List<BdIspProbeRecord> = emptyList(),
    prior: PriorGovernanceObservation? = null,
    currentLifecycleOverride: VerificationLifecycleStatus? = null,
    recheckHours: Int = DEFAULT_RECHECK_HOURS,
    nowEpochMs: Long = System.currentTimeMillis(),
  ): GovernanceDecision {
    val normalizedDomain = DomainExtractor.extractNormalizedDomain(endpoint.host)
    val isIpLiteral = DomainExtractor.isIpLiteral(endpoint.host)
    val freshProbes = ispProbes.filterNot { it.isStale }
    val staleProbes = ispProbes.filter { it.isStale }

    val checkedSources = mutableListOf<String>()
    val observed = mutableListOf<String>()
    val missing = mutableListOf<String>()

    if (dns != null) {
      checkedSources += "dns:${dns.summary.wireName}"
      observed += "DNS=${dns.summary.wireName}"
    } else if (!isIpLiteral) {
      missing += "Multi-resolver DNS verification"
    }

    if (registration != null) {
      checkedSources += "rdap:${registration.lookupStatus.wireName}"
      observed += "Registration=${registration.lookupStatus.wireName}"
      registration.limitationNote?.let { missing += it }
    } else if (!isIpLiteral) {
      missing += "Registration / RDAP verification"
    }

    if (http != null) {
      checkedSources += "http:${http.outcome}"
      observed += "HTTP=${http.outcome}" + (http.statusCode?.let { " ($it)" } ?: "")
    }
    if (ftp != null) {
      checkedSources += "ftp:${ftp.outcome}"
      observed += "FTP=${ftp.outcome}" + (ftp.greetingCode?.let { " ($it)" } ?: "")
    }
    if (freshProbes.isNotEmpty()) {
      checkedSources += "bd_isp_probes:${freshProbes.size}_fresh"
      observed += "${freshProbes.size} fresh Bangladesh ISP probe(s)"
    } else {
      missing += "Fresh multi-ISP Bangladesh probe evidence"
    }
    if (staleProbes.isNotEmpty()) {
      observed += "${staleProbes.size} stale ISP probe(s) excluded"
    }

    fun buildDecision(
      status: VerificationLifecycleStatus,
      confidence: ConfidenceLevel,
      reason: String,
      recommendedAction: String,
      removalEligible: Boolean = false,
      recheckAfterHours: Int? = null,
      extraSource: String? = null,
      extraObserved: String? = null,
    ): GovernanceDecision {
      if (extraSource != null && checkedSources.isEmpty()) checkedSources += extraSource
      if (extraObserved != null) observed += extraObserved
      return GovernanceDecision(
        serverId = endpoint.id,
        status = status,
        confidence = confidence,
        reason = reason,
        recommendedAction = recommendedAction,
        removalEligible = removalEligible,
        normalizedDomain = normalizedDomain,
        checkedSources = checkedSources.distinct(),
        observedEvidence = observed.distinct(),
        missingEvidence = missing.distinct(),
        checkedAtEpochMs = nowEpochMs,
        recheckAfterEpochMs = recheckAfterHours?.let { nowEpochMs + it * MILLIS_PER_HOUR },
        dns = dns,
        registration = registration,
        http = http,
        ftp = ftp,
        ispProbes = ispProbes,
      )
    }

    // Rule 0: Preserve explicit ARCHIVED lock unless user explicitly restores
    if (currentLifecycleOverride == VerificationLifecycleStatus.ARCHIVED) {
      return buildDecision(
        status = VerificationLifecycleStatus.ARCHIVED,
        confidence = ConfidenceLevel.HIGH,
        reason = "Archived reversibly by user governance review.",
        recommendedAction = "Keep archived; restore anytime with 1 tap.",
        extraSource = "user_governance",
      )
    }

    // Rule 1: Unsupported protocols (e.g., FTPS, unknown schemes)
    if (endpoint.protocol == ServerProtocol.UNSUPPORTED ||
      endpoint.protocol == ServerProtocol.FTPS ||
      ftp?.outcome == "unsupported_protocol"
    ) {
      return buildDecision(
        status = VerificationLifecycleStatus.UNSUPPORTED_PROTOCOL,
        confidence = ConfidenceLevel.HIGH,
        reason = "Protocol ${endpoint.protocol.displayLabel} is unsupported for automated probing.",
        recommendedAction = "Keep entry preserved without removal.",
        extraSource = "url_parser",
      )
    }

    // Rule 2: Private RFC1918 IPv4 exemption — never delete private ISP-internal ranges
    if (endpoint.isPrivateIp) {
      val freshReachable =
        freshProbes.any {
          it.connectionResult in setOf("reachable", "ok", "restricted", "auth_required", "forbidden")
        }
      return if (freshReachable) {
        buildDecision(
          status = VerificationLifecycleStatus.ISP_RESTRICTED,
          confidence = ConfidenceLevel.HIGH,
          reason = "Private RFC1918 ISP-intranet endpoint verified inside partner ISP probes.",
          recommendedAction = "Keep preserved for internal ISP users.",
          extraObserved = "RFC1918 host ${endpoint.host}",
        )
      } else {
        buildDecision(
          status = VerificationLifecycleStatus.UNKNOWN,
          confidence = ConfidenceLevel.NONE,
          reason = "Private RFC1918 IP address exempted from public dead-domain detection.",
          recommendedAction = "Retain permanently; reachable only inside specific Bangladesh ISPs.",
          extraSource = "private_ip_exemption",
          extraObserved = "RFC1918 host ${endpoint.host}",
        )
      }
    }

    // Rule 3: Multi-ISP Bangladesh probe corroboration overrides current-network unreachability
    val positiveIsps =
      freshProbes
        .filter {
          it.connectionResult in setOf("reachable", "ok") ||
            it.httpOrFtpResult.startsWith("2") ||
            it.httpOrFtpResult.startsWith("3")
        }.map { it.ispId }
        .toSet()
    val restrictedIsps =
      freshProbes
        .filter { it.connectionResult == "restricted" || it.httpOrFtpResult in setOf("401", "403", "auth_required") }
        .map { it.ispId }
        .toSet()

    if (positiveIsps.size >= 2) {
      return buildDecision(
        status = VerificationLifecycleStatus.ACTIVE,
        confidence = ConfidenceLevel.HIGH,
        reason = "Verified reachable across ${positiveIsps.size} independent Bangladesh ISPs.",
        recommendedAction = "Keep active in catalog.",
      )
    }
    val accessibleIsps = positiveIsps + restrictedIsps
    if (accessibleIsps.isNotEmpty() && http?.outcome != "redirected") {
      val failingIsps = freshProbes.map { it.ispId }.toSet() - accessibleIsps
      val status =
        if (accessibleIsps.size == 1 || failingIsps.isNotEmpty() || restrictedIsps.isNotEmpty()) {
          VerificationLifecycleStatus.ISP_RESTRICTED
        } else {
          VerificationLifecycleStatus.ACTIVE
        }
      return buildDecision(
        status = status,
        confidence = ConfidenceLevel.HIGH,
        reason = "Accessible from ${accessibleIsps.size} Bangladesh ISP(s); ISP-specific routing or policy detected.",
        recommendedAction = "Retain and protect; never delete ISP-specific BDIX endpoints.",
      )
    }

    // Rule 4: Direct HTTP/FTP probe interpretation
    if (http != null) {
      when (http.outcome) {
        "reachable_2xx" ->
          return buildDecision(
            status = VerificationLifecycleStatus.ACTIVE,
            confidence = ConfidenceLevel.HIGH,
            reason = "Endpoint responded with HTTP ${http.statusCode ?: 200}.",
            recommendedAction = "Keep active in catalog.",
          )
        "redirected" ->
          return buildDecision(
            status = VerificationLifecycleStatus.REDIRECTED,
            confidence = ConfidenceLevel.HIGH,
            reason = "Endpoint safely redirected to ${http.finalUrl ?: "target"} (HTTP ${http.statusCode ?: 301}).",
            recommendedAction = "Keep active in catalog.",
          )
        "auth_401", "forbidden_403" ->
          return buildDecision(
            status = VerificationLifecycleStatus.ISP_RESTRICTED,
            confidence = ConfidenceLevel.HIGH,
            reason = "HTTP ${http.statusCode} confirms host is active with access/auth restriction.",
            recommendedAction = "Keep in catalog as restricted service.",
          )
        "rate_limited_429", "server_error_5xx", "tls_error", "not_found_404", "ssrf_blocked" -> {
          val msg =
            when (http.outcome) {
              "rate_limited_429" -> "HTTP 429 rate limiting indicates an active service under load."
              "server_error_5xx" -> "HTTP ${http.statusCode} is temporary upstream maintenance/error."
              "tls_error" -> "Expired or mismatched TLS certificate is not proof of domain expiration."
              "ssrf_blocked" -> "Unsafe redirect target blocked; verify endpoint URL manually."
              else -> "HTTP 404 on a specific path does not prove the domain is dead."
            }
          return buildDecision(
            status = VerificationLifecycleStatus.TEMPORARILY_UNAVAILABLE,
            confidence = ConfidenceLevel.MEDIUM,
            reason = msg,
            recommendedAction = "Preserve and recheck later.",
            recheckAfterHours = recheckHours,
          )
        }
        "gone_410" -> {
          val multiIspGone =
            freshProbes
              .filter { it.httpOrFtpResult == "410" }
              .map { it.ispId }
              .distinct()
              .size >= 2
          val elapsedHours = elapsedHours(prior, nowEpochMs)
          return if (multiIspGone && elapsedHours >= recheckHours) {
            buildDecision(
              status = VerificationLifecycleStatus.REMOVAL_CANDIDATE,
              confidence = ConfidenceLevel.MEDIUM,
              reason = "HTTP 410 Gone corroborated across multiple ISPs and recheck window.",
              recommendedAction = "Review evidence before optional archival.",
              removalEligible = true,
            )
          } else {
            buildDecision(
              status = VerificationLifecycleStatus.TEMPORARILY_UNAVAILABLE,
              confidence = ConfidenceLevel.MEDIUM,
              reason = "HTTP 410 Gone observed on path; flagged for investigation without automatic removal.",
              recommendedAction = "Inspect root path and re-verify later.",
              recheckAfterHours = recheckHours,
            )
          }
        }
      }
    }

    if (ftp != null) {
      when (ftp.outcome) {
        "reachable_220" ->
          return buildDecision(
            status = VerificationLifecycleStatus.ACTIVE,
            confidence = ConfidenceLevel.HIGH,
            reason = "FTP server responded with RFC 959 220 banner.",
            recommendedAction = "Keep active in catalog.",
          )
        "auth_required", "login_rejected" ->
          return buildDecision(
            status = VerificationLifecycleStatus.ISP_RESTRICTED,
            confidence = ConfidenceLevel.HIGH,
            reason = "FTP server is live and requires authentication.",
            recommendedAction = "Keep preserved in catalog.",
          )
        "server_unavailable_421", "directory_unavailable_550" ->
          return buildDecision(
            status = VerificationLifecycleStatus.TEMPORARILY_UNAVAILABLE,
            confidence = ConfidenceLevel.MEDIUM,
            reason = "FTP returned ${ftp.greetingCode ?: ftp.outcome}; server host is active.",
            recommendedAction = "Retain and retry later.",
            recheckAfterHours = recheckHours,
          )
      }
    }

    // Rule 5: Non-private IP literal unreachable on current network -> Unreachable / Unknown, NEVER expired domain
    if (isIpLiteral) {
      val failedCurrentProbe =
        http?.outcome in setOf("timeout", "connection_refused", "error") ||
          ftp?.outcome in setOf("timeout", "connection_refused", "error")
      return if (failedCurrentProbe) {
        buildDecision(
          status = VerificationLifecycleStatus.UNREACHABLE_FROM_CURRENT_ISP,
          confidence = ConfidenceLevel.LOW,
          reason =
            "Public/BDIX IP literal unreachable from current network; " +
              "BDIX routing varies across Bangladesh ISPs.",
          recommendedAction = "Preserve entry; verify with partner BD ISP probes.",
        )
      } else {
        buildDecision(
          status = VerificationLifecycleStatus.UNKNOWN,
          confidence = ConfidenceLevel.NONE,
          reason = "Public IP endpoint awaits multi-ISP probe evidence.",
          recommendedAction = "Preserve until BD ISP probe confirmation.",
          extraSource = "ip_literal_policy",
        )
      }
    }

    // Rule 6: DNS temporary failures / disagreements
    val dnsSummary = dns?.summary ?: DnsSummary.NOT_CHECKED
    val regStatus = registration?.lookupStatus ?: RegistrationLookupStatus.NOT_CHECKED

    if (dnsSummary == DnsSummary.SERVFAIL || dnsSummary == DnsSummary.TIMEOUT) {
      return buildDecision(
        status = VerificationLifecycleStatus.DNS_TEMPORARY_FAILURE,
        confidence = ConfidenceLevel.MEDIUM,
        reason = "DNS resolution returned temporary ${dnsSummary.wireName}; never treated as domain expiration.",
        recommendedAction = "Keep active and recheck DNS later.",
        recheckAfterHours = recheckHours,
      )
    }

    if (dnsSummary == DnsSummary.DISAGREED) {
      return buildDecision(
        status = VerificationLifecycleStatus.UNKNOWN,
        confidence = ConfidenceLevel.LOW,
        reason = "Independent DNS resolvers disagreed on domain existence.",
        recommendedAction = "Preserve conservatively and recheck later.",
        recheckAfterHours = recheckHours,
      )
    }

    if (dnsSummary == DnsSummary.SINGLE_NXDOMAIN) {
      return buildDecision(
        status = VerificationLifecycleStatus.DNS_NXDOMAIN_SUSPECTED,
        confidence = ConfidenceLevel.LOW,
        reason = "Only one resolver returned NXDOMAIN; insufficient for domain expiration.",
        recommendedAction = "Wait for multi-resolver & RDAP corroboration.",
        recheckAfterHours = recheckHours,
      )
    }

    // Rule 7: Registry states that remain potentially restorable
    if (regStatus in
      setOf(
        RegistrationLookupStatus.HOLD,
        RegistrationLookupStatus.REDEMPTION_PERIOD,
        RegistrationLookupStatus.EXPIRED_GRACE,
        RegistrationLookupStatus.PENDING_DELETE,
      )
    ) {
      return buildDecision(
        status = VerificationLifecycleStatus.DOMAIN_EXPIRED_SUSPECTED,
        confidence = ConfidenceLevel.MEDIUM,
        reason = "RDAP reports '${regStatus.wireName}'; domain remains registered/restorable.",
        recommendedAction = "Re-check after ${recheckHours}h before confirming expiration.",
        recheckAfterHours = recheckHours,
      )
    }

    // Rule 8: Strong dead-domain path A — Authoritative RDAP 404 + Multi/Auth NXDOMAIN + 0 positive BD ISP probes
    if (regStatus == RegistrationLookupStatus.UNREGISTERED_404 &&
      (dnsSummary == DnsSummary.MULTI_NXDOMAIN || dnsSummary == DnsSummary.AUTH_NXDOMAIN)
    ) {
      val elapsedHours = elapsedHours(prior, nowEpochMs)
      val priorCorroborated =
        prior != null &&
          prior.status in
          setOf(
            VerificationLifecycleStatus.DOMAIN_EXPIRED_SUSPECTED,
            VerificationLifecycleStatus.DOMAIN_EXPIRED_CONFIRMED,
            VerificationLifecycleStatus.REMOVAL_CANDIDATE,
          ) &&
          elapsedHours >= recheckHours
      return if (priorCorroborated || dnsSummary == DnsSummary.AUTH_NXDOMAIN) {
        buildDecision(
          status = VerificationLifecycleStatus.REMOVAL_CANDIDATE,
          confidence = ConfidenceLevel.HIGH,
          reason = "Authoritative RDAP 404 + corroborated multi-resolver NXDOMAIN with zero positive BD ISP evidence.",
          recommendedAction = "Inspect evidence and archive only if confirmed (100% reversible).",
          removalEligible = true,
        )
      } else {
        buildDecision(
          status = VerificationLifecycleStatus.DOMAIN_EXPIRED_CONFIRMED,
          confidence = ConfidenceLevel.HIGH,
          reason =
            "Authoritative RDAP 404 + multi-resolver NXDOMAIN observed; " +
              "waiting ${recheckHours}h recheck window.",
          recommendedAction = "Recheck after ${recheckHours}h before marking Removal Candidate.",
          recheckAfterHours = recheckHours,
        )
      }
    }

    // Rule 9: Multi/Auth NXDOMAIN without confirmed RDAP 404 (e.g., .bd BTCL limitation or registered-but-NXDOMAIN)
    if (dnsSummary == DnsSummary.MULTI_NXDOMAIN || dnsSummary == DnsSummary.AUTH_NXDOMAIN) {
      if (regStatus == RegistrationLookupStatus.REGISTRATION_LOOKUP_UNAVAILABLE) {
        val elapsedHours = elapsedHours(prior, nowEpochMs)
        return if (prior != null &&
          (prior.dnsSummary == DnsSummary.MULTI_NXDOMAIN || prior.dnsSummary == DnsSummary.AUTH_NXDOMAIN) &&
          elapsedHours >= recheckHours &&
          dnsSummary == DnsSummary.AUTH_NXDOMAIN
        ) {
          buildDecision(
            status = VerificationLifecycleStatus.REMOVAL_CANDIDATE,
            confidence = ConfidenceLevel.MEDIUM,
            reason = "Repeated authoritative NXDOMAIN across >= ${recheckHours}h with unavailable registry RDAP (.bd).",
            recommendedAction = "Manual registry verification recommended before archival.",
            removalEligible = true,
          )
        } else {
          buildDecision(
            status =
              if (prior ==
                null
              ) {
                VerificationLifecycleStatus.UNKNOWN
              } else {
                VerificationLifecycleStatus.DNS_NXDOMAIN_SUSPECTED
              },
            confidence = ConfidenceLevel.LOW,
            reason = "Multi-resolver NXDOMAIN observed, but registration lookup is unavailable (e.g. .bd / BTCL TLD).",
            recommendedAction = "Retain conservatively and recheck after ${recheckHours}h.",
            recheckAfterHours = recheckHours,
          )
        }
      }
      return buildDecision(
        status = VerificationLifecycleStatus.DNS_NXDOMAIN_SUSPECTED,
        confidence = ConfidenceLevel.MEDIUM,
        reason = "Multi-resolver NXDOMAIN observed while domain remains registered or unconfirmed in RDAP.",
        recommendedAction = "Retain and recheck after ${recheckHours}h.",
        recheckAfterHours = recheckHours,
      )
    }

    // Rule 10: Healthy DNS, unreachable from current ISP/network
    if ((dnsSummary == DnsSummary.HEALTHY || dnsSummary == DnsSummary.NODATA) &&
      (
        http?.outcome in setOf("timeout", "connection_refused", "error") ||
          ftp?.outcome in setOf("timeout", "connection_refused", "error") ||
          freshProbes.isNotEmpty()
      )
    ) {
      val status =
        if (freshProbes.isNotEmpty() && positiveIsps.isEmpty()) {
          VerificationLifecycleStatus.TEMPORARILY_UNAVAILABLE
        } else {
          VerificationLifecycleStatus.UNREACHABLE_FROM_CURRENT_ISP
        }
      return buildDecision(
        status = status,
        confidence = ConfidenceLevel.MEDIUM,
        reason =
          "Domain resolves via DNS but is unreachable from current network " +
            "(typical for ISP-scoped BDIX routing).",
        recommendedAction = "Preserve in catalog; do not remove without multi-ISP confirmation.",
      )
    }

    return buildDecision(
      status = VerificationLifecycleStatus.UNKNOWN,
      confidence = ConfidenceLevel.NONE,
      reason = "Insufficient independent evidence to confirm availability or domain expiration.",
      recommendedAction = "Preserve as Unknown until verified.",
      extraSource = "conservative_default",
    )
  }

  fun toHttpEvidence(
    result: ServerTestResult,
    nowEpochMs: Long = result.testedAtEpochMs,
  ): HttpProbeEvidence {
    val code = result.httpStatusCode
    val outcome =
      when {
        result.errorClassification == ErrorClassification.SSRF_REDIRECT_BLOCKED -> "ssrf_blocked"
        result.errorClassification == ErrorClassification.TLS_FAILURE -> "tls_error"
        result.errorClassification == ErrorClassification.TIMEOUT -> "timeout"
        result.errorClassification == ErrorClassification.CONNECTION_REFUSED -> "connection_refused"
        result.errorClassification == ErrorClassification.DNS_FAILURE -> "dns_failure"
        code != null && code in 200..299 && result.finalUrl != null -> "redirected"
        code != null && code in 200..299 -> "reachable_2xx"
        code == 401 -> "auth_401"
        code == 403 -> "forbidden_403"
        code == 404 -> "not_found_404"
        code == 410 -> "gone_410"
        code == 429 -> "rate_limited_429"
        code != null && code in 500..599 -> "server_error_5xx"
        result.status == TestStatus.REACHABLE && result.finalUrl != null -> "redirected"
        result.status == TestStatus.REACHABLE -> "reachable_2xx"
        result.status == TestStatus.TIMED_OUT -> "timeout"
        result.status == TestStatus.UNSUPPORTED -> "unsupported"
        else -> "error"
      }
    return HttpProbeEvidence(
      outcome = outcome,
      statusCode = code,
      finalUrl = result.finalUrl,
      redirectChain = listOfNotNull(result.finalUrl),
      responseTimeMs = result.httpResponseLatencyMs ?: result.primaryLatencyMs,
      tlsError = if (result.errorClassification == ErrorClassification.TLS_FAILURE) result.failureMessage else null,
      errorDetail = result.failureMessage,
      checkedAtEpochMs = nowEpochMs,
    )
  }

  fun toFtpEvidence(
    result: ServerTestResult,
    nowEpochMs: Long = result.testedAtEpochMs,
  ): FtpProbeEvidence {
    val code = result.ftpGreetingCode
    val outcome =
      when {
        result.status == TestStatus.UNSUPPORTED -> "unsupported_protocol"
        code != null && code in 200..299 -> "reachable_220"
        code == 421 -> "server_unavailable_421"
        code == 530 -> "login_rejected"
        code == 550 -> "directory_unavailable_550"
        result.errorClassification == ErrorClassification.TIMEOUT || result.status == TestStatus.TIMED_OUT -> "timeout"
        result.errorClassification == ErrorClassification.CONNECTION_REFUSED -> "connection_refused"
        else -> "error"
      }
    return FtpProbeEvidence(
      outcome = outcome,
      greetingCode = code,
      responseTimeMs = result.ftpConnectLatencyMs ?: result.primaryLatencyMs,
      errorDetail = result.failureMessage,
      checkedAtEpochMs = nowEpochMs,
    )
  }

  private fun elapsedHours(
    prior: PriorGovernanceObservation?,
    nowEpochMs: Long,
  ): Double {
    if (prior == null || nowEpochMs <= prior.checkedAtEpochMs) return 0.0
    return (nowEpochMs - prior.checkedAtEpochMs).toDouble() / MILLIS_PER_HOUR
  }
}
