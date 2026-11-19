from __future__ import annotations

import json
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Any
from urllib.parse import urlparse

from .models import CatalogEntry, VerificationLifecycleStatus
from .url_utils import (
  SUPPORTED_SCHEMES,
  canonical_key,
  extract_registrable_domain,
  is_rfc1918_private_ip,
  parse_ip,
)

BUILTIN_ENTRY_RE = re.compile(
  r'BuiltInCatalogEntry\(\s*"([^"]+)",\s*"([^"]*)",\s*ServerCategory\.([A-Z_]+)\s*,?\s*\)',
  re.S,
)
OTT_ENTRY_RE = re.compile(
  r'OttPlatform\(\s*id\s*=\s*"([^"]+)",\s*name\s*=\s*"([^"]+)",\s*url\s*=\s*"([^"]+)"',
  re.S,
)


@dataclass(frozen=True)
class OfflineValidationReport:
  valid: bool
  curated_count: int
  ott_count: int
  ftp_json_endpoints: int
  live_tv_json_endpoints: int
  duplicate_keys_detected: list[str]
  invalid_urls: list[str]
  unsupported_protocols: list[str]
  json_errors: list[str]
  governance_errors: list[str]

  def summary_lines(self) -> list[str]:
    return [
      f"Offline catalog validation: {'PASSED' if self.valid else 'FAILED'}",
      f"  Curated BDIX entries (Kotlin): {self.curated_count}",
      f"  OTT Directory entries (Kotlin): {self.ott_count}",
      f"  Canonical JSON FTP/Movies endpoints: {self.ftp_json_endpoints}",
      f"  Canonical JSON Live TV endpoints: {self.live_tv_json_endpoints}",
      f"  Duplicate canonical keys in curated/OTT: {len(self.duplicate_keys_detected)}",
      f"  Invalid URLs: {len(self.invalid_urls)}",
      f"  Unsupported protocols: {len(self.unsupported_protocols)}",
      f"  JSON/Schema errors: {len(self.json_errors)}",
      f"  Governance manifest errors: {len(self.governance_errors)}",
    ]


def _make_entry(
  entry_id: str,
  raw_url: str,
  isp_name: str,
  category: str,
  source_file: str,
) -> CatalogEntry:
  cleaned_url = raw_url.strip()
  parsed = urlparse(cleaned_url)
  scheme = (parsed.scheme or "").lower()
  host = (parsed.hostname or "").strip()
  ip = parse_ip(host) if host else None
  return CatalogEntry(
    entry_id=entry_id,
    url=cleaned_url,
    canonical_key=canonical_key(cleaned_url),
    category=category,
    isp_name=isp_name.strip() or host,
    protocol=scheme if scheme in SUPPORTED_SCHEMES else "unsupported",
    host=host,
    explicit_port=parsed.port,
    normalized_domain=None if ip is not None else extract_registrable_domain(host),
    is_ip_literal=ip is not None,
    is_private_ip=is_rfc1918_private_ip(host) if ip is not None else False,
    source_file=source_file,
  )


def load_bundled_catalog_entries(repo_root: Path) -> tuple[list[CatalogEntry], list[CatalogEntry]]:
  catalog_dir = repo_root / "app/src/main/java/com/jubayerhossain/bdixtester/data/catalog"
  kt_sources = [
    catalog_dir / "BundledFtpCatalog.kt",
    catalog_dir / "BundledFtpCatalogPart2.kt",
    catalog_dir / "BundledMediaCatalog.kt",
  ]
  curated: list[CatalogEntry] = []
  seen_curated: set[str] = set()
  idx = 0
  for src in kt_sources:
    if not src.exists():
      continue
    text = src.read_text(encoding="utf-8")
    for raw_url, isp_name, category in BUILTIN_ENTRY_RE.findall(text):
      entry = _make_entry(
        entry_id=f"builtin:{idx}",
        raw_url=raw_url,
        isp_name=isp_name,
        category=category,
        source_file=src.name,
      )
      if entry.canonical_key and entry.canonical_key not in seen_curated:
        seen_curated.add(entry.canonical_key)
        curated.append(entry)
        idx += 1

  ott_entries: list[CatalogEntry] = []
  ott_file = catalog_dir / "BdOttPlatforms.kt"
  if ott_file.exists():
    text = ott_file.read_text(encoding="utf-8")
    for ott_id, ott_name, raw_url in OTT_ENTRY_RE.findall(text):
      ott_entries.append(
        _make_entry(
          entry_id=ott_id,
          raw_url=raw_url,
          isp_name=ott_name,
          category="OTT_PLATFORM",
          source_file=ott_file.name,
        ),
      )

  return curated, ott_entries


def load_governance_manifest(manifest_path: Path) -> tuple[dict[str, dict[str, Any]], list[str]]:
  if not manifest_path.exists():
    return {}, []
  try:
    payload = json.loads(manifest_path.read_text(encoding="utf-8"))
  except Exception as exc:
    return {}, [f"Invalid JSON in governance manifest {manifest_path}: {exc}"]

  errors: list[str] = []
  if not isinstance(payload, dict):
    return {}, ["Governance manifest root must be a JSON object."]

  valid_statuses = {s.value for s in VerificationLifecycleStatus}
  records: dict[str, dict[str, Any]] = {}
  entries = payload.get("entries", [])
  if not isinstance(entries, list):
    errors.append("Governance manifest 'entries' must be a list.")
    return {}, errors

  for i, rec in enumerate(entries):
    if not isinstance(rec, dict):
      errors.append(f"Governance entry #{i} must be an object.")
      continue
    key = rec.get("canonical_key") or rec.get("entry_id")
    status = rec.get("status")
    if not isinstance(key, str) or not key.strip():
      errors.append(f"Governance entry #{i} missing 'canonical_key' or 'entry_id'.")
      continue
    if status not in valid_statuses:
      errors.append(f"Governance entry '{key}' has unknown status '{status}'.")
      continue
    if status == VerificationLifecycleStatus.ARCHIVED.value and not rec.get("confirmed_by"):
      errors.append(f"Archived entry '{key}' must record explicit 'confirmed_by' provenance.")
    records[key] = rec

  return records, errors


def validate_repository_catalogs(repo_root: Path) -> OfflineValidationReport:
  curated, ott = load_bundled_catalog_entries(repo_root)
  all_entries = curated + ott

  duplicate_keys: list[str] = []
  seen: set[str] = set()
  invalid_urls: list[str] = []
  unsupported_protocols: list[str] = []

  for e in all_entries:
    if not e.url or not e.host:
      invalid_urls.append(f"{e.entry_id}: {e.url}")
    if e.protocol not in SUPPORTED_SCHEMES:
      unsupported_protocols.append(f"{e.entry_id}: {e.url}")
    if e.canonical_key in seen and e.category != "OTT_PLATFORM":
      duplicate_keys.append(e.canonical_key)
    seen.add(e.canonical_key)

  json_errors: list[str] = []
  ftp_json_count = 0
  live_tv_json_count = 0

  ftp_json_path = repo_root / "public/assets/aistudio/bdix_ftp_movies_catalog_4acd8ccb.json"
  if ftp_json_path.exists():
    try:
      data = json.loads(ftp_json_path.read_text(encoding="utf-8"))
      for key in ("schema_version", "catalog_name", "isps", "server_index", "statistics"):
        if key not in data:
          json_errors.append(f"{ftp_json_path.name}: missing '{key}'")
      server_idx = data.get("server_index", {})
      if isinstance(server_idx, dict):
        ftp_list = server_idx.get("ftp", [])
        movie_list = server_idx.get("movies", [])
        if isinstance(ftp_list, list) and isinstance(movie_list, list):
          ftp_json_count = len(ftp_list) + len(movie_list)
          if ftp_json_count == 0:
            json_errors.append(f"{ftp_json_path.name}: server_index ftp/movies is empty")
        else:
          json_errors.append(f"{ftp_json_path.name}: server_index lists invalid")
    except Exception as exc:
      json_errors.append(f"{ftp_json_path.name}: invalid JSON ({exc})")
  else:
    json_errors.append(f"Missing canonical JSON: {ftp_json_path}")

  tv_json_path = repo_root / "public/assets/aistudio/bdix_live_tv_catalog_70b3542f.json"
  if tv_json_path.exists():
    try:
      data = json.loads(tv_json_path.read_text(encoding="utf-8"))
      for key in ("schema_version", "catalog_name", "isps", "server_index", "statistics"):
        if key not in data:
          json_errors.append(f"{tv_json_path.name}: missing '{key}'")
      server_idx = data.get("server_index", {})
      if isinstance(server_idx, dict):
        tv_list = server_idx.get("live_tv", [])
        if isinstance(tv_list, list):
          live_tv_json_count = len(tv_list)
          if live_tv_json_count == 0:
            json_errors.append(f"{tv_json_path.name}: server_index live_tv is empty")
        else:
          json_errors.append(f"{tv_json_path.name}: live_tv list invalid")
    except Exception as exc:
      json_errors.append(f"{tv_json_path.name}: invalid JSON ({exc})")
  else:
    json_errors.append(f"Missing canonical JSON: {tv_json_path}")

  manifest_path = repo_root / "config/catalog_governance_manifest.json"
  _, gov_errors = load_governance_manifest(manifest_path)

  if len(curated) == 0:
    json_errors.append("No curated BDIX catalog entries were parsed.")

  valid = not (invalid_urls or unsupported_protocols or json_errors or gov_errors or duplicate_keys)
  return OfflineValidationReport(
    valid=valid,
    curated_count=len(curated),
    ott_count=len(ott),
    ftp_json_endpoints=ftp_json_count,
    live_tv_json_endpoints=live_tv_json_count,
    duplicate_keys_detected=duplicate_keys,
    invalid_urls=invalid_urls,
    unsupported_protocols=unsupported_protocols,
    json_errors=json_errors,
    governance_errors=gov_errors,
  )
