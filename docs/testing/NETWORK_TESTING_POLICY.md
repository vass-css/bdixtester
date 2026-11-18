# Network Testing Policy — BDIX Tester

**Date:** 2026-11-18

---

## 1. Authorized vs. Prohibited Network Actions

BDIX Tester is strictly a user-initiated server-directory and connectivity utility.

### Permitted (Phase 5+ Upon Authorization)

- User-triggered single-endpoint, category, or catalog reachability checks.
- Lightweight `HEAD` / bounded `GET` HTTP(S) probes with response bodies immediately closed.
- Non-invasive RFC 959 FTP control-channel greeting (`220` + `QUIT`).
- Explicit opt-in speed test against a declared `speedTestUrl` with user-visible byte caps.
- HTTPS-only conditional remote catalog synchronization (`ETag` / `If-None-Match`).

### Strictly Prohibited

- Port scanning or network-range sweeps (`192.168.x.1..254`, etc.).
- Directory or credential brute forcing.
- Vulnerability scanning or authentication bypass.
- Automated content harvesting or scraping.
- Silent background reachability scanning.
- Disabling TLS certificate validation (`TrustAll`) or upgrading `http://` to `https://` silently.
