# BDIX & Network Protocol Research — BDIX Tester

**Research Date:** 2026-11-18

---

## 1. BDIX Connectivity Fundamentals

Bangladesh Internet Exchange (BDIX) interconnects participating local ISPs, content providers, and educational/enterprise networks inside Bangladesh. When a broadband subscriber's ISP peers directly with a host or caches/routes local server traffic over BDIX peering, that endpoint may respond with low latency and high local throughput independent of international upstream bandwidth.

### Technical Reality & Diagnostic Constraints

1. **ISP-Dependent Routing:** Reachability from ISP A does **not** guarantee reachability from ISP B or mobile carrier C. Therefore, BDIX Tester classifies a working endpoint strictly as **"Reachable from current network"**, never as globally **"BDIX Verified"** without independent provenance.
2. **Private IPv4 Peering / Intra-ISP Portals:** Some ISP-hosted media/FTP servers reside on RFC 1918 private addresses (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`). These are only reachable inside that ISP's local routing domain or LAN/VPN. Failing to reach a private IP on another ISP does not mean the catalog entry is globally invalid.

Reference: IANA IPv4 Special-Purpose Address Registry — https://www.iana.org/assignments/iana-ipv4-special-registry/iana-ipv4-special-registry.xhtml

---

## 2. Scheme-Driven Protocol Classification

A frequent pitfall in legacy BDIX lists is treating any URL containing the substring `ftp` (such as `http://ftp.example.com`) as an FTP server. **Protocol classification MUST be determined strictly by the URL scheme:**

| URL Scheme                 | Classified Protocol | Default Port                            | Future Handling                                                                                                                   |
| -------------------------- | ------------------- | --------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------- |
| `http://`                  | `HTTP`              | 80                                      | Lightweight `HEAD` (bounded `GET` fallback) via HTTP client; cleartext explicitly scoped for user-initiated endpoint probing only |
| `https://`                 | `HTTPS`             | 443                                     | Standard system TLS verification (RFC 8446 / platform trust store); never bypass certificate validation                           |
| `ftp://`                   | `FTP`               | 21                                      | Dedicated RFC 959 control-connection check (Phase 5+); never tested via HTTP fallback                                             |
| `ftps://`                  | `FTPS`              | 990 (implicit) / 21 (explicit RFC 4217) | Separate from plain FTP; requires explicit TLS strategy before enabling                                                           |
| Any other / missing scheme | `UNSUPPORTED`       | N/A                                     | Marked `Unsupported` in UI and queued for manual catalog review                                                                   |

References:

- HTTP Semantics (RFC 9110): https://www.rfc-editor.org/rfc/rfc9110
- HTTP/1.1 (RFC 9112): https://www.rfc-editor.org/rfc/rfc9112
- File Transfer Protocol (RFC 959): https://www.rfc-editor.org/rfc/rfc959
- Securing FTP with TLS (RFC 4217): https://www.rfc-editor.org/rfc/rfc4217
- TLS 1.3 (RFC 8446): https://www.rfc-editor.org/rfc/rfc8446

---

## 3. Reachability vs. Latency vs. Download Speed

BDIX Tester enforces strict separation between three distinct network metrics:

1. **Reachability (Categorical State):** Whether the device could establish communication at the target protocol layer (e.g., `REACHABLE` for HTTP 2xx/3xx, `RESTRICTED` for HTTP 401/403/404, `UNREACHABLE` for DNS/connection refusal, `TIMED_OUT`).
2. **Latency (Milliseconds):**
   - _DNS Resolution Time:_ Name lookup latency.
   - _TCP Connect Time:_ Socket handshake latency.
   - _HTTP Response Latency:_ Time-to-first-byte / response headers.
   - _FTP Control Connect Time:_ Time to receive RFC 959 `220` greeting.
   - **Important:** Android blocks or restricts raw ICMP sockets on many devices/networks; therefore BDIX Tester **never** labels application-layer latency as "ICMP Ping".
3. **Download Throughput (Mbps):**
   - Only measured when the user explicitly initiates a speed test against an explicitly configured test resource (Phase 7).
   - Formula: `Download Mbps = (Transferred Bytes × 8) / (Elapsed Seconds × 1,000,000)`.
   - Never inferred from latency and never executed silently during a reachability scan.

---

## 4. Cleartext HTTP & Network Security Policy

- Remote catalog synchronization (Phase 8) MUST be **HTTPS-only**.
- Endpoint reachability probing (Phase 5+) necessarily involves `http://` URLs because many local Bangladesh ISP portals operate on plain HTTP. No user credentials or personal data may ever be transmitted over plain HTTP, and global TLS verification must never be weakened (`TrustAll` managers are strictly prohibited).
- Reference: https://developer.android.com/privacy-and-security/security-config
