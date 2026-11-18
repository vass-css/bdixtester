# Network Testing Architecture — BDIX Tester

**Phase:** 2 (Design Specification — Engine Implementation Scheduled for Phases 5–7)  
**Date:** 2026-11-18

---

## 1. Design Goals & Safety Constraints

1. **Non-Invasive Probing Only:** Never implement port scanning, IP-range enumeration, directory brute forcing, credential guessing, or bulk file scraping.
2. **Protocol-Strict Dispatch:** Route probes strictly by URL scheme (`HTTP`, `HTTPS`, `FTP`, `FTPS`, `UNSUPPORTED`).
3. **Bounded Concurrency & Clean Cancellation:** Never spawn unbounded coroutines; ensure `Job.cancel()` immediately aborts queued probes and closes active sockets/calls.

---

## 2. Concurrency & Queue Policy

| Parameter                | Quick Scan Default                               | Full Scan Default                 | Notes                                                           |
| ------------------------ | ------------------------------------------------ | --------------------------------- | --------------------------------------------------------------- |
| Global Concurrency Limit | `8` (via `Semaphore`)                            | `6` (via `Semaphore`)             | Prevents socket exhaustion & radio congestion on mobile devices |
| Per-Host Concurrency     | `1` active probe / host                          | `1` active probe / host           | Prevents flooding a single local ISP server                     |
| Connect Timeout          | `2,500 ms`                                       | `5,000 ms`                        | Quick Scan favors speed; inconclusive slow hosts noted in UI    |
| Read / Header Timeout    | `2,500 ms`                                       | `6,000 ms`                        | Bounded header read only                                        |
| Automatic Retries        | `0`                                              | `1` (transient socket reset only) | User can explicitly trigger "Retry Failed"                      |
| Overlapping Scans        | Disallowed (`Mutex` + active `Job` cancel/guard) | Disallowed                        | Only one scan session runs at a time                            |

### Error Isolation & Progress Reporting

- Each endpoint probe executes inside `supervisorScope` + `try/catch` (`CancellationException` always rethrown!) so a DNS or TLS error on one server never aborts the batch.
- Completed outcomes stream into a `StateFlow<ScanProgressState>` updating Total, Completed, Remaining, Reachable, Unreachable, Restricted/Error, Unsupported, and Cancelled counts atomically.

---

## 3. Protocol Engines (Phases 5–6)

### 3.1 HTTP / HTTPS Engine

1. Validate URL syntax via `UrlParser`.
2. Measure DNS resolution + TCP connect + TLS handshake + HTTP header latency using OkHttp `EventListener` (never mislabeled as ICMP ping).
3. Send `HEAD` request with redirects enabled (bounded max redirects, `Connection: close` or pooled lightweight socket) and response body immediately closed without downloading HTML/media payloads.
4. If server rejects `HEAD` with `405 Method Not Allowed` or `501 Not Implemented`, perform a bounded `GET` with `Range: bytes=0-0` (or immediate close after reading status line/headers).
5. **TLS Policy:** Use standard Android system trust store; **never** install trust-all `X509TrustManager` or `HostnameVerifier`. Never silently rewrite `http://` to `https://`.

### 3.2 FTP / FTPS Engine

- **Plain FTP (`ftp://`):** Connect TCP socket to explicit port (default `21`) within timeout, read RFC 959 `220` banner, send clean `QUIT`, and close socket. No anonymous brute forcing, no `LIST` directory enumeration, and no file transfers.
- **FTPS (`ftps://`):** Treated separately from plain FTP. Reported as `UNSUPPORTED` until explicit/implicit RFC 4217 TLS support is explicitly authorized and implemented.

---

## 4. Dedicated Opt-In Speed Testing (Phase 7)

- Strictly separated from reachability scans and **never** triggered automatically during Quick Scan or Full Scan.
- Runs only when the user selects an endpoint that has an explicitly configured `speedTestUrl` and confirms data usage.
- Streams bytes through a fixed 32 KB scratch buffer (`InputStream.read(buffer)`), discarding bytes immediately so memory stays constant (`O(1)`).
- Formula: `Download Mbps = (Transferred Bytes × 8) / (Elapsed Seconds × 1_000_000.0)`.
- Enforces configurable transfer cap (e.g., 5 MB Quick profile / 20 MB Accurate profile) and minimum valid byte threshold before reporting Mbps.
