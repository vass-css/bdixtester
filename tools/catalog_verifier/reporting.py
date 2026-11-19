from __future__ import annotations

import json
from collections import Counter
from pathlib import Path
from typing import Any

from .models import ClassificationDecision

AUDIT_BUCKETS = [
  "Confirmed Active",
  "ISP Restricted",
  "Unreachable From Current Network",
  "Temporarily Unavailable",
  "Unverified",
  "Expired Suspected",
  "Domain Expired Confirmed",
  "Removal Candidates",
  "Archived",
]


def build_json_report(
  decisions: list[ClassificationDecision],
  generated_at_utc: str,
  metadata: dict[str, Any] | None = None,
) -> dict[str, Any]:
  bucket_counts = Counter(d.audit_bucket for d in decisions)
  status_counts = Counter(d.status.value for d in decisions)
  category_counts = Counter(d.entry.category for d in decisions)

  return {
    "schema_version": 1,
    "generated_at_utc": generated_at_utc,
    "metadata": metadata or {},
    "summary": {
      "total_entries": len(decisions),
      "by_category": dict(category_counts),
      "by_audit_bucket": {b: bucket_counts.get(b, 0) for b in AUDIT_BUCKETS},
      "by_status": dict(status_counts),
      "removal_candidate_count": status_counts.get("removal_candidate", 0),
    },
    "entries": [d.to_dict() for d in decisions],
  }


def build_markdown_report(report_json: dict[str, Any]) -> str:
  summary = report_json["summary"]
  meta = report_json.get("metadata", {})
  lines: list[str] = [
    "# BDIX Tester Catalog Verification & Audit Report",
    "",
    f"- **Generated (UTC)**: `{report_json['generated_at_utc']}`",
    f"- **Total Audited Endpoints**: **{summary['total_entries']}**",
    f"- **Network Environment**: `{meta.get('environment_note', 'Standard public execution environment')}`",
    "",
    "## Category Breakdown",
    "",
  ]
  for cat, count in sorted(summary["by_category"].items()):
    lines.append(f"- `{cat}`: **{count}**")

  lines.extend(["", "## Audit Summary by Classification Bucket", "", "| Bucket | Count |", "|---|---:|"])
  for bucket in AUDIT_BUCKETS:
    lines.append(f"| {bucket} | {summary['by_audit_bucket'].get(bucket, 0)} |")

  candidates = [e for e in report_json["entries"] if e["status"] in ("removal_candidate", "domain_expired_confirmed")]
  lines.extend(["", "## Removal Candidates & Confirmed Expired Domains", ""])
  if not candidates:
    lines.append("No endpoints met conservative multi-source removal-candidate criteria in this audit.")
  else:
    lines.extend(["| ID | URL | Category | Status | Confidence | Reason |", "|---|---|---|---|---|---|"])
    for item in candidates:
      e = item["entry"]
      lines.append(
        f"| `{e['entry_id']}` | `{e['url']}` | `{e['category']}` | `{item['status']}` | `{item['confidence']}` | {item['reason']} |"
      )

  lines.extend(["", "## Sample Entries Across Audit Buckets", ""])
  for bucket in AUDIT_BUCKETS:
    matched = [e for e in report_json["entries"] if e["audit_bucket"] == bucket]
    lines.append(f"### {bucket} ({len(matched)})")
    if not matched:
      lines.append("- None")
    else:
      for item in matched[:15]:
        e = item["entry"]
        lines.append(f"- `{e['entry_id']}` (`{e['category']}`) — `{e['url']}` — {item['reason']}")
      if len(matched) > 15:
        lines.append(f"- ... and {len(matched) - 15} more entries (see JSON report for complete evidence).")
    lines.append("")

  return "\n".join(lines).rstrip() + "\n"


def write_reports(
  decisions: list[ClassificationDecision],
  generated_at_utc: str,
  json_out: Path | None,
  md_out: Path | None,
  metadata: dict[str, Any] | None = None,
) -> dict[str, Any]:
  payload = build_json_report(decisions, generated_at_utc, metadata=metadata)
  if json_out is not None:
    json_out.parent.mkdir(parents=True, exist_ok=True)
    json_out.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
  if md_out is not None:
    md_out.parent.mkdir(parents=True, exist_ok=True)
    md_out.write_text(build_markdown_report(payload), encoding="utf-8")
  return payload
