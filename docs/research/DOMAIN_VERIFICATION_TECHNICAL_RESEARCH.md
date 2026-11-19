# Technical Research Report — Domain, Protocol & Background Verification

This document summarizes authoritative technical standards, official API capabilities, and known limitations governing BDIX Tester's conservative verification architecture.

---

## 1. DNS Resolution & Authoritative Semantics

- **References**:
  - IETF RFC 1035 (*Domain Names — Implementation and Specification*)
  - IETF RFC 2308 (*Negative Caching of DNS Queries — DNS NCACHE*)
  - IETF RFC 8484 (*DNS Queries over HTTPS — DoH*)
- **Critical Distinctions**:
  - **`NXDOMAIN` (RCODE 3)**: Domain name does not exist in the authoritative zone.
  - **`NODATA` (RCODE 0 / `NOERROR` with 0 answers for requested RRtype)**: Domain exists, e.g., `NS`/`SOA`/`AAAA` present, but `A` record absent. **Must not** be conflated with `NXDOMAIN`.
  - **`SERVFAIL` (RCODE 2)**: Resolver could not validate DNSSEC chain, authoritative nameserver timed out, or lame delegation occurred. Indicates **temporary DNS failure**, never domain deletion.
  - **`REFUSED` (RCODE 5) / Network Timeout**: Policy/rate-limit/reachability refusal or packet loss; classified as **temporary failure / unknown**, never deletion.
- **Verification Rule & Limitation**:
  - A single public resolver returning `NXDOMAIN` may reflect stale negative cache, split-horizon ISP filtering, or interception.
  - Our verifier queries multiple independent public resolvers (e.g., Cloudflare `1.1.1.1`, Google `8.8.8.8`, Quad9 `9.9.9.9` over DoH JSON/wire DNS) and checks authoritative nameservers (`NS`/`SOA`) when diagnosing `NXDOMAIN`.

---

## 2. Domain Registration Lookup (ICANN RDAP & `.bd` ccTLD)

- **References**:
  - IETF RFC 9224 (*Finding the Authoritative Registration Data Access Protocol (RDAP) Service*) — IANA bootstrap registry `https://data.iana.org/rdap/dns.json`
  - IETF RFC 9083 (*JSON Responses for the Registration Data Access Protocol (RDAP)*)
  - IETF RFC 8056 (*Extensible Provisioning Protocol (EPP) and Registration Data Access Protocol (RDAP) Status Mapping*)
- **EPP / RDAP Status Interpretation Rules**:
  - `active` / `ok` → Registered and active.
  - `clientHold` (`"client hold"`) / `serverHold` (`"server hold"`) → Domain is suspended/not published in DNS, **but still registered and restorable**. Not proof of permanent expiration.
  - `redemptionPeriod` (`"redemption period"`) → Expired past auto-renew grace, held ~30 days for owner recovery with redemption fee. Treat as `domain_expired_suspected`, not confirmed deleted.
  - `pendingDelete` (`"pending delete"`) → Scheduled for purge after redemption (~5 days), still not yet dropped until registry purge completes (`404 Not Found` in authoritative RDAP after drop).
  - Past `expiration` event date alone is **not** proof of deletion because registry renewals, auto-renew grace periods, and RDAP propagation lag frequently keep domains active or renewable.
- **Confirmed Bangladesh `.bd` ccTLD Limitation**:
  - `.bd` (including `.com.bd`, `.net.bd`, `.org.bd`, `.edu.bd`) is operated by **BTCL** (`domain.btcl.gov.bd` / `bdia.btcl.com.bd`).
  - **No official public ICANN RDAP endpoint exists for `.bd`** in IANA's `dns.json` bootstrap, nor does BTCL publish an unauthenticated structured machine registration API.
  - **Engineering Guarantee**: BDIX Tester **never fabricates a `.bd` RDAP URL**. Any `.bd` domain (48 curated BDIX entries + 2 OTT entries) or other TLD absent from IANA RDAP bootstrap records `registration_lookup_unavailable` and remains conservative (`unknown` or `domain_expired_suspected`).

---

## 3. HTTP/HTTPS, FTP & SSRF Safety Semantics

- **HTTP/HTTPS (RFC 9110)**:
  - `200..299` → Active / reachable.
  - `300..399` → Inspect redirect hop target before classifying; record redirect chain cleanly.
  - `401` / `403` / `429` / `500..599` → Auth required, access policy/IP restriction, rate limiting, or server error. **Domain is alive** (`isp_restricted` or `temporarily_unavailable`).
  - `404` → Specific path unavailable; **does not invalidate the domain or root service**.
  - `410` (Gone) → Indicates intentional removal of the requested resource; flag for investigation, **do not automatically delete the domain** without independent corroboration.
  - TLS handshake/certificate failure → Expired/self-signed/SNI mismatch indicates misconfiguration, **never** domain deletion.
- **FTP Control Probing (RFC 959)**:
  - Probe TCP greeting (`220` / `120`->`220`), `421` temporary unavailability, `331`/`530` authentication required/rejected, `550` directory unavailable, connection refused, timeout, or unsupported scheme (`FTPS` RFC 4217). Never submit dictionary passwords or perform bulk directory crawls.
- **SSRF Prevention**:
  - Public probes and redirect followers validate every resolved target against loopback (`127.0.0.0/8`, `::1`), RFC1918 private ranges (`10/8`, `172.16/12`, `192.168/16`), link-local / cloud metadata (`169.254.0.0/16`, `169.254.169.254`, `fe80::/10`), and carrier-grade/multicast ranges.
  - Legitimate private RFC1918 catalog endpoints (51 curated BDIX entries) are **preserved and exempted** from public CI dead-domain classification, while on-device Android probes allow user-triggered LAN/ISP-intranet testing safely without following malicious external-to-internal redirects.

---

## 4. Python, Android & CI Toolchains

- **Python**: Stdlib `Python 3.11` (`urllib.request`, `socket`, `ssl`, `http.client`, `ipaddress`, `struct` for RFC 1035 UDP DNS queries, `json`, `unittest`) ensures zero external `pip` dependency fragility across CI and developer machines.
- **Android Persistence (`Room 2.8.5`)**: Non-destructive schema upgrade `MIGRATION_2_3` preserves `servers`, `favorites`, `test_history`, `scan_sessions`, and `catalog_metadata` while adding `endpoint_governance` and `verification_events` tables, verified via exported schema `3.json`.
- **Android Background Work (`WorkManager 2.12.0` + NotificationChannel)**: Respects `NetworkType.CONNECTED` and `setRequiresBatteryNotLow(true)`, persists incremental progress, respects API 33+ `POST_NOTIFICATIONS` permission, and emits one deduplicated completion/failure summary notification.
- **GitHub Actions Security & Determinism**: Separates required PR-blocking deterministic checks (`android-ci.yml`) from optional manual/scheduled live checks (`catalog-live-verification.yml`) with strict `permissions`, `concurrency`, `timeout-minutes`, artifact uploads, and zero automated catalog deletion.
