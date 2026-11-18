# ADR-005: HTTP/HTTPS Network Client Selection

**Status:** Accepted (Phase 2 — Client Wiring Deferred to Phase 5)  
**Date:** 2026-11-18

## Context

Phase 5 requires lightweight HTTP/HTTPS reachability probing (`HEAD` with bounded `GET` fallback), explicit connect/read timeouts, TLS verification, and streaming speed measurement.

## Requirements

- Fine-grained timeout control and cancellation support.
- Access to DNS/TCP/TLS timing via event callbacks.
- Streaming response body support without buffering entire downloads in memory.

## Alternatives

1. `HttpURLConnection` (verbose cancellation & event timing).
2. Ktor Client.
3. OkHttp (`4.10.0`).

## Selected Approach

Use OkHttp (`EventListener`, `Call.cancel()`, connection pooling, bounded `ResponseBody.close()`) for Phase 5–8 network probing and HTTPS remote catalog fetching.

## Reason

OkHttp is battle-tested on Android, supports `Call.cancel()` for immediate scan abortion, and exposes granular connection lifecycle events without heavy overhead.

## Trade-offs

Adds ~500 KB dependency footprint when enabled in Phase 5 (kept commented out during Phase 3).

## Future Consequences

`TrustAll` certificate managers remain strictly forbidden; cleartext HTTP probing will be carefully documented and isolated from HTTPS-only remote catalog updates.
