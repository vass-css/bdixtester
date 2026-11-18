# ADR-012: Data Export via Storage Access Framework

**Status:** Accepted (Phase 2 — Implemented in Phase 9)  
**Date:** 2026-11-18

## Context

Users asked to export working server lists and scan summaries without granting broad external storage permissions.

## Requirements

- Support CSV, JSON, and plain text formats.
- Zero `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` permissions.

## Alternatives

1. Writing directly to `/sdcard/Download` with legacy storage permissions.
2. Android Storage Access Framework (`ActivityResultContracts.CreateDocument`).

## Selected Approach

Use Android SAF (`CreateDocument`) in Phase 9 to stream serialized CSV/JSON/Text snapshots to user-chosen URIs.

## Reason

Fully compliant with Google Play scoped-storage and least-privilege policies.

## Trade-offs

Requires user interaction through the system document picker per export.

## Future Consequences

No storage permissions are ever declared in `AndroidManifest.xml`.
