# Verification Architecture, 15-Status Lifecycle & Conservative Removal Rules

## 1. Layered Verification Model

BDIX Tester evaluates three strictly isolated evidence planes before assigning a classification:

1. **Domain & Registration Plane** (keyed by normalized registrable/FQDN domain, **never** by raw IP or endpoint URL path):
   - Multi-resolver DNS observations (`A`, `AAAA`, `CNAME`, `NS`, `SOA`, RCODE, TTL, resolver identity, timestamp, consistency).
   - Authoritative nameserver confirmation when multiple public resolvers report `NXDOMAIN`.
   - ICANN RDAP registration lookup (supported TLDs via IANA bootstrap) recording registration status, EPP codes, creation/expiration/update timestamps, registrar, and explicit `.bd` / unsupported TLD unavailability.
2. **Endpoint Service Plane** (keyed by canonical endpoint `serverId` / URL):
   - Protocol (`http`, `https`, `ftp`, `ftps`), explicit port, path, HTTP response / redirect chain / TLS failure / FTP greeting classification.
   - SSRF redirect guards protecting against localhost, private, link-local, and metadata redirects.
   - One failed URL path (`404`, `410`) or FTP auth rejection (`530`) **never** marks sibling endpoints on the same host or the parent domain as dead.
3. **Bangladesh ISP Accessibility Plane** (keyed by `(endpoint, isp_id)`):
   - External, provenance-verified ISP Probe JSON records containing ISP name/ID, region, timestamp, probe software version, DNS result, TCP/HTTP/FTP outcome, and latency.
   - Freshness enforcement (default stale threshold: **7 days / 168 hours**). Stale ISP probe entries are displayed for audit context but ignored as current evidence.

---

## 2. Fifteen-Status Lifecycle Reference

| Status | Meaning | Eligible for Removal? |
|---|---|---|
| `active` | Positive availability confirmed (multi-ISP or direct reachability + healthy DNS/HTTP/FTP `2xx`). | No |
| `redirected` | Endpoint responds with `3xx` and valid safe destination; redirect chain recorded for audit. | No |
| `isp_restricted` | Reachable from at least one BD ISP or returns `401`/`403` access policy restriction. | No |
| `unreachable_from_current_isp` | Current network cannot reach endpoint, while domain DNS/registration or other ISP status is not dead. | No |
| `temporarily_unavailable` | Timeout, connection refused, `HTTP 429`, `HTTP 5xx`, `FTP 421`, TLS error, or maintenance signal. | No |
| `dns_temporary_failure` | `SERVFAIL`, `REFUSED`, DNS timeout, or inconsistent resolver behavior. | No |
| `unsupported_protocol` | Unsupported URL scheme or explicit unimplemented protocol (`ftps://`). | No |
| `unknown` | Insufficient evidence, private RFC1918 IP checked externally, or `.bd`/unsupported registration lookup unavailable. | No |
| `pending_verification` | Initial state awaiting first verification run or scheduled recheck window (`24–72h`). | No |
| `dns_nxdomain_suspected` | Single resolver `NXDOMAIN` or initial multi-resolver `NXDOMAIN` awaiting recheck/authoritative proof. | No |
| `domain_expired_suspected` | Preliminary signs (e.g., `redemptionPeriod`, past expiry date, `clientHold`) that remain potentially renewable. | No |
| `domain_expired_confirmed` | Corroborated multi-resolver + authoritative `NXDOMAIN` AND/OR confirmed post-redemption/deleted RDAP status across recheck interval. | Requires Review Gate |
| `permanently_removed` | Independently corroborated permanent closure (e.g., `domain_expired_confirmed` or multi-ISP corroborated `410` permanent shutdown notice). | Requires Review Gate |
| `removal_candidate` | Passed strict conservative corroboration + recheck rules; queued for explicit human review. | Pending User Action |
| `archived` | Excluded from active catalog list by explicit user confirmation; 100% restorable with favorites & history intact. | Archived (Reversible) |

---

## 3. Conservative Removal Decision Rules

A domain/endpoint **cannot** enter `removal_candidate` unless ALL applicable safeguards pass:

1. **Non-Private Exemption**: Endpoint is NOT a private RFC1918 IP (`10/8`, `172.16/12`, `192.168/16`) or ISP-internal non-DNS IP literal lacking external dead proof.
2. **Positive ISP Veto**: Zero fresh BD ISP probe records report `reachable` / `restricted` (`HTTP 2xx/3xx/401/403`).
3. **Corroborated Dead / Expired Evidence** (at least one strong corroborated path):
   - **Path A (Confirmed Domain Deletion)**: Multi-resolver `NXDOMAIN` (>= 2 independent resolvers) + authoritative `NXDOMAIN`/unregistered zone + corroborated RDAP deletion (`404 Not Found` in authoritative RDAP server or `pendingDelete` past drop window) verified across `recheck_hours` (configurable `24–72h`, default `24h`).
   - **Path B (Multi-Resolver + Authoritative NXDOMAIN Over Time)**: For TLDs without RDAP (`registration_lookup_unavailable`, e.g., `.bd`), a single check stays at `unknown` / `dns_nxdomain_suspected`; only repeated corroborated authoritative `NXDOMAIN` across >= 2 independent recheck windows (`>= 24h`) with zero positive ISP evidence can escalate to `removal_candidate`, and **never** without flagging the missing RDAP limitation.
   - **Path C (Permanent Service Removal)**: Multiple independent fresh probes confirm `HTTP 410` or explicit closure AND domain/endpoint recheck confirms no active sibling paths.
4. **Never Auto-Archive / Never Hard-Delete**: Transitions from `removal_candidate` to `archived` ALWAYS require explicit user or maintainer confirmation.
