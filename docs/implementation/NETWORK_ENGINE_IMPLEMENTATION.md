# Network Engine Implementation (Phase 5)

## HTTP & HTTPS Probe Engine (`HttpEndpointTester`)

- **Stage Latency Instrumentation:** Per-call `ProbeTimingTracker` attached as OkHttp `Request.tag` and read via `EventListener` records `dnsTimeMs`, `tcpConnectTimeMs`, `tlsHandshakeTimeMs`, and `httpResponseLatencyMs` using `System.nanoTime()`.
- **Low-Overhead Probing:** Attempts `HEAD` first; on `405 Method Not Allowed`, `501 Not Implemented`, or `ProtocolException`, falls back to `GET` with `Range: bytes=0-1023` and `Accept-Encoding: identity`. Hard body truncation reads at most `1024` bytes even if a server ignores the `Range` header.
- **Redirect & TLS Security:** Disables automatic OkHttp redirects (`followRedirects(false)`, `followSslRedirects(false)`) and manually validates up to `5` redirect hops. Any HTTPS-to-HTTP downgrade redirect is rejected as `ErrorClassification.PROTOCOL_DOWNGRADE_BLOCKED`. Uses default Android system trust anchors with zero custom `TrustManager` or `HostnameVerifier` bypasses.
- **Cleartext HTTP Isolation:** Because built-in BDIX hosts (`365` `http://` entries, private/public IPv4, dynamic user custom endpoints) cannot be enumerated in static `<domain-config>` rules, `res/xml/network_security_config.xml` configures cleartext solely at runtime within `HttpEndpointTester` / `SpeedTestEngine` on HTTP targets; remote catalog sync and future app services remain strictly HTTPS-only, zero credentials are sent over HTTP, and HTTPS->HTTP downgrades are blocked.
- **Result Semantics:** `2xx` -> `REACHABLE`; `401/403/404/4xx/5xx` -> `RESTRICTED` (distinguishing host responsiveness from network unreachability); DNS failure / connection refused -> `UNREACHABLE`; deadline expiry -> `TIMED_OUT` (Quick Scan `2500 ms` marks `isQuickScanInconclusive = true`).

## RFC 959 FTP Control Probe Engine (`FtpControlTester`)

- Connects directly via TCP `Socket` to explicit port or default `21`.
- Reads multi-line RFC 959 control banners safely: handles intermediate `120` replies before `220`, multi-line `220-...` terminating on `220 `, rejects `421` (`RESTRICTED`), rejects non-FTP banners (`FTP_PROTOCOL_ERROR`), enforces `8 KiB` / `32` line ceiling, sends `QUIT\r\n` and reads `221`, and hooks coroutine `invokeOnCancellation` to close sockets immediately.
- Explicit `ftps://` endpoints immediately return `TestStatus.UNSUPPORTED` (`UNSUPPORTED_SCHEME`) and never silently downgrade to plain `ftp://`.
