# ADR-010: Catalog Verification Model

**Status:** Accepted (Phase 2)  
**Date:** 2026-11-18

## Context

The reference app markets servers as "human-verified", yet users encounter dead endpoints, and automated reachability from one ISP does not prove BDIX peering.

## Requirements

- Transparent provenance and review timestamps.
- Clear separation between live reachability and human verification.

## Alternatives

1. Labeling every HTTP `200` server as "BDIX Verified".
2. Explicit `VerificationState` (`UNVERIFIED`, `COMMUNITY_SUBMITTED`, `REVIEWED`, `FLAGGED_STALE`) paired with `"Reachable from current network"` live status.

## Selected Approach

Keep `VerificationState` + `lastReviewedAtEpochMs` strictly as catalog provenance metadata, separate from live `TestStatus`.

## Reason

Prevents misleading users about ISP-specific reachability or claiming unverified human review.

## Trade-offs

Requires disciplined catalog curation metadata when remote catalogs are authored.

## Future Consequences

Users can inspect exact provenance and review dates inside `ServerDetailsScreen`.
