# ADR-004: Scheme-Driven Protocol Separation

**Status:** Accepted (Phase 2)  
**Date:** 2026-11-18

## Context

Many BDIX server domains contain `"ftp"` in the hostname (e.g., `http://ftp.example.net`) while actually serving HTTP/HTTPS portals. Legacy tools often misclassify these as FTP.

## Requirements

- Accurate protocol classification.
- Separate handling for HTTP, HTTPS, FTP, FTPS, and unsupported schemes.

## Alternatives

1. Heuristic keyword matching on URL strings.
2. Strict RFC-aligned URL scheme parsing (`http://`, `https://`, `ftp://`, `ftps://`).

## Selected Approach

`UrlParser` inspects the parsed URI scheme exclusively to assign `ServerProtocol`, keeping category (`FTP_FILES`, `MOVIES`, etc.) separate from protocol.

## Reason

Ensures HTTP endpoints are never routed to an FTP tester and FTP endpoints are never misreported via HTTP fallback.

## Trade-offs

Entries without a valid scheme or with non-standard schemes are classified as `UNSUPPORTED` and flagged for manual review rather than guessed.

## Future Consequences

Guarantees clean dispatch routing in the Phase 5 network testing engine.
