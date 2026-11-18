# ADR-006: FTP and FTPS Implementation Strategy

**Status:** Accepted (Phase 2 — Implementation Deferred to Phase 5+)  
**Date:** 2026-11-18

## Context

Actual `ftp://` and `ftps://` endpoints behave fundamentally differently from HTTP servers and cannot be tested by OkHttp.

## Requirements

- Validate control-channel reachability without directory brute forcing or unauthorized file downloads.
- Isolate plain FTP from FTPS.
- Support clean timeout and cancellation.

## Alternatives

1. Marking all `ftp://` and `ftps://` permanently unsupported.
2. Adding Apache Commons Net or custom lightweight RFC 959 control-socket handshake.

## Selected Approach

Implement a dedicated, non-invasive RFC 959 TCP control-connection handshake (`220` greeting + `QUIT`) for `ftp://` in Phase 5+, while classifying `ftps://` separately as `UNSUPPORTED` until explicit/implicit RFC 4217 TLS support is reviewed and approved.

## Reason

A bounded TCP socket + banner check verifies FTP reachability with minimal bytes and zero credential/directory probing.

## Trade-offs

Control-channel reachability does not verify passive-mode data ports or login permissions (documented transparently in result details).

## Future Consequences

`FtpEndpointTester` interface remains decoupled from HTTP testing so both can evolve independently.
