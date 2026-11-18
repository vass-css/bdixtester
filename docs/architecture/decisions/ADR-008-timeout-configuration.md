# ADR-008: Timeout Configuration Strategy

**Status:** Accepted (Phase 2 — Applied in Phase 5–6)  
**Date:** 2026-11-18

## Context

Local BDIX endpoints respond in milliseconds when reachable, whereas unreachable cross-ISP or private IPv4 hosts can hang for OS TCP defaults (60+ seconds).

## Requirements

- Separate Quick Scan and Full Scan timeout profiles.
- Distinct `TIMED_OUT` classification vs. `UNREACHABLE` (refused/DNS failure).

## Alternatives

1. Fixed 30s OS default timeout.
2. Configurable Quick Scan (2.5s connect / 2.5s read) and Full Scan (5s connect / 6s read) profiles.

## Selected Approach

Configurable mode-specific connect and read timeouts with explicit UI disclosure that Quick Scan's shorter timeout may leave slower hosts inconclusive.

## Reason

Keeps batch scans predictable and responsive while allowing thorough checks in Full Scan.

## Trade-offs

Very slow legitimate servers (> 2.5s) may time out in Quick Scan unless retested in Full Scan.

## Future Consequences

User-adjustable timeout sliders in Settings (Phase 6+) bind directly to `ScanConfig`.
