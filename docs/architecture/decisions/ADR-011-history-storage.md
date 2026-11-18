# ADR-011: Local Testing History Storage

**Status:** Accepted (Phase 2 — Implemented in Phase 9)  
**Date:** 2026-11-18

## Context

Users requested the ability to cross-check previously tested URLs and compare outcomes across networks.

## Requirements

- Privacy-conscious local-only storage (no Wi-Fi SSID/location permission).
- Bounded retention and user-triggered deletion.

## Alternatives

1. Cloud-synced test history requiring accounts.
2. In-memory only history lost on process death.
3. Retention-bounded local Room table with optional user-typed network profile label.

## Selected Approach

Store local scan history entries in Room (Phase 9) with configurable retention limit (e.g., max days / max rows) and a one-tap "Clear History" action.

## Reason

Satisfies user cross-checking needs while maintaining zero-account privacy.

## Trade-offs

History does not sync automatically across devices (export/import handles portability).

## Future Consequences

`ServerTestResult` models in Phase 2 already mirror the planned Room entity fields.
