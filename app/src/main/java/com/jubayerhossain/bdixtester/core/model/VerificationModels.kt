package com.jubayerhossain.bdixtester.core.model

enum class VerificationLifecycleStatus(
  val wireName: String,
  val displayLabel: String,
) {
  ACTIVE("active", "Active"),
  REDIRECTED("redirected", "Redirected"),
  ISP_RESTRICTED("isp_restricted", "ISP Restricted"),
  UNREACHABLE_FROM_CURRENT_ISP("unreachable_from_current_isp", "Unreachable From Current ISP"),
  TEMPORARILY_UNAVAILABLE("temporarily_unavailable", "Temporarily Unavailable"),
  DNS_TEMPORARY_FAILURE("dns_temporary_failure", "DNS Temporary Failure"),
  UNSUPPORTED_PROTOCOL("unsupported_protocol", "Unsupported Protocol"),
  UNKNOWN("unknown", "Unknown"),
  PENDING_VERIFICATION("pending_verification", "Pending Verification"),
  DNS_NXDOMAIN_SUSPECTED("dns_nxdomain_suspected", "DNS NXDOMAIN Suspected"),
  DOMAIN_EXPIRED_SUSPECTED("domain_expired_suspected", "Domain Expired Suspected"),
  DOMAIN_EXPIRED_CONFIRMED("domain_expired_confirmed", "Domain Expired Confirmed"),
  PERMANENTLY_REMOVED("permanently_removed", "Permanently Removed"),
  REMOVAL_CANDIDATE("removal_candidate", "Removal Candidate"),
  ARCHIVED("archived", "Archived"),
  ;

  val isRemovalCandidateOrConfirmed: Boolean
    get() =
      this == REMOVAL_CANDIDATE ||
        this == DOMAIN_EXPIRED_CONFIRMED ||
        this == PERMANENTLY_REMOVED

  val isProtectedFromAutoRemoval: Boolean
    get() =
      this == ACTIVE ||
        this == REDIRECTED ||
        this == ISP_RESTRICTED ||
        this == UNREACHABLE_FROM_CURRENT_ISP ||
        this == TEMPORARILY_UNAVAILABLE ||
        this == DNS_TEMPORARY_FAILURE ||
        this == UNSUPPORTED_PROTOCOL ||
        this == UNKNOWN ||
        this == PENDING_VERIFICATION

  companion object {
    fun fromWireOrName(raw: String?): VerificationLifecycleStatus {
      if (raw.isNullOrBlank()) return PENDING_VERIFICATION
      return entries.firstOrNull {
        it.name.equals(raw, ignoreCase = true) || it.wireName.equals(raw, ignoreCase = true)
      } ?: UNKNOWN
    }
  }
}

enum class ConfidenceLevel(
  val wireName: String,
  val displayLabel: String,
) {
  HIGH("high", "High"),
  MEDIUM("medium", "Medium"),
  LOW("low", "Low"),
  NONE("none", "None"),
  ;

  companion object {
    fun fromWireOrName(raw: String?): ConfidenceLevel =
      entries.firstOrNull {
        it.name.equals(raw, ignoreCase = true) || it.wireName.equals(raw, ignoreCase = true)
      } ?: NONE
  }
}

enum class GovernanceFilter {
  ALL,
  ACTIVE,
  ISP_RESTRICTED,
  UNREACHABLE,
  TEMPORARILY_UNAVAILABLE,
  EXPIRED_SUSPECTED,
  REMOVAL_CANDIDATES,
  ARCHIVED,
  UNVERIFIED,
}

enum class DnsSummary(
  val wireName: String,
) {
  HEALTHY("healthy"),
  NODATA("nodata"),
  SINGLE_NXDOMAIN("single_nxdomain"),
  MULTI_NXDOMAIN("multi_nxdomain"),
  AUTH_NXDOMAIN("auth_nxdomain"),
  SERVFAIL("servfail"),
  TIMEOUT("timeout"),
  DISAGREED("disagreed"),
  NOT_CHECKED("not_checked"),
  ;

  companion object {
    fun fromWire(raw: String?): DnsSummary =
      entries.firstOrNull { it.wireName.equals(raw, ignoreCase = true) || it.name.equals(raw, ignoreCase = true) }
        ?: NOT_CHECKED
  }
}

enum class RegistrationLookupStatus(
  val wireName: String,
) {
  REGISTERED("registered"),
  UNREGISTERED_404("unregistered_404"),
  PENDING_DELETE("pending_delete"),
  REDEMPTION_PERIOD("redemption_period"),
  HOLD("hold"),
  EXPIRED_GRACE("expired_grace"),
  REGISTRATION_LOOKUP_UNAVAILABLE("registration_lookup_unavailable"),
  LOOKUP_ERROR("lookup_error"),
  NOT_APPLICABLE("not_applicable"),
  NOT_CHECKED("not_checked"),
  ;

  companion object {
    fun fromWire(raw: String?): RegistrationLookupStatus =
      entries.firstOrNull { it.wireName.equals(raw, ignoreCase = true) || it.name.equals(raw, ignoreCase = true) }
        ?: NOT_CHECKED
  }
}

data class ResolverProbeResult(
  val resolverName: String,
  val resolverEndpoint: String,
  val recordType: String,
  val rcode: String,
  val answers: List<String> = emptyList(),
  val ttlSeconds: Int? = null,
  val error: String? = null,
  val checkedAtEpochMs: Long = 0L,
)

data class DnsEvidence(
  val domain: String,
  val resolverResults: List<ResolverProbeResult> = emptyList(),
  val authoritativeResults: List<ResolverProbeResult> = emptyList(),
  val nsRecords: List<String> = emptyList(),
  val soaRecords: List<String> = emptyList(),
  val summary: DnsSummary = DnsSummary.NOT_CHECKED,
  val checkedAtEpochMs: Long = 0L,
)

data class RegistrationEvidence(
  val domain: String,
  val lookupStatus: RegistrationLookupStatus = RegistrationLookupStatus.NOT_CHECKED,
  val authoritativeSource: String? = null,
  val eppStatuses: List<String> = emptyList(),
  val registrar: String? = null,
  val createdAtIso: String? = null,
  val expiresAtIso: String? = null,
  val updatedAtIso: String? = null,
  val limitationNote: String? = null,
  val checkedAtEpochMs: Long = 0L,
)

data class HttpProbeEvidence(
  val outcome: String,
  val statusCode: Int? = null,
  val finalUrl: String? = null,
  val redirectChain: List<String> = emptyList(),
  val responseTimeMs: Long? = null,
  val tlsError: String? = null,
  val errorDetail: String? = null,
  val checkedAtEpochMs: Long = 0L,
)

data class FtpProbeEvidence(
  val outcome: String,
  val greetingCode: Int? = null,
  val bannerExcerpt: String? = null,
  val responseTimeMs: Long? = null,
  val errorDetail: String? = null,
  val checkedAtEpochMs: Long = 0L,
)

data class BdIspProbeRecord(
  val ispId: String,
  val ispName: String,
  val region: String,
  val probedAtUtc: String,
  val endpointUrl: String,
  val protocol: String,
  val dnsResult: String,
  val connectionResult: String,
  val httpOrFtpResult: String,
  val responseTimeMs: Long? = null,
  val errorInfo: String? = null,
  val probeSoftwareVersion: String = "unknown",
  val isStale: Boolean = false,
)

data class PriorGovernanceObservation(
  val status: VerificationLifecycleStatus,
  val checkedAtEpochMs: Long,
  val dnsSummary: DnsSummary? = null,
  val registrationStatus: RegistrationLookupStatus? = null,
)

data class GovernanceDecision(
  val serverId: String,
  val status: VerificationLifecycleStatus,
  val confidence: ConfidenceLevel,
  val reason: String,
  val recommendedAction: String,
  val removalEligible: Boolean,
  val normalizedDomain: String? = null,
  val checkedSources: List<String> = emptyList(),
  val observedEvidence: List<String> = emptyList(),
  val missingEvidence: List<String> = emptyList(),
  val checkedAtEpochMs: Long,
  val recheckAfterEpochMs: Long? = null,
  val dns: DnsEvidence? = null,
  val registration: RegistrationEvidence? = null,
  val http: HttpProbeEvidence? = null,
  val ftp: FtpProbeEvidence? = null,
  val ispProbes: List<BdIspProbeRecord> = emptyList(),
)

data class EndpointGovernanceRecord(
  val serverId: String,
  val lifecycleStatus: VerificationLifecycleStatus = VerificationLifecycleStatus.PENDING_VERIFICATION,
  val confidence: ConfidenceLevel = ConfidenceLevel.NONE,
  val reason: String = "Pending conservative domain, registration, and multi-ISP verification.",
  val recommendedAction: String = "Run safe verification to inspect DNS, RDAP, and connectivity.",
  val removalEligible: Boolean = false,
  val normalizedDomain: String? = null,
  val dnsSummary: DnsSummary = DnsSummary.NOT_CHECKED,
  val registrationStatus: RegistrationLookupStatus = RegistrationLookupStatus.NOT_CHECKED,
  val checkedSources: List<String> = emptyList(),
  val observedEvidence: List<String> = emptyList(),
  val missingEvidence: List<String> = emptyList(),
  val dns: DnsEvidence? = null,
  val registration: RegistrationEvidence? = null,
  val http: HttpProbeEvidence? = null,
  val ftp: FtpProbeEvidence? = null,
  val ispProbes: List<BdIspProbeRecord> = emptyList(),
  val recheckAfterEpochMs: Long? = null,
  val lastVerifiedAtEpochMs: Long? = null,
  val archivedAtEpochMs: Long? = null,
  val archivedByUser: Boolean = false,
  val updatedAtEpochMs: Long = 0L,
)

data class VerificationEventRecord(
  val eventId: Long = 0L,
  val serverId: String,
  val previousStatus: VerificationLifecycleStatus,
  val newStatus: VerificationLifecycleStatus,
  val confidence: ConfidenceLevel,
  val triggerSource: String,
  val summary: String,
  val detailsJson: String = "{}",
  val createdAtEpochMs: Long = 0L,
)

data class GovernanceOverviewCounts(
  val totalCatalogCount: Int = 0,
  val activeCount: Int = 0,
  val ispRestrictedCount: Int = 0,
  val unreachableCount: Int = 0,
  val temporarilyUnavailableCount: Int = 0,
  val expiredSuspectedCount: Int = 0,
  val removalCandidateCount: Int = 0,
  val archivedCount: Int = 0,
  val unverifiedCount: Int = 0,
)

data class VerificationRunProgress(
  val isRunning: Boolean = false,
  val processedCount: Int = 0,
  val totalCount: Int = 0,
  val currentEndpointName: String? = null,
  val candidatesDetected: Int = 0,
  val lastCompletedAtEpochMs: Long? = null,
  val lastErrorMessage: String? = null,
)
