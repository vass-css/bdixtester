from __future__ import annotations

from dataclasses import asdict, dataclass, field
from enum import Enum
from typing import Any


class VerificationLifecycleStatus(str, Enum):
  ACTIVE = "active"
  REDIRECTED = "redirected"
  ISP_RESTRICTED = "isp_restricted"
  UNREACHABLE_FROM_CURRENT_ISP = "unreachable_from_current_isp"
  TEMPORARILY_UNAVAILABLE = "temporarily_unavailable"
  DNS_TEMPORARY_FAILURE = "dns_temporary_failure"
  UNSUPPORTED_PROTOCOL = "unsupported_protocol"
  UNKNOWN = "unknown"
  PENDING_VERIFICATION = "pending_verification"
  DNS_NXDOMAIN_SUSPECTED = "dns_nxdomain_suspected"
  DOMAIN_EXPIRED_SUSPECTED = "domain_expired_suspected"
  DOMAIN_EXPIRED_CONFIRMED = "domain_expired_confirmed"
  PERMANENTLY_REMOVED = "permanently_removed"
  REMOVAL_CANDIDATE = "removal_candidate"
  ARCHIVED = "archived"


class ConfidenceLevel(str, Enum):
  HIGH = "high"
  MEDIUM = "medium"
  LOW = "low"
  NONE = "none"


@dataclass(frozen=True)
class CatalogEntry:
  entry_id: str
  url: str
  canonical_key: str
  category: str
  isp_name: str
  protocol: str
  host: str
  explicit_port: int | None
  normalized_domain: str | None
  is_ip_literal: bool
  is_private_ip: bool
  source_file: str


@dataclass(frozen=True)
class ResolverResult:
  resolver_name: str
  resolver_endpoint: str
  record_type: str
  rcode: str  # NOERROR, NXDOMAIN, SERVFAIL, REFUSED, TIMEOUT, ERROR
  answers: list[str] = field(default_factory=list)
  ttl_seconds: int | None = None
  error: str | None = None
  checked_at_utc: str = ""


@dataclass(frozen=True)
class DnsObservation:
  domain: str
  resolver_results: list[ResolverResult] = field(default_factory=list)
  authoritative_results: list[ResolverResult] = field(default_factory=list)
  ns_records: list[str] = field(default_factory=list)
  soa_records: list[str] = field(default_factory=list)
  summary: str = "not_checked"  # healthy, nodata, single_nxdomain, multi_nxdomain, auth_nxdomain, servfail, timeout, disagreed, not_checked
  checked_at_utc: str = ""


@dataclass(frozen=True)
class RegistrationObservation:
  domain: str
  lookup_status: str  # registered, unregistered_404, pending_delete, redemption_period, hold, expired_grace, registration_lookup_unavailable, lookup_error, not_applicable
  authoritative_source: str | None = None
  epp_statuses: list[str] = field(default_factory=list)
  registrar: str | None = None
  created_at: str | None = None
  expires_at: str | None = None
  updated_at: str | None = None
  limitation_note: str | None = None
  checked_at_utc: str = ""


@dataclass(frozen=True)
class HttpObservation:
  outcome: str  # reachable_2xx, redirected, auth_401, forbidden_403, not_found_404, gone_410, rate_limited_429, server_error_5xx, tls_error, timeout, connection_refused, ssrf_blocked, unsupported, error, not_checked
  status_code: int | None = None
  final_url: str | None = None
  redirect_chain: list[str] = field(default_factory=list)
  response_time_ms: int | None = None
  tls_error: str | None = None
  error_detail: str | None = None
  checked_at_utc: str = ""


@dataclass(frozen=True)
class FtpObservation:
  outcome: str  # reachable_220, auth_required, login_rejected, directory_unavailable_550, server_unavailable_421, timeout, unsupported_protocol, connection_refused, error, not_checked
  greeting_code: int | None = None
  banner_excerpt: str | None = None
  response_time_ms: int | None = None
  error_detail: str | None = None
  checked_at_utc: str = ""


@dataclass(frozen=True)
class IspProbeRecord:
  isp_id: str
  isp_name: str
  region: str
  probed_at_utc: str
  endpoint_url: str
  protocol: str
  dns_result: str
  connection_result: str
  http_or_ftp_result: str
  response_time_ms: int | None = None
  error_info: str | None = None
  probe_software_version: str = "unknown"
  is_stale: bool = False


@dataclass(frozen=True)
class PriorObservation:
  status: VerificationLifecycleStatus
  checked_at_utc: str
  dns_summary: str | None = None
  registration_status: str | None = None


@dataclass(frozen=True)
class ClassificationDecision:
  entry: CatalogEntry
  status: VerificationLifecycleStatus
  confidence: ConfidenceLevel
  audit_bucket: str
  reason: str
  recommended_action: str
  removal_eligible: bool
  checked_sources: list[str]
  observed_evidence: list[str]
  missing_evidence: list[str]
  checked_at_utc: str
  dns: DnsObservation | None = None
  registration: RegistrationObservation | None = None
  http: HttpObservation | None = None
  ftp: FtpObservation | None = None
  isp_probes: list[IspProbeRecord] = field(default_factory=list)

  def to_dict(self) -> dict[str, Any]:
    data = asdict(self)
    data["status"] = self.status.value
    data["confidence"] = self.confidence.value
    return data
