from __future__ import annotations

from datetime import datetime, timedelta, timezone

from .isp_probes import parse_utc_iso
from .models import (
  CatalogEntry,
  ClassificationDecision,
  ConfidenceLevel,
  DnsObservation,
  FtpObservation,
  HttpObservation,
  IspProbeRecord,
  PriorObservation,
  RegistrationObservation,
  VerificationLifecycleStatus,
)


def _bucket_for_status(status: VerificationLifecycleStatus) -> str:
  return {
    VerificationLifecycleStatus.ACTIVE: "Confirmed Active",
    VerificationLifecycleStatus.REDIRECTED: "Confirmed Active",
    VerificationLifecycleStatus.ISP_RESTRICTED: "ISP Restricted",
    VerificationLifecycleStatus.UNREACHABLE_FROM_CURRENT_ISP: "Unreachable From Current Network",
    VerificationLifecycleStatus.TEMPORARILY_UNAVAILABLE: "Temporarily Unavailable",
    VerificationLifecycleStatus.DNS_TEMPORARY_FAILURE: "Temporarily Unavailable",
    VerificationLifecycleStatus.UNSUPPORTED_PROTOCOL: "Unverified",
    VerificationLifecycleStatus.UNKNOWN: "Unverified",
    VerificationLifecycleStatus.PENDING_VERIFICATION: "Unverified",
    VerificationLifecycleStatus.DNS_NXDOMAIN_SUSPECTED: "Expired Suspected",
    VerificationLifecycleStatus.DOMAIN_EXPIRED_SUSPECTED: "Expired Suspected",
    VerificationLifecycleStatus.DOMAIN_EXPIRED_CONFIRMED: "Domain Expired Confirmed",
    VerificationLifecycleStatus.PERMANENTLY_REMOVED: "Removal Candidates",
    VerificationLifecycleStatus.REMOVAL_CANDIDATE: "Removal Candidates",
    VerificationLifecycleStatus.ARCHIVED: "Archived",
  }[status]


def _elapsed_hours(prior: PriorObservation | None, now_utc: datetime) -> float:
  if prior is None:
    return 0.0
  try:
    prev_dt = parse_utc_iso(prior.checked_at_utc)
    return max(0.0, (now_utc - prev_dt).total_seconds() / 3600.0)
  except ValueError:
    return 0.0


def classify_endpoint(
  entry: CatalogEntry,
  dns: DnsObservation | None = None,
  registration: RegistrationObservation | None = None,
  http: HttpObservation | None = None,
  ftp: FtpObservation | None = None,
  isp_probes: list[IspProbeRecord] | None = None,
  prior: PriorObservation | None = None,
  governance_override: VerificationLifecycleStatus | None = None,
  recheck_hours: int = 24,
  now_utc: datetime | None = None,
) -> ClassificationDecision:
  ref_now = now_utc or datetime.now(timezone.utc)
  now_iso = ref_now.replace(microsecond=0).isoformat().replace("+00:00", "Z")
  probes = isp_probes or []
  fresh_probes = [p for p in probes if not p.is_stale]
  stale_probes = [p for p in probes if p.is_stale]

  checked_sources: list[str] = []
  observed: list[str] = []
  missing: list[str] = []

  if dns is not None:
    checked_sources.append(f"dns:{dns.summary}")
    observed.append(f"DNS summary={dns.summary}")
  elif not entry.is_ip_literal:
    missing.append("Multi-resolver DNS check")

  if registration is not None:
    checked_sources.append(f"rdap:{registration.lookup_status}")
    observed.append(f"Registration lookup={registration.lookup_status}")
    if registration.limitation_note:
      missing.append(registration.limitation_note)
  elif not entry.is_ip_literal:
    missing.append("Registration/RDAP check")

  if http is not None:
    checked_sources.append(f"http:{http.outcome}")
    observed.append(f"HTTP outcome={http.outcome}" + (f" ({http.status_code})" if http.status_code else ""))
  if ftp is not None:
    checked_sources.append(f"ftp:{ftp.outcome}")
    observed.append(f"FTP outcome={ftp.outcome}" + (f" ({ftp.greeting_code})" if ftp.greeting_code else ""))

  if fresh_probes:
    checked_sources.append(f"bd_isp_probes:{len(fresh_probes)}_fresh")
    observed.append(f"{len(fresh_probes)} fresh Bangladesh ISP probe(s)")
  else:
    missing.append("Fresh external Bangladesh ISP probe data")
  if stale_probes:
    observed.append(f"{len(stale_probes)} stale ISP probe(s) excluded from current proof")

  # Rule 0: Explicit archived lock is preserved
  if governance_override == VerificationLifecycleStatus.ARCHIVED:
    return ClassificationDecision(
      entry=entry,
      status=VerificationLifecycleStatus.ARCHIVED,
      confidence=ConfidenceLevel.HIGH,
      audit_bucket="Archived",
      reason="Entry was explicitly archived via reversible user/maintainer governance action.",
      recommended_action="Keep archived; restore anytime from Settings -> Catalog Governance & Restore.",
      removal_eligible=False,
      checked_sources=checked_sources or ["governance_manifest"],
      observed_evidence=observed,
      missing_evidence=missing,
      checked_at_utc=now_iso,
      dns=dns,
      registration=registration,
      http=http,
      ftp=ftp,
      isp_probes=probes,
    )

  # Rule 1: Unsupported protocol
  if entry.protocol in ("unsupported", "ftps") or (ftp and ftp.outcome == "unsupported_protocol"):
    return ClassificationDecision(
      entry=entry,
      status=VerificationLifecycleStatus.UNSUPPORTED_PROTOCOL,
      confidence=ConfidenceLevel.HIGH,
      audit_bucket=_bucket_for_status(VerificationLifecycleStatus.UNSUPPORTED_PROTOCOL),
      reason=f"Protocol '{entry.protocol}' is unsupported for automated verification without downgrading.",
      recommended_action="Preserve entry as unsupported protocol; do not remove.",
      removal_eligible=False,
      checked_sources=checked_sources or ["url_parser"],
      observed_evidence=observed,
      missing_evidence=missing,
      checked_at_utc=now_iso,
      dns=dns,
      registration=registration,
      http=http,
      ftp=ftp,
      isp_probes=probes,
    )

  # Rule 2: Private RFC1918 IP exemption (critical rule)
  if entry.is_private_ip:
    fresh_ok = [p for p in fresh_probes if p.connection_result in {"reachable", "ok", "restricted", "auth_required", "forbidden"}]
    if fresh_ok:
      status = VerificationLifecycleStatus.ISP_RESTRICTED
      reason = "Private RFC1918 ISP-intranet endpoint verified reachable inside partner ISP probe."
      conf = ConfidenceLevel.HIGH
    else:
      status = VerificationLifecycleStatus.UNKNOWN
      reason = "Private RFC1918 IP address exempted from public dead-domain classification."
      conf = ConfidenceLevel.NONE
    return ClassificationDecision(
      entry=entry,
      status=status,
      confidence=conf,
      audit_bucket=_bucket_for_status(status),
      reason=reason,
      recommended_action="Preserve permanently; test only from inside the attributed ISP network.",
      removal_eligible=False,
      checked_sources=checked_sources or ["private_ip_policy"],
      observed_evidence=observed + [f"Private RFC1918 host {entry.host}"],
      missing_evidence=missing,
      checked_at_utc=now_iso,
      dns=dns,
      registration=registration,
      http=http,
      ftp=ftp,
      isp_probes=probes,
    )

  # Rule 3: Fresh Bangladesh ISP probe evaluation (multi-ISP positive overrides public CI unreachability)
  positive_isps = {
    p.isp_id
    for p in fresh_probes
    if p.connection_result in {"reachable", "ok"} or p.http_or_ftp_result.startswith(("2", "3"))
  }
  restricted_isps = {
    p.isp_id
    for p in fresh_probes
    if p.connection_result == "restricted" or p.http_or_ftp_result in {"401", "403", "auth_required"}
  }
  if len(positive_isps) >= 2:
    return ClassificationDecision(
      entry=entry,
      status=VerificationLifecycleStatus.ACTIVE,
      confidence=ConfidenceLevel.HIGH,
      audit_bucket=_bucket_for_status(VerificationLifecycleStatus.ACTIVE),
      reason=f"Verified reachable from {len(positive_isps)} independent Bangladesh ISPs.",
      recommended_action="Keep active in catalog.",
      removal_eligible=False,
      checked_sources=checked_sources,
      observed_evidence=observed,
      missing_evidence=missing,
      checked_at_utc=now_iso,
      dns=dns,
      registration=registration,
      http=http,
      ftp=ftp,
      isp_probes=probes,
    )
  if len(positive_isps | restricted_isps) >= 1 and (
    http is None or http.outcome not in {"redirected"}
  ):
    isp_count = len(positive_isps | restricted_isps)
    failing_isps = len({p.isp_id for p in fresh_probes} - (positive_isps | restricted_isps))
    status = VerificationLifecycleStatus.ISP_RESTRICTED if (isp_count == 1 or failing_isps > 0 or restricted_isps) else VerificationLifecycleStatus.ACTIVE
    return ClassificationDecision(
      entry=entry,
      status=status,
      confidence=ConfidenceLevel.HIGH,
      audit_bucket=_bucket_for_status(status),
      reason=f"Accessible on {isp_count} Bangladesh ISP(s); ISP-specific routing or access policy observed.",
      recommended_action="Protect and retain; never delete ISP-specific BDIX services.",
      removal_eligible=False,
      checked_sources=checked_sources,
      observed_evidence=observed,
      missing_evidence=missing,
      checked_at_utc=now_iso,
      dns=dns,
      registration=registration,
      http=http,
      ftp=ftp,
      isp_probes=probes,
    )

  # Rule 4: Direct HTTP / FTP positive or temporary/policy outcomes
  if http is not None:
    if http.outcome == "reachable_2xx":
      return ClassificationDecision(
        entry=entry,
        status=VerificationLifecycleStatus.ACTIVE,
        confidence=ConfidenceLevel.HIGH,
        audit_bucket=_bucket_for_status(VerificationLifecycleStatus.ACTIVE),
        reason=f"Endpoint responded with HTTP {http.status_code}.",
        recommended_action="Keep active in catalog.",
        removal_eligible=False,
        checked_sources=checked_sources,
        observed_evidence=observed,
        missing_evidence=missing,
        checked_at_utc=now_iso,
        dns=dns,
        registration=registration,
        http=http,
        ftp=ftp,
        isp_probes=probes,
      )
    if http.outcome == "redirected":
      return ClassificationDecision(
        entry=entry,
        status=VerificationLifecycleStatus.REDIRECTED,
        confidence=ConfidenceLevel.HIGH,
        audit_bucket=_bucket_for_status(VerificationLifecycleStatus.REDIRECTED),
        reason=f"Endpoint redirected safely to {http.final_url} (HTTP {http.status_code}).",
        recommended_action="Inspect redirect destination; keep entry active.",
        removal_eligible=False,
        checked_sources=checked_sources,
        observed_evidence=observed,
        missing_evidence=missing,
        checked_at_utc=now_iso,
        dns=dns,
        registration=registration,
        http=http,
        ftp=ftp,
        isp_probes=probes,
      )
    if http.outcome in {"auth_401", "forbidden_403"}:
      return ClassificationDecision(
        entry=entry,
        status=VerificationLifecycleStatus.ISP_RESTRICTED,
        confidence=ConfidenceLevel.HIGH,
        audit_bucket=_bucket_for_status(VerificationLifecycleStatus.ISP_RESTRICTED),
        reason=f"Server responded with HTTP {http.status_code} ({http.outcome}); host and domain are alive.",
        recommended_action="Retain as ISP/auth-restricted service; never mark expired.",
        removal_eligible=False,
        checked_sources=checked_sources,
        observed_evidence=observed,
        missing_evidence=missing,
        checked_at_utc=now_iso,
        dns=dns,
        registration=registration,
        http=http,
        ftp=ftp,
        isp_probes=probes,
      )
    if http.outcome in {"rate_limited_429", "server_error_5xx", "tls_error", "not_found_404"}:
      detail_map = {
        "rate_limited_429": "HTTP 429 rate limiting indicates active service under load.",
        "server_error_5xx": f"HTTP {http.status_code} indicates temporary upstream server error.",
        "tls_error": "TLS certificate or handshake failure is not proof of domain expiration.",
        "not_found_404": "HTTP 404 on a specific path does not prove the domain or root service is dead.",
      }
      return ClassificationDecision(
        entry=entry,
        status=VerificationLifecycleStatus.TEMPORARILY_UNAVAILABLE,
        confidence=ConfidenceLevel.MEDIUM,
        audit_bucket=_bucket_for_status(VerificationLifecycleStatus.TEMPORARILY_UNAVAILABLE),
        reason=detail_map[http.outcome],
        recommended_action="Keep in catalog and re-check later.",
        removal_eligible=False,
        checked_sources=checked_sources,
        observed_evidence=observed,
        missing_evidence=missing,
        checked_at_utc=now_iso,
        dns=dns,
        registration=registration,
        http=http,
        ftp=ftp,
        isp_probes=probes,
      )
    if http.outcome == "gone_410":
      # HTTP 410 alone must trigger investigation, never automatic removal
      multi_isp_gone = len({p.isp_id for p in fresh_probes if p.http_or_ftp_result == "410"}) >= 2
      if multi_isp_gone and _elapsed_hours(prior, ref_now) >= recheck_hours:
        return ClassificationDecision(
          entry=entry,
          status=VerificationLifecycleStatus.REMOVAL_CANDIDATE,
          confidence=ConfidenceLevel.MEDIUM,
          audit_bucket="Removal Candidates",
          reason="HTTP 410 Gone corroborated across multiple independent ISP probes and recheck window.",
          recommended_action="Human review recommended before archival.",
          removal_eligible=True,
          checked_sources=checked_sources,
          observed_evidence=observed,
          missing_evidence=missing,
          checked_at_utc=now_iso,
          dns=dns,
          registration=registration,
          http=http,
          ftp=ftp,
          isp_probes=probes,
        )
      return ClassificationDecision(
        entry=entry,
        status=VerificationLifecycleStatus.TEMPORARILY_UNAVAILABLE,
        confidence=ConfidenceLevel.MEDIUM,
        audit_bucket="Temporarily Unavailable",
        reason="HTTP 410 Gone observed; flagged for path/service investigation without automatic domain removal.",
        recommended_action="Investigate sibling paths/root domain and await independent corroboration.",
        removal_eligible=False,
        checked_sources=checked_sources,
        observed_evidence=observed,
        missing_evidence=missing,
        checked_at_utc=now_iso,
        dns=dns,
        registration=registration,
        http=http,
        ftp=ftp,
        isp_probes=probes,
      )

  if ftp is not None:
    if ftp.outcome == "reachable_220":
      return ClassificationDecision(
        entry=entry,
        status=VerificationLifecycleStatus.ACTIVE,
        confidence=ConfidenceLevel.HIGH,
        audit_bucket="Confirmed Active",
        reason="FTP service responded with RFC 959 220 greeting.",
        recommended_action="Keep active in catalog.",
        removal_eligible=False,
        checked_sources=checked_sources,
        observed_evidence=observed,
        missing_evidence=missing,
        checked_at_utc=now_iso,
        dns=dns,
        registration=registration,
        http=http,
        ftp=ftp,
        isp_probes=probes,
      )
    if ftp.outcome in {"auth_required", "login_rejected"}:
      return ClassificationDecision(
        entry=entry,
        status=VerificationLifecycleStatus.ISP_RESTRICTED,
        confidence=ConfidenceLevel.HIGH,
        audit_bucket="ISP Restricted",
        reason="FTP server responded requiring authentication; service is alive.",
        recommended_action="Retain in catalog without credential brute forcing.",
        removal_eligible=False,
        checked_sources=checked_sources,
        observed_evidence=observed,
        missing_evidence=missing,
        checked_at_utc=now_iso,
        dns=dns,
        registration=registration,
        http=http,
        ftp=ftp,
        isp_probes=probes,
      )
    if ftp.outcome in {"server_unavailable_421", "directory_unavailable_550"}:
      return ClassificationDecision(
        entry=entry,
        status=VerificationLifecycleStatus.TEMPORARILY_UNAVAILABLE,
        confidence=ConfidenceLevel.MEDIUM,
        audit_bucket="Temporarily Unavailable",
        reason=f"FTP reported {ftp.outcome}; server host exists.",
        recommended_action="Re-check later.",
        removal_eligible=False,
        checked_sources=checked_sources,
        observed_evidence=observed,
        missing_evidence=missing,
        checked_at_utc=now_iso,
        dns=dns,
        registration=registration,
        http=http,
        ftp=ftp,
        isp_probes=probes,
      )

  # Rule 5: IP literals (non-private) without HTTP/FTP success stay unreachable/temp-unavailable, NEVER expired domain
  if entry.is_ip_literal:
    if (http and http.outcome in {"timeout", "connection_refused"}) or (ftp and ftp.outcome in {"timeout", "connection_refused"}):
      status = VerificationLifecycleStatus.UNREACHABLE_FROM_CURRENT_ISP
      reason = "Public/BDIX IP endpoint unreachable from current network; BDIX routing differs across Bangladesh ISPs."
    else:
      status = VerificationLifecycleStatus.UNKNOWN
      reason = "IP-literal endpoint lacks Bangladesh ISP probe verification."
    return ClassificationDecision(
      entry=entry,
      status=status,
      confidence=ConfidenceLevel.LOW,
      audit_bucket=_bucket_for_status(status),
      reason=reason,
      recommended_action="Supply Bangladesh ISP probe data before making any governance decision.",
      removal_eligible=False,
      checked_sources=checked_sources or ["ip_literal_policy"],
      observed_evidence=observed,
      missing_evidence=missing,
      checked_at_utc=now_iso,
      dns=dns,
      registration=registration,
      http=http,
      ftp=ftp,
      isp_probes=probes,
    )

  # Rule 6: Domain registration + Multi-resolver DNS logic
  dns_sum = dns.summary if dns else "not_checked"
  reg_status = registration.lookup_status if registration else "not_checked"

  if dns_sum in {"servfail", "timeout"}:
    return ClassificationDecision(
      entry=entry,
      status=VerificationLifecycleStatus.DNS_TEMPORARY_FAILURE,
      confidence=ConfidenceLevel.MEDIUM,
      audit_bucket="Temporarily Unavailable",
      reason=f"DNS resolution encountered temporary '{dns_sum}' condition; never proof of domain expiration.",
      recommended_action="Keep in catalog and retry DNS verification later.",
      removal_eligible=False,
      checked_sources=checked_sources,
      observed_evidence=observed,
      missing_evidence=missing,
      checked_at_utc=now_iso,
      dns=dns,
      registration=registration,
      http=http,
      ftp=ftp,
      isp_probes=probes,
    )

  if dns_sum == "disagreed":
    return ClassificationDecision(
      entry=entry,
      status=VerificationLifecycleStatus.UNKNOWN,
      confidence=ConfidenceLevel.LOW,
      audit_bucket="Unverified",
      reason="Independent DNS resolvers disagreed; preserving entry under conservative policy.",
      recommended_action="Re-check later against authoritative nameservers.",
      removal_eligible=False,
      checked_sources=checked_sources,
      observed_evidence=observed,
      missing_evidence=missing,
      checked_at_utc=now_iso,
      dns=dns,
      registration=registration,
      http=http,
      ftp=ftp,
      isp_probes=probes,
    )

  if dns_sum == "single_nxdomain":
    return ClassificationDecision(
      entry=entry,
      status=VerificationLifecycleStatus.DNS_NXDOMAIN_SUSPECTED,
      confidence=ConfidenceLevel.LOW,
      audit_bucket="Expired Suspected",
      reason="Only a single resolver returned NXDOMAIN; insufficient to declare domain expired.",
      recommended_action="Verify with multiple independent resolvers and authoritative NS.",
      removal_eligible=False,
      checked_sources=checked_sources,
      observed_evidence=observed,
      missing_evidence=missing,
      checked_at_utc=now_iso,
      dns=dns,
      registration=registration,
      http=http,
      ftp=ftp,
      isp_probes=probes,
    )

  # Registered / hold / redemption / expired_grace domains must NOT be treated as confirmed dead
  if reg_status in {"hold", "redemption_period", "expired_grace", "pending_delete"}:
    reason_map = {
      "hold": "RDAP reports clientHold/serverHold; domain remains registered and potentially restorable.",
      "redemption_period": "RDAP reports redemptionPeriod; domain may still be restored by owner.",
      "expired_grace": "RDAP expiration date passed, but domain remains present in registry (grace/renewal window).",
      "pending_delete": "RDAP reports pendingDelete; awaiting final registry drop and post-drop NXDOMAIN corroboration.",
    }
    return ClassificationDecision(
      entry=entry,
      status=VerificationLifecycleStatus.DOMAIN_EXPIRED_SUSPECTED,
      confidence=ConfidenceLevel.MEDIUM,
      audit_bucket="Expired Suspected",
      reason=reason_map[reg_status],
      recommended_action=f"Recheck after {recheck_hours}h window to verify renewal or final deletion.",
      removal_eligible=False,
      checked_sources=checked_sources,
      observed_evidence=observed,
      missing_evidence=missing,
      checked_at_utc=now_iso,
      dns=dns,
      registration=registration,
      http=http,
      ftp=ftp,
      isp_probes=probes,
    )

  # Strong dead-domain path A: RDAP unregistered_404 + corroborated multi/auth NXDOMAIN
  if reg_status == "unregistered_404" and dns_sum in {"multi_nxdomain", "auth_nxdomain"}:
    recheck_ok = (
      prior is not None
      and prior.status in {VerificationLifecycleStatus.DOMAIN_EXPIRED_SUSPECTED, VerificationLifecycleStatus.DOMAIN_EXPIRED_CONFIRMED, VerificationLifecycleStatus.REMOVAL_CANDIDATE}
      and _elapsed_hours(prior, ref_now) >= recheck_hours
    ) or dns_sum == "auth_nxdomain"
    if recheck_ok:
      status = (
        VerificationLifecycleStatus.REMOVAL_CANDIDATE
        if governance_override != VerificationLifecycleStatus.DOMAIN_EXPIRED_CONFIRMED
        else VerificationLifecycleStatus.DOMAIN_EXPIRED_CONFIRMED
      )
      return ClassificationDecision(
        entry=entry,
        status=VerificationLifecycleStatus.REMOVAL_CANDIDATE,
        confidence=ConfidenceLevel.HIGH,
        audit_bucket="Removal Candidates",
        reason=(
          "Authoritative RDAP confirms domain is unregistered (404) AND multiple independent DNS resolvers/NS "
          "confirm NXDOMAIN with zero positive Bangladesh ISP evidence."
        ),
        recommended_action="Review evidence and confirm archival if appropriate (reversible).",
        removal_eligible=True,
        checked_sources=checked_sources,
        observed_evidence=observed,
        missing_evidence=missing,
        checked_at_utc=now_iso,
        dns=dns,
        registration=registration,
        http=http,
        ftp=ftp,
        isp_probes=probes,
      )
    return ClassificationDecision(
      entry=entry,
      status=VerificationLifecycleStatus.DOMAIN_EXPIRED_CONFIRMED,
      confidence=ConfidenceLevel.HIGH,
      audit_bucket="Domain Expired Confirmed",
      reason=(
        "Authoritative RDAP 404 + multi-resolver NXDOMAIN observed; awaiting recheck window "
        f"({recheck_hours}h) before promoting to removal_candidate."
      ),
      recommended_action=f"Hold in review queue and re-verify after {recheck_hours}h.",
      removal_eligible=False,
      checked_sources=checked_sources,
      observed_evidence=observed,
      missing_evidence=missing,
      checked_at_utc=now_iso,
      dns=dns,
      registration=registration,
      http=http,
      ftp=ftp,
      isp_probes=probes,
    )

  # Strong dead-domain path B: Registration lookup unavailable (e.g., .bd) + corroborated multi/auth NXDOMAIN
  if dns_sum in {"multi_nxdomain", "auth_nxdomain"}:
    if reg_status == "registration_lookup_unavailable":
      elapsed = _elapsed_hours(prior, ref_now)
      if prior is not None and prior.dns_summary in {"multi_nxdomain", "auth_nxdomain"} and elapsed >= recheck_hours and dns_sum == "auth_nxdomain":
        return ClassificationDecision(
          entry=entry,
          status=VerificationLifecycleStatus.REMOVAL_CANDIDATE,
          confidence=ConfidenceLevel.MEDIUM,
          audit_bucket="Removal Candidates",
          reason=(
            "Repeated multi-resolver & authoritative NXDOMAIN confirmed across >= "
            f"{recheck_hours}h recheck window; registration API unavailable for TLD."
          ),
          recommended_action="Manual review required (verify registry portal before archiving).",
          removal_eligible=True,
          checked_sources=checked_sources,
          observed_evidence=observed,
          missing_evidence=missing,
          checked_at_utc=now_iso,
          dns=dns,
          registration=registration,
          http=http,
          ftp=ftp,
          isp_probes=probes,
        )
      return ClassificationDecision(
        entry=entry,
        status=VerificationLifecycleStatus.UNKNOWN if not prior else VerificationLifecycleStatus.DNS_NXDOMAIN_SUSPECTED,
        confidence=ConfidenceLevel.LOW,
        audit_bucket="Unverified" if not prior else "Expired Suspected",
        reason=(
          "Multi-resolver NXDOMAIN observed, but authoritative registration API is unavailable "
          "(e.g., .bd / BTCL TLD limitation). Staying conservative."
        ),
        recommended_action=f"Preserve entry and re-verify after {recheck_hours}h or consult registry portal.",
        removal_eligible=False,
        checked_sources=checked_sources,
        observed_evidence=observed,
        missing_evidence=missing,
        checked_at_utc=now_iso,
        dns=dns,
        registration=registration,
        http=http,
        ftp=ftp,
        isp_probes=probes,
      )

    # Registered domain yet NXDOMAIN -> suspected (DNS misconfiguration or clientHold transition), never removal candidate immediately
    return ClassificationDecision(
      entry=entry,
      status=VerificationLifecycleStatus.DNS_NXDOMAIN_SUSPECTED,
      confidence=ConfidenceLevel.MEDIUM,
      audit_bucket="Expired Suspected",
      reason="Multiple resolvers returned NXDOMAIN while RDAP shows active/registered or unconfirmed deletion.",
      recommended_action=f"Keep entry and re-verify after {recheck_hours}h.",
      removal_eligible=False,
      checked_sources=checked_sources,
      observed_evidence=observed,
      missing_evidence=missing,
      checked_at_utc=now_iso,
      dns=dns,
      registration=registration,
      http=http,
      ftp=ftp,
      isp_probes=probes,
    )

  # Rule 7: Healthy DNS/Registration, but unreachable from current runner/ISP
  if dns_sum in {"healthy", "nodata"} and (
    (http and http.outcome in {"timeout", "connection_refused", "error"})
    or (ftp and ftp.outcome in {"timeout", "connection_refused", "error"})
    or fresh_probes
  ):
    if fresh_probes and not positive_isps:
      reason = "Domain resolves normally, but endpoint was inaccessible across currently supplied ISP probes (possibly temporarily offline)."
      status = VerificationLifecycleStatus.TEMPORARILY_UNAVAILABLE
    else:
      reason = "Domain resolves via public DNS, but HTTP/FTP endpoint is unreachable from current network (typical for BDIX ISP-only routing)."
      status = VerificationLifecycleStatus.UNREACHABLE_FROM_CURRENT_ISP
    return ClassificationDecision(
      entry=entry,
      status=status,
      confidence=ConfidenceLevel.MEDIUM,
      audit_bucket=_bucket_for_status(status),
      reason=reason,
      recommended_action="Preserve entry; verify using Bangladesh ISP probes inside target BDIX networks.",
      removal_eligible=False,
      checked_sources=checked_sources,
      observed_evidence=observed,
      missing_evidence=missing,
      checked_at_utc=now_iso,
      dns=dns,
      registration=registration,
      http=http,
      ftp=ftp,
      isp_probes=probes,
    )

  return ClassificationDecision(
    entry=entry,
    status=VerificationLifecycleStatus.UNKNOWN,
    confidence=ConfidenceLevel.NONE,
    audit_bucket="Unverified",
    reason="Insufficient independent evidence to confirm availability or domain expiration.",
    recommended_action="Preserve entry as Unverified until DNS/ISP evidence is collected.",
    removal_eligible=False,
    checked_sources=checked_sources or ["none"],
    observed_evidence=observed,
    missing_evidence=missing,
    checked_at_utc=now_iso,
    dns=dns,
    registration=registration,
    http=http,
    ftp=ftp,
    isp_probes=probes,
  )
