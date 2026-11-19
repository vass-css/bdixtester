from __future__ import annotations

import json
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Any

from .models import IspProbeRecord
from .url_utils import canonical_key

DEFAULT_PROBE_TTL_HOURS = 168  # 7 days
REQUIRED_RECORD_FIELDS = {
  "isp_id",
  "isp_name",
  "region",
  "probed_at_utc",
  "endpoint_url",
  "protocol",
  "dns_result",
  "connection_result",
  "http_or_ftp_result",
  "probe_software_version",
}


def parse_utc_iso(ts: str) -> datetime:
  cleaned = ts.strip()
  if cleaned.endswith("Z"):
    cleaned = cleaned[:-1] + "+00:00"
  dt = datetime.fromisoformat(cleaned)
  if dt.tzinfo is None:
    dt = dt.replace(tzinfo=timezone.utc)
  return dt.astimezone(timezone.utc)


def validate_isp_probe_payload(payload: Any) -> list[str]:
  errors: list[str] = []
  if not isinstance(payload, dict):
    return ["Root ISP probe payload must be a JSON object."]
  if not isinstance(payload.get("schema_version"), (int, str)):
    errors.append("Missing or invalid 'schema_version'.")
  if not isinstance(payload.get("generated_at_utc"), str) or not payload["generated_at_utc"].strip():
    errors.append("Missing 'generated_at_utc' ISO-8601 timestamp.")
  else:
    try:
      parse_utc_iso(payload["generated_at_utc"])
    except ValueError:
      errors.append("Invalid 'generated_at_utc' ISO-8601 format.")

  probes = payload.get("probes")
  if not isinstance(probes, list):
    errors.append("'probes' must be a list of ISP probe objects.")
    return errors

  for idx, item in enumerate(probes):
    if not isinstance(item, dict):
      errors.append(f"Probe #{idx} must be a JSON object.")
      continue
    missing = sorted(REQUIRED_RECORD_FIELDS - set(item.keys()))
    if missing:
      errors.append(f"Probe #{idx} missing required fields: {', '.join(missing)}")
      continue
    for str_key in ("isp_id", "isp_name", "endpoint_url", "protocol", "dns_result", "connection_result", "http_or_ftp_result", "probe_software_version"):
      if not isinstance(item.get(str_key), str) or not item[str_key].strip():
        errors.append(f"Probe #{idx} field '{str_key}' must be a non-empty string.")
    try:
      parse_utc_iso(str(item["probed_at_utc"]))
    except ValueError:
      errors.append(f"Probe #{idx} has invalid 'probed_at_utc': {item.get('probed_at_utc')}")
    rt = item.get("response_time_ms")
    if rt is not None and (not isinstance(rt, int) or rt < 0):
      errors.append(f"Probe #{idx} has invalid 'response_time_ms': {rt}")
  return errors


def load_isp_probes_by_key(
  json_path: Path,
  now_utc: datetime | None = None,
  ttl_hours: int = DEFAULT_PROBE_TTL_HOURS,
) -> tuple[dict[str, list[IspProbeRecord]], list[str]]:
  raw = json.loads(json_path.read_text(encoding="utf-8"))
  errors = validate_isp_probe_payload(raw)
  if errors:
    return {}, errors

  ref_now = now_utc or datetime.now(timezone.utc)
  max_age = timedelta(hours=max(1, ttl_hours))
  grouped: dict[str, list[IspProbeRecord]] = {}

  for item in raw.get("probes", []):
    probed_dt = parse_utc_iso(item["probed_at_utc"])
    is_stale = (ref_now - probed_dt) > max_age or probed_dt > (ref_now + timedelta(minutes=15))
    record = IspProbeRecord(
      isp_id=item["isp_id"].strip(),
      isp_name=item["isp_name"].strip(),
      region=item["region"].strip(),
      probed_at_utc=item["probed_at_utc"].strip(),
      endpoint_url=item["endpoint_url"].strip(),
      protocol=item["protocol"].strip().lower(),
      dns_result=item["dns_result"].strip().lower(),
      connection_result=item["connection_result"].strip().lower(),
      http_or_ftp_result=str(item["http_or_ftp_result"]).strip().lower(),
      response_time_ms=item.get("response_time_ms"),
      error_info=item.get("error_info"),
      probe_software_version=item["probe_software_version"].strip(),
      is_stale=is_stale,
    )
    key = canonical_key(record.endpoint_url)
    grouped.setdefault(key, []).append(record)

  return grouped, []
