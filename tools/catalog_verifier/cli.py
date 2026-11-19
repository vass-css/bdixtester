from __future__ import annotations

import argparse
import concurrent.futures
import json
import sys
import threading
import time
from pathlib import Path

from .catalog_loader import (
  load_bundled_catalog_entries,
  load_governance_manifest,
  validate_repository_catalogs,
)
from .decision_engine import classify_endpoint
from .isp_probes import load_isp_probes_by_key, validate_isp_probe_payload
from .models import (
  ClassificationDecision,
  DnsObservation,
  PriorObservation,
  RegistrationObservation,
  VerificationLifecycleStatus,
)
from .network_probes import (
  RdapClient,
  probe_ftp_endpoint,
  probe_http_endpoint,
  utc_now_iso,
  verify_domain_dns,
)
from .reporting import write_reports


def _load_prior_observations(prior_json: Path | None) -> dict[str, PriorObservation]:
  if prior_json is None or not prior_json.exists():
    return {}
  data = json.loads(prior_json.read_text(encoding="utf-8"))
  out: dict[str, PriorObservation] = {}
  for item in data.get("entries", []):
    key = item.get("entry", {}).get("canonical_key")
    if not key:
      continue
    try:
      status = VerificationLifecycleStatus(item["status"])
    except Exception:
      continue
    out[key] = PriorObservation(
      status=status,
      checked_at_utc=item.get("checked_at_utc", ""),
      dns_summary=(item.get("dns") or {}).get("summary"),
      registration_status=(item.get("registration") or {}).get("lookup_status"),
    )
  return out


def cmd_validate_catalog(args: argparse.Namespace) -> int:
  report = validate_repository_catalogs(Path(args.repo_root).resolve())
  for line in report.summary_lines():
    print(line)
  for err in report.duplicate_keys_detected + report.invalid_urls + report.unsupported_protocols + report.json_errors + report.governance_errors:
    print(f"  ERROR: {err}")
  return 0 if report.valid else 1


def cmd_validate_isp_probes(args: argparse.Namespace) -> int:
  p = Path(args.input).resolve()
  if not p.exists():
    print(f"ERROR: ISP probe file not found: {p}", file=sys.stderr)
    return 1
  raw = json.loads(p.read_text(encoding="utf-8"))
  errors = validate_isp_probe_payload(raw)
  if errors:
    for err in errors:
      print(f"ERROR: {err}", file=sys.stderr)
    return 1
  grouped, _ = load_isp_probes_by_key(p, ttl_hours=args.ttl_hours)
  total = sum(len(v) for v in grouped.values())
  stale = sum(1 for v in grouped.values() for r in v if r.is_stale)
  print(f"ISP probe validation PASSED: {total} records across {len(grouped)} endpoints ({stale} stale).")
  return 0


def cmd_verify_live(args: argparse.Namespace) -> int:
  repo_root = Path(args.repo_root).resolve()
  curated, ott = load_bundled_catalog_entries(repo_root)
  entries = curated + (ott if args.include_ott else [])
  if args.limit and args.limit > 0:
    entries = entries[: args.limit]

  gov_map, gov_errors = load_governance_manifest(repo_root / "config/catalog_governance_manifest.json")
  if gov_errors:
    for err in gov_errors:
      print(f"WARNING: governance error: {err}", file=sys.stderr)

  isp_map = {}
  if args.isp_probes:
    isp_map, isp_errors = load_isp_probes_by_key(Path(args.isp_probes).resolve(), ttl_hours=args.isp_ttl_hours)
    if isp_errors:
      for err in isp_errors:
        print(f"ERROR: ISP probe file invalid: {err}", file=sys.stderr)
      return 1

  prior_map = _load_prior_observations(Path(args.prior_report).resolve() if args.prior_report else None)

  # Step 1: Domain-level DNS & RDAP verification once per unique domain/host (strict rate limiting)
  unique_domains: dict[str, str | None] = {}
  for e in entries:
    if not e.is_ip_literal and e.host:
      unique_domains[e.host.lower()] = e.normalized_domain

  dns_cache: dict[str, DnsObservation] = {}
  rdap_cache: dict[str, RegistrationObservation] = {}
  rdap_client = RdapClient(timeout_sec=args.timeout)
  rdap_lock = threading.Lock()

  if not args.offline_only:
    def _check_domain(host: str, reg_domain: str | None) -> tuple[str, DnsObservation, RegistrationObservation]:
      time.sleep(args.domain_delay)
      dns_obs = verify_domain_dns(host, registrable_domain=reg_domain, timeout_sec=args.timeout)
      reg_key = reg_domain or host
      with rdap_lock:
        reg_obs = rdap_cache.get(reg_key)
      if reg_obs is None:
        reg_obs = rdap_client.lookup_domain(reg_key)
        with rdap_lock:
          rdap_cache[reg_key] = reg_obs
      return host, dns_obs, reg_obs

    with concurrent.futures.ThreadPoolExecutor(max_workers=max(1, min(args.max_workers, 8))) as pool:
      futs = [pool.submit(_check_domain, h, r) for h, r in unique_domains.items()]
      for fut in concurrent.futures.as_completed(futs):
        host, dns_obs, reg_obs = fut.result()
        dns_cache[host] = dns_obs

  # Step 2: Endpoint HTTP/FTP probe + conservative classification
  def _verify_entry(entry) -> ClassificationDecision:
    dns_obs = dns_cache.get(entry.host.lower()) if not entry.is_ip_literal else None
    reg_key = entry.normalized_domain or entry.host.lower()
    reg_obs = rdap_cache.get(reg_key) if not entry.is_ip_literal else None

    http_obs = None
    ftp_obs = None
    if not args.offline_only and not entry.is_private_ip and not args.dns_only:
      if entry.protocol in ("http", "https"):
        # Skip active HTTP connection if DNS already failed globally with NXDOMAIN
        if dns_obs is None or dns_obs.summary in ("healthy", "nodata", "disagreed", "not_checked"):
          http_obs = probe_http_endpoint(entry.url, timeout_sec=args.timeout)
      elif entry.protocol in ("ftp", "ftps"):
        if dns_obs is None or dns_obs.summary in ("healthy", "nodata", "disagreed", "not_checked"):
          ftp_obs = probe_ftp_endpoint(entry.url, timeout_sec=args.timeout)

    gov_rec = gov_map.get(entry.canonical_key) or gov_map.get(entry.entry_id)
    gov_override = None
    if gov_rec and gov_rec.get("status"):
      gov_override = VerificationLifecycleStatus(gov_rec["status"])

    return classify_endpoint(
      entry=entry,
      dns=dns_obs,
      registration=reg_obs,
      http=http_obs,
      ftp=ftp_obs,
      isp_probes=isp_map.get(entry.canonical_key, []),
      prior=prior_map.get(entry.canonical_key),
      governance_override=gov_override,
      recheck_hours=args.recheck_hours,
    )

  decisions: list[ClassificationDecision] = []
  with concurrent.futures.ThreadPoolExecutor(max_workers=max(1, min(args.max_workers, 12))) as pool:
    decisions = list(pool.map(_verify_entry, entries))

  payload = write_reports(
    decisions=decisions,
    generated_at_utc=utc_now_iso(),
    json_out=Path(args.json_out).resolve() if args.json_out else None,
    md_out=Path(args.md_out).resolve() if args.md_out else None,
    metadata={
      "environment_note": args.env_note,
      "recheck_hours": args.recheck_hours,
      "offline_only": bool(args.offline_only),
      "dns_only": bool(args.dns_only),
      "isp_probes_supplied": bool(args.isp_probes),
    },
  )
  print(f"Verification complete: {payload['summary']['total_entries']} endpoints.")
  for b, c in payload["summary"]["by_audit_bucket"].items():
    print(f"  {b}: {c}")
  return 0


def cmd_show_candidates(args: argparse.Namespace) -> int:
  data = json.loads(Path(args.report_json).resolve().read_text(encoding="utf-8"))
  candidates = [e for e in data.get("entries", []) if e.get("status") in ("removal_candidate", "domain_expired_confirmed")]
  print(f"Found {len(candidates)} candidate/confirmed expired domain entries:")
  for item in candidates:
    e = item["entry"]
    print(f"- [{item['status']}] {e['url']} ({e['category']}, {item['confidence']}) -> {item['reason']}")
  return 0


def build_parser() -> argparse.ArgumentParser:
  parser = argparse.ArgumentParser(prog="catalog_verifier", description="BDIX Tester Safe Catalog Verifier")
  sub = parser.add_subparsers(dest="command", required=True)

  p_val = sub.add_parser("validate-catalog", help="Run deterministic offline catalog & governance validation")
  p_val.add_argument("--repo-root", default=".")
  p_val.set_defaults(func=cmd_validate_catalog)

  p_isp = sub.add_parser("validate-isp-probes", help="Validate external BD ISP probe JSON schema & freshness")
  p_isp.add_argument("--input", required=True)
  p_isp.add_argument("--ttl-hours", type=int, default=168)
  p_isp.set_defaults(func=cmd_validate_isp_probes)

  p_live = sub.add_parser("verify-live", help="Run conservative DNS, RDAP, and endpoint verification")
  p_live.add_argument("--repo-root", default=".")
  p_live.add_argument("--include-ott", action="store_true", default=True)
  p_live.add_argument("--no-include-ott", action="store_false", dest="include_ott")
  p_live.add_argument("--isp-probes", default=None)
  p_live.add_argument("--isp-ttl-hours", type=int, default=168)
  p_live.add_argument("--prior-report", default=None)
  p_live.add_argument("--recheck-hours", type=int, default=24)
  p_live.add_argument("--max-workers", type=int, default=8)
  p_live.add_argument("--timeout", type=float, default=4.0)
  p_live.add_argument("--domain-delay", type=float, default=0.05)
  p_live.add_argument("--limit", type=int, default=0)
  p_live.add_argument("--offline-only", action="store_true")
  p_live.add_argument("--dns-only", action="store_true")
  p_live.add_argument("--env-note", default="Public network runner (no direct Bangladesh BDIX peering)")
  p_live.add_argument("--json-out", default="build/reports/catalog-audit-report.json")
  p_live.add_argument("--md-out", default="build/reports/catalog-audit-report.md")
  p_live.set_defaults(func=cmd_verify_live)

  p_cand = sub.add_parser("show-candidates", help="List removal candidates from a JSON audit report")
  p_cand.add_argument("--report-json", required=True)
  p_cand.set_defaults(func=cmd_show_candidates)

  return parser


def main(argv: list[str] | None = None) -> int:
  parser = build_parser()
  args = parser.parse_args(argv)
  return int(args.func(args))


if __name__ == "__main__":
  raise SystemExit(main())
