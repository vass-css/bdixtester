# Performance & Resource Budgeting Report — BDIX Tester

**Date:** 2026-11-18  
**Scope:** Implemented Engine Resource Bounds & JVM/Robolectric Measured Execution Timings

---

## 1. Engine Resource & Concurrency Budgets (Implemented)

| Component                                 | Configured Bound                                                                                 | Purpose                                                                                                                   |
| ----------------------------------------- | ------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------- |
| `ScanCoordinator` Global Concurrency      | Default `3` (`MIN = 1`, `MAX = 5`)                                                               | Prevents socket exhaustion and ISP/router connection drops                                                                |
| `ScanCoordinator` Per-Host Semaphore      | `1` concurrent probe per host                                                                    | Prevents overwhelming shared-host ports (`10.16.100.241/*` variants)                                                      |
| Exact-URL Coalescing                      | `Map<originalUrl, List<ServerEndpoint>>`                                                         | Reduces `369` supplied entries (`365` unique URLs) to `365` unique probes per Full Scan (`4` duplicate probes eliminated) |
| `HttpEndpointTester` Timeouts             | Connect `5,000 ms`, Read `6,000 ms`, Request Deadline `15,000 ms`; Quick Scan overall `2,500 ms` | Bounded non-blocking execution via `withTimeoutOrNull` + `call.cancel()`                                                  |
| `HttpEndpointTester` Bandwidth Protection | `HEAD` first; fallback `GET` with `Range: bytes=0-1023` + 1,024 B stream cap                     | Prevents accidental large-file downloads during connectivity checks                                                       |
| `FtpControlTester` Stream Guards          | `8 KiB` max greeting bytes, `32` max lines, `800 ms` `QUIT` wait                                 | Prevents memory/greeting-flood DoS on malformed or hostile TCP banners                                                    |
| `SpeedTestEngine` Streaming Bounds        | `32 KiB` fixed buffer, `5 MiB` default byte cap (`20 MiB` max), `Accept-Encoding: identity`      | Exact byte cap stopping without extra over-read byte; progress updates throttled to `>= 100 ms`                           |
| `CatalogSignatureVerifier` Bounds         | Manifest `<= 64 KiB`, Signature `<= 512 B`, Payload `<= 2 MiB`                                   | Enforced via bounded byte stream reader independent of server `Content-Length`                                            |

## 2. Measured Host JVM / Robolectric Suite Timings

From `gradle :app:testDebugUnitTest --console=plain` (33 tests, host JVM):

| Suite                                                          | Tests | Measured Wall Time (s) |
| -------------------------------------------------------------- | ----: | ---------------------: |
| `BuiltInCatalogAuditTest`                                      |     2 |                `0.013` |
| `UrlParserTest`                                                |     5 |                `0.002` |
| `RoomDatabaseMigrationTest`                                    |     3 |                `0.438` |
| `NetworkProtocolTest` (`MockWebServer` + `ServerSocket`)       |     8 |                `5.502` |
| `ScanCoordinatorTest`                                          |     2 |                `0.300` |
| `SpeedTestEngineTest` (`MockWebServer`)                        |     4 |                `0.158` |
| `CatalogSignatureAndSyncTest`                                  |     3 |                `0.547` |
| `ExportAndIntentSafetyTest`                                    |     2 |                `0.160` |
| `NavigationUiTest` (Compose/Robolectric)                       |     1 |               `16.263` |
| `LocalizationAccessibilityUiTest` (Bangla Compose/Robolectric) |     1 |                `1.069` |

## 3. Hardware & Live Network Benchmarking Status

- **Physical Android Device / Live Bangladesh ISP Scan Telemetry:** **NOT YET EXECUTED** in this cloud environment (no ADB, emulator runtime, or live Bangladesh BDIX ISP routing is available here). Macrobenchmark / Android Studio Profiler memory and frame-time traces remain pending physical device verification.
