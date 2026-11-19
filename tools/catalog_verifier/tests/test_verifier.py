from __future__ import annotations

import json
import tempfile
import unittest
from datetime import datetime, timedelta, timezone
from pathlib import Path

from tools.catalog_verifier.catalog_loader import validate_repository_catalogs
from tools.catalog_verifier.decision_engine import classify_endpoint
from tools.catalog_verifier.isp_probes import load_isp_probes_by_key
from tools.catalog_verifier.models import (
  CatalogEntry,
  DnsObservation,
  FtpObservation,
  HttpObservation,
  IspProbeRecord,
  PriorObservation,
  RegistrationObservation,
  VerificationLifecycleStatus,
)
from tools.catalog_verifier.network_probes import RdapClient, parse_rdap_domain_payload
from tools.catalog_verifier.reporting import build_json_report, build_markdown_report
from tools.catalog_verifier.url_utils import (
  canonical_key,
  extract_registrable_domain,
  is_rfc1918_private_ip,
  validate_public_probe_target,
)

REPO_ROOT = Path(__file__).resolve().parents[3]


def _entry(
  url: str = "http://ftp.example-isp.net/movies",
  category: str = "FTP_FILES",
  protocol: str = "http",
  host: str = "ftp.example-isp.net",
  domain: str | None = "example-isp.net",
  is_ip: bool = False,
  is_private: bool = False,
) -> CatalogEntry:
  return CatalogEntry(
    entry_id="builtin:test",
    url=url,
    canonical_key=canonical_key(url),
    category=category,
    isp_name="Test ISP",
    protocol=protocol,
    host=host,
    explicit_port=None,
    normalized_domain=domain,
    is_ip_literal=is_ip,
    is_private_ip=is_private,
    source_file="TestCatalog.kt",
  )


class CatalogVerifierTestSuite(unittest.TestCase):
  def test_offline_repository_catalog_validation_passes(self) -> None:
    report = validate_repository_catalogs(REPO_ROOT)
    self.assertTrue(report.valid, f"Validation failed: {report}")
    self.assertEqual(report.curated_count, 752)
    self.assertEqual(report.ott_count, 26)
    self.assertEqual(report.ftp_json_endpoints, 667)
    self.assertEqual(report.live_tv_json_endpoints, 90)

  def test_domain_normalization_and_ssrf_guards(self) -> None:
    self.assertEqual(extract_registrable_domain("www.server1.example.com.bd"), "example.com.bd")
    self.assertEqual(extract_registrable_domain("cdn.circleftp.net"), "circleftp.net")
    self.assertTrue(is_rfc1918_private_ip("10.16.100.244"))
    self.assertTrue(is_rfc1918_private_ip("172.16.50.4"))
    self.assertTrue(is_rfc1918_private_ip("192.168.10.20"))
    self.assertFalse(is_rfc1918_private_ip("103.58.73.6"))

    # SSRF guards block localhost, private IPs, link-local metadata, unsupported schemes
    for bad in (
      "http://127.0.0.1:8080/admin",
      "http://localhost/health",
      "http://169.254.169.254/latest/meta-data/",
      "http://10.0.0.1/",
      "http://192.168.1.1/",
      "file:///etc/passwd",
      "gopher://127.0.0.1:70/",
    ):
      ok, reason = validate_public_probe_target(bad, resolve_dns=False)
      self.assertFalse(ok, f"Expected SSRF block for {bad}, got reason={reason}")

    ok_pub, _ = validate_public_probe_target("https://chorki.net", resolve_dns=False)
    self.assertTrue(ok_pub)

  # --- 22-Scenario False-Positive & Conservative Governance Matrix ---

  def test_01_one_isp_cannot_access_server_not_expired(self) -> None:
    d = classify_endpoint(
      _entry(),
      dns=DnsObservation("ftp.example-isp.net", summary="healthy"),
      http=HttpObservation("timeout"),
      isp_probes=[
        IspProbeRecord("link3", "Link3", "Dhaka", "2026-11-18T00:00:00Z", "http://ftp.example-isp.net/movies", "http", "noerror", "timeout", "timeout"),
      ],
    )
    self.assertNotIn(d.status, {VerificationLifecycleStatus.DOMAIN_EXPIRED_CONFIRMED, VerificationLifecycleStatus.REMOVAL_CANDIDATE})
    self.assertFalse(d.removal_eligible)

  def test_02_another_isp_can_access_server_active_or_isp_restricted(self) -> None:
    d = classify_endpoint(
      _entry(),
      dns=DnsObservation("ftp.example-isp.net", summary="healthy"),
      http=HttpObservation("timeout"),
      isp_probes=[
        IspProbeRecord("link3", "Link3", "Dhaka", "2026-11-18T00:00:00Z", "http://ftp.example-isp.net/movies", "http", "noerror", "timeout", "timeout"),
        IspProbeRecord("amberit", "Amber IT", "Dhaka", "2026-11-18T00:00:00Z", "http://ftp.example-isp.net/movies", "http", "noerror", "reachable", "200"),
      ],
    )
    self.assertEqual(d.status, VerificationLifecycleStatus.ISP_RESTRICTED)
    self.assertFalse(d.removal_eligible)

  def test_03_private_rfc1918_ip_exempt_from_dead_classification(self) -> None:
    d = classify_endpoint(
      _entry(url="http://10.16.100.244", host="10.16.100.244", domain=None, is_ip=True, is_private=True),
      http=HttpObservation("ssrf_blocked"),
    )
    self.assertEqual(d.status, VerificationLifecycleStatus.UNKNOWN)
    self.assertFalse(d.removal_eligible)

  def test_04_single_resolver_nxdomain_stays_suspected(self) -> None:
    d = classify_endpoint(_entry(), dns=DnsObservation("ftp.example-isp.net", summary="single_nxdomain"))
    self.assertEqual(d.status, VerificationLifecycleStatus.DNS_NXDOMAIN_SUSPECTED)
    self.assertFalse(d.removal_eligible)

  def test_05_multiple_resolvers_disagree_stays_unknown(self) -> None:
    d = classify_endpoint(_entry(), dns=DnsObservation("ftp.example-isp.net", summary="disagreed"))
    self.assertEqual(d.status, VerificationLifecycleStatus.UNKNOWN)
    self.assertFalse(d.removal_eligible)

  def test_06_servfail_is_temporary_dns_failure(self) -> None:
    d = classify_endpoint(_entry(), dns=DnsObservation("ftp.example-isp.net", summary="servfail"))
    self.assertEqual(d.status, VerificationLifecycleStatus.DNS_TEMPORARY_FAILURE)
    self.assertFalse(d.removal_eligible)

  def test_07_dns_timeout_is_temporary_dns_failure(self) -> None:
    d = classify_endpoint(_entry(), dns=DnsObservation("ftp.example-isp.net", summary="timeout"))
    self.assertEqual(d.status, VerificationLifecycleStatus.DNS_TEMPORARY_FAILURE)
    self.assertFalse(d.removal_eligible)

  def test_08_http_401_auth_required_not_expired(self) -> None:
    d = classify_endpoint(_entry(), dns=DnsObservation("ftp.example-isp.net", summary="healthy"), http=HttpObservation("auth_401", status_code=401))
    self.assertEqual(d.status, VerificationLifecycleStatus.ISP_RESTRICTED)
    self.assertFalse(d.removal_eligible)

  def test_09_http_403_forbidden_not_expired(self) -> None:
    d = classify_endpoint(_entry(), dns=DnsObservation("ftp.example-isp.net", summary="healthy"), http=HttpObservation("forbidden_403", status_code=403))
    self.assertEqual(d.status, VerificationLifecycleStatus.ISP_RESTRICTED)
    self.assertFalse(d.removal_eligible)

  def test_10_http_404_one_path_does_not_expire_domain(self) -> None:
    d = classify_endpoint(_entry(), dns=DnsObservation("ftp.example-isp.net", summary="healthy"), http=HttpObservation("not_found_404", status_code=404))
    self.assertEqual(d.status, VerificationLifecycleStatus.TEMPORARILY_UNAVAILABLE)
    self.assertFalse(d.removal_eligible)

  def test_11_http_410_alone_investigates_without_auto_removal(self) -> None:
    d = classify_endpoint(_entry(), dns=DnsObservation("ftp.example-isp.net", summary="healthy"), http=HttpObservation("gone_410", status_code=410))
    self.assertEqual(d.status, VerificationLifecycleStatus.TEMPORARILY_UNAVAILABLE)
    self.assertFalse(d.removal_eligible)

  def test_12_http_429_rate_limited_not_expired(self) -> None:
    d = classify_endpoint(_entry(), http=HttpObservation("rate_limited_429", status_code=429))
    self.assertEqual(d.status, VerificationLifecycleStatus.TEMPORARILY_UNAVAILABLE)
    self.assertFalse(d.removal_eligible)

  def test_13_http_500_server_error_not_expired(self) -> None:
    d = classify_endpoint(_entry(), http=HttpObservation("server_error_5xx", status_code=500))
    self.assertEqual(d.status, VerificationLifecycleStatus.TEMPORARILY_UNAVAILABLE)
    self.assertFalse(d.removal_eligible)

  def test_14_tls_error_not_expired(self) -> None:
    d = classify_endpoint(_entry(), http=HttpObservation("tls_error", tls_error="CERT_HAS_EXPIRED"))
    self.assertEqual(d.status, VerificationLifecycleStatus.TEMPORARILY_UNAVAILABLE)
    self.assertFalse(d.removal_eligible)

  def test_15_ftp_auth_failure_not_expired(self) -> None:
    ftp_entry = _entry(url="ftp://files.example-isp.net", protocol="ftp", host="files.example-isp.net")
    d = classify_endpoint(ftp_entry, ftp=FtpObservation("login_rejected", greeting_code=530))
    self.assertEqual(d.status, VerificationLifecycleStatus.ISP_RESTRICTED)
    self.assertFalse(d.removal_eligible)

  def test_16_domain_lookup_unavailable_stays_unknown(self) -> None:
    d = classify_endpoint(
      _entry(),
      dns=DnsObservation("ftp.example-isp.net", summary="not_checked"),
      registration=RegistrationObservation("example-isp.net", "registration_lookup_unavailable"),
    )
    self.assertEqual(d.status, VerificationLifecycleStatus.UNKNOWN)
    self.assertFalse(d.removal_eligible)

  def test_17_bd_registration_source_unavailable_never_invents_rdap(self) -> None:
    client = RdapClient()
    obs = client.lookup_domain("iscreen.com.bd")
    self.assertEqual(obs.lookup_status, "registration_lookup_unavailable")
    self.assertIn(".bd", obs.limitation_note or "")

    d = classify_endpoint(
      _entry(url="https://iscreen.com.bd", host="iscreen.com.bd", domain="iscreen.com.bd"),
      dns=DnsObservation("iscreen.com.bd", summary="multi_nxdomain"),
      registration=obs,
    )
    self.assertEqual(d.status, VerificationLifecycleStatus.UNKNOWN)
    self.assertFalse(d.removal_eligible)

  def test_18_stale_isp_probe_data_excluded_from_current_proof(self) -> None:
    now = datetime(2026, 11, 18, 12, 0, tzinfo=timezone.utc)
    stale_ts = (now - timedelta(days=14)).strftime("%Y-%m-%dT%H:%M:%SZ")
    payload = {
      "schema_version": 1,
      "generated_at_utc": now.strftime("%Y-%m-%dT%H:%M:%SZ"),
      "probes": [
        {
          "isp_id": "link3",
          "isp_name": "Link3",
          "region": "Dhaka",
          "probed_at_utc": stale_ts,
          "endpoint_url": "http://ftp.example-isp.net/movies",
          "protocol": "http",
          "dns_result": "noerror",
          "connection_result": "reachable",
          "http_or_ftp_result": "200",
          "probe_software_version": "1.0.0",
        }
      ],
    }
    with tempfile.NamedTemporaryFile("w+", suffix=".json") as tf:
      json.dump(payload, tf)
      tf.flush()
      grouped, errs = load_isp_probes_by_key(Path(tf.name), now_utc=now, ttl_hours=168)
      self.assertEqual(errs, [])
      probes = grouped[canonical_key("http://ftp.example-isp.net/movies")]
      self.assertTrue(probes[0].is_stale)

    d = classify_endpoint(_entry(), isp_probes=probes, now_utc=now)
    self.assertEqual(d.status, VerificationLifecycleStatus.UNKNOWN)

  def test_19_no_isp_probe_data_stays_unverified(self) -> None:
    d = classify_endpoint(_entry())
    self.assertEqual(d.status, VerificationLifecycleStatus.UNKNOWN)
    self.assertEqual(d.audit_bucket, "Unverified")

  def test_20_rdap_hold_and_redemption_not_permanently_deleted(self) -> None:
    obs_hold = parse_rdap_domain_payload("hold.net", "https://rdap.verisign.com/net/v1", {"status": ["client hold"]}, "2026-11-18T00:00:00Z")
    self.assertEqual(obs_hold.lookup_status, "hold")
    d_hold = classify_endpoint(_entry(), dns=DnsObservation("hold.net", summary="multi_nxdomain"), registration=obs_hold)
    self.assertEqual(d_hold.status, VerificationLifecycleStatus.DOMAIN_EXPIRED_SUSPECTED)
    self.assertFalse(d_hold.removal_eligible)

    obs_redemption = parse_rdap_domain_payload("red.net", "https://rdap.verisign.com/net/v1", {"status": ["redemption period"]}, "2026-11-18T00:00:00Z")
    self.assertEqual(obs_redemption.lookup_status, "redemption_period")
    d_red = classify_endpoint(_entry(), dns=DnsObservation("red.net", summary="multi_nxdomain"), registration=obs_redemption)
    self.assertEqual(d_red.status, VerificationLifecycleStatus.DOMAIN_EXPIRED_SUSPECTED)
    self.assertFalse(d_red.removal_eligible)

  def test_21_confirmed_domain_deletion_eligible_for_removal_review(self) -> None:
    now = datetime(2026, 11, 18, 12, 0, tzinfo=timezone.utc)
    prior = PriorObservation(
      status=VerificationLifecycleStatus.DOMAIN_EXPIRED_CONFIRMED,
      checked_at_utc=(now - timedelta(hours=36)).strftime("%Y-%m-%dT%H:%M:%SZ"),
      dns_summary="auth_nxdomain",
      registration_status="unregistered_404",
    )
    d = classify_endpoint(
      _entry(url="http://deleted-domain-example.com", host="deleted-domain-example.com", domain="deleted-domain-example.com"),
      dns=DnsObservation("deleted-domain-example.com", summary="auth_nxdomain"),
      registration=RegistrationObservation("deleted-domain-example.com", "unregistered_404", authoritative_source="https://rdap.verisign.com/com/v1"),
      prior=prior,
      recheck_hours=24,
      now_utc=now,
    )
    self.assertEqual(d.status, VerificationLifecycleStatus.REMOVAL_CANDIDATE)
    self.assertTrue(d.removal_eligible)
    self.assertEqual(d.audit_bucket, "Removal Candidates")

  def test_22_archived_entry_respects_governance_and_reports_render_cleanly(self) -> None:
    d_arch = classify_endpoint(_entry(), governance_override=VerificationLifecycleStatus.ARCHIVED)
    self.assertEqual(d_arch.status, VerificationLifecycleStatus.ARCHIVED)
    self.assertEqual(d_arch.audit_bucket, "Archived")

    d_restored = classify_endpoint(_entry(), dns=DnsObservation("ftp.example-isp.net", summary="healthy"), http=HttpObservation("reachable_2xx", status_code=200))
    self.assertEqual(d_restored.status, VerificationLifecycleStatus.ACTIVE)

    rep_json = build_json_report([d_arch, d_restored], "2026-11-18T12:00:00Z")
    self.assertEqual(rep_json["summary"]["total_entries"], 2)
    md = build_markdown_report(rep_json)
    self.assertIn("Confirmed Active", md)
    self.assertIn("Archived", md)


if __name__ == "__main__":
  unittest.main()
