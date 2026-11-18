# ADR-009: Remote Catalog Update Strategy

**Status:** Accepted (Phase 2 — Implemented in Phase 8)  
**Date:** 2026-11-18

## Context

Public Play Store feedback highlighted stale/dead entries when server catalogs cannot refresh independently of app releases.

## Requirements

- Low bandwidth consumption.
- Atomic, rollback-safe updates over HTTPS-only.
- Offline fallback to built-in catalog.

## Alternatives

1. Shipping catalog updates only via Play Store APK releases.
2. Unconditional full JSON download on every app launch.
3. Versioned HTTPS JSON with `ETag`/`If-None-Match` conditional caching, schema validation, and WorkManager metered/battery constraints.

## Selected Approach

Conditional HTTPS JSON synchronization with schema/version validation, atomic transaction swap, retention of last-known-good catalog, and optional WorkManager background refresh.

## Reason

Keeps data usage tiny (`304 Not Modified` when unchanged) and prevents malformed responses from corrupting local data.

## Trade-offs

Requires maintaining a versioned static JSON endpoint (to be selected/hosted before Phase 8).

## Future Consequences

No production URL is hardcoded in Phases 1–3; hosting can be configured cleanly in Phase 8.
